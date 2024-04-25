/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Engine;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidContainerRegistry;

import Reika.DragonAPI.Libraries.ReikaInventoryHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Base.TileEntity.TileEntityEngine;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.EngineType;
import Reika.RotaryCraft.Registry.ItemRegistry;
import Reika.RotaryCraft.Registry.SoundRegistry;

public class TileEntityPerformanceEngine extends TileEntityEngine {

	/** Used in combustion power */
	public int additives;
	private boolean starvedengine;

	@Override
	public int getMaxTemperature() {
		return 240;
	}

	@Override
	protected void consumeFuel(float scale) {
		fuel.removeLiquid(scale*this.getConsumedFuel());
		if (rand.nextInt(30) == 0)
			if (additives > 0)
				additives--;
	}

	@Override
	protected void internalizeFuel() {
		if (inv[0] != null && fuel.getLevel()+FluidContainerRegistry.BUCKET_VOLUME < FUELCAP) {
			if (inv[0].getItem() == ItemRegistry.ETHANOL.getItemInstance()) {
				ReikaInventoryHelper.decrStack(0, inv);
				fuel.addLiquid(1000, RotaryCraft.ethanolFluid);
			}
		}
		if (inv [1] != null && additives < FUELCAP/FluidContainerRegistry.BUCKET_VOLUME) { //additives
			Item id = inv[1].getItem();
			if (id == Items.blaze_powder || id == Items.redstone || id == Items.gunpowder) {
				ReikaInventoryHelper.decrStack(1, inv);
				if (id == Items.redstone)
					additives += 1;
				if (id == Items.gunpowder)
					additives += 2;
				if (id == Items.blaze_powder)
					additives += 4;
			}
		}
	}

	@Override
	protected boolean getRequirements(World world, int x, int y, int z, int meta) {
		if (fuel.isEmpty())
			return false;
		starvedengine = additives <= 0;
		return true;
	}

	@Override
	protected void playSounds(World world, int x, int y, int z, float pitchMultiplier, float volume) {
		soundtick++;
		if (this.isMuffled(world, x, y, z)) {
			volume *= 0.3125F;
		}

		if (soundtick < this.getSoundLength(1F/pitchMultiplier) && soundtick < 2000)
			return;
		soundtick = 0;

		SoundRegistry.CAR.playSoundAtBlock(world, x, y, z, 0.33F*volume, 0.9F*pitchMultiplier);
	}

	@Override
	protected void offlineCooldown(World world, int x, int y, int z, int Tamb) {
		if (temperature > Tamb+300)
			temperature -= (temperature-Tamb)/100;
		else if (temperature > Tamb+100)
			temperature -= (temperature-Tamb)/50;
		else if (temperature > Tamb+40)
			temperature -= (temperature-Tamb)/10;
		else if (temperature > Tamb+4)
			temperature -= (temperature-Tamb)/2;
		else
			temperature = Tamb;
	}

	@Override
	public void updateTemperature(World world, int x, int y, int z, int meta) {
		super.updateTemperature(world, x, y, z, meta);

		int Tamb = ReikaWorldHelper.getAmbientTemperatureAt(world, x, y, z);
		if (temperature < Tamb)
			temperature += Math.max((Tamb-temperature)/40, 1);
		if (omega > 0 && torque > 0) { //If engine is on
			temperature += 1;
			if (water.getLevel() > 0 && temperature > Tamb) {
				water.removeLiquid(20);
				temperature--;
			}
			if (temperature > this.getMaxTemperature()/2) {
				if (rand.nextInt(10) == 0) {
					world.spawnParticle("smoke", x+0.5, y+0.5, z+0.5, 0, 0, 0);
					world.playSoundEffect(x+0.5, y+0.5, z+0.5, "random.fizz", 1F, 1F);
				}
			}
			if (temperature > this.getMaxTemperature()/1.25) {
				if (rand.nextInt(3) == 0) {
					world.playSoundEffect(x+0.5, y+0.5, z+0.5, "random.fizz", 1F, 1F);
				}
				world.spawnParticle("smoke", x+0.0, y+1.0625, z+0.5, 0, 0, 0);
				world.spawnParticle("smoke", x+0.5, y+1.0625, z+0.5, 0, 0, 0);
				world.spawnParticle("smoke", x+1, y+1.0625, z+0.5, 0, 0, 0);
				world.spawnParticle("smoke", x+0.0, y+1.0625, z+0, 0, 0, 0);
				world.spawnParticle("smoke", x+0.0, y+1.0625, z+1, 0, 0, 0);
				world.spawnParticle("smoke", x+1, y+1.0625, z+0, 0, 0, 0);
				world.spawnParticle("smoke", x+1, y+1.0625, z+1, 0, 0, 0);
			}
		}
		if (temperature > this.getMaxTemperature()) {
			this.overheat(world, x, y, z);
		}
	}

	@Override
	public void overheat(World world, int x, int y, int z) {
		temperature = this.getMaxTemperature();
		ReikaWorldHelper.overheat(world, x, y, z, ItemStacks.scrap.copy(), 0, 27, true, 1.5F, true, ConfigRegistry.BLOCKDAMAGE.getState(), 6F);
		world.setBlockToAir(x, y, z);
	}

	@Override
	public int getFuelLevel() {
		return fuel.getLevel();
	}

	public int getAdditivesScaled(int par1) {
		return (additives * par1*1000) / FUELCAP;
	}

	@Override
	protected int getMaxSpeed(World world, int x, int y, int z, int meta) {
		return starvedengine ? EngineType.GAS.getSpeed() : EngineType.SPORT.getSpeed();
	}

	@Override
	protected int getGenTorque(World world, int x, int y, int z, int meta) {
		return starvedengine ? EngineType.GAS.getTorque() : EngineType.SPORT.getTorque();
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT)
	{
		super.writeSyncTag(NBT);

		if (type.usesAdditives())
			NBT.setInteger("additive", additives);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT)
	{
		super.readSyncTag(NBT);

		if (type.usesAdditives())
			additives = NBT.getInteger("additive");
	}

	@Override
	protected void affectSurroundings(World world, int x, int y, int z, int meta) {

	}

	@Override
	public boolean allowHeatExtraction() {
		return false;
	}

	@Override
	public boolean canBeCooledWithFins() {
		return false;
	}

}
