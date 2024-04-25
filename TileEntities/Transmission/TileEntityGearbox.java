/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Transmission;

import java.util.ArrayList;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import Reika.ChromatiCraft.API.Interfaces.WorldRift;
import Reika.DragonAPI.ModList;
import Reika.DragonAPI.ASM.APIStripper.Strippable;
import Reika.DragonAPI.ASM.DependentMethodStripper.ModDependent;
import Reika.DragonAPI.Instantiable.HybridTank;
import Reika.DragonAPI.Instantiable.StepTimer;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Libraries.IO.ReikaSoundHelper;
import Reika.DragonAPI.Libraries.Java.ReikaRandomHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaEngLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.RotaryCraft.RotaryConfig;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.API.Interfaces.ComplexIO;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Auxiliary.RotaryAux;
import Reika.RotaryCraft.Auxiliary.ShaftPowerEmitter;
import Reika.RotaryCraft.Auxiliary.Interfaces.NBTMachine;
import Reika.RotaryCraft.Auxiliary.Interfaces.PipeConnector;
import Reika.RotaryCraft.Auxiliary.Interfaces.SimpleProvider;
import Reika.RotaryCraft.Auxiliary.Interfaces.TemperatureTE;
import Reika.RotaryCraft.Base.TileEntity.TileEntity1DTransmitter;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPiping.Flow;
import Reika.RotaryCraft.Registry.DifficultyEffects;
import Reika.RotaryCraft.Registry.GearboxTypes;
import Reika.RotaryCraft.Registry.MachineRegistry;
import Reika.RotaryCraft.Registry.MaterialRegistry;
import Reika.RotaryCraft.Registry.RotaryAchievements;

import buildcraft.api.transport.IPipeConnection.ConnectOverride;
import buildcraft.api.transport.IPipeTile.PipeType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import vazkii.botania.api.mana.IManaReceiver;

@Strippable(value={"vazkii.botania.api.mana.IManaReceiver", "Reika.ChromatiCraft.API.Interfaces.Repairable"})
public class TileEntityGearbox extends TileEntity1DTransmitter implements PipeConnector, IFluidHandler, TemperatureTE, NBTMachine, IManaReceiver {

	public boolean reduction = true; // Reduction gear if true, accelerator if false

	private int damage = 0;

	private GearboxTypes type;

	private final HybridTank tank = new HybridTank("gear", 24000);
	private boolean failed;

	private int temperature;
	private StepTimer tempTimer = new StepTimer(20);

	private GearboxTypes bearingTier;

	private static final int MAX_DAMAGE = 480;

	public static final double BEARINGREDUCTION = 0.25;
	public static final double BEARINGINCREASE = 1.0;

	public TileEntityGearbox(GearboxTypes type) {
		if (type == null)
			type = GearboxTypes.WOOD;
		this.type = type;
		bearingTier = type;
	}

	public TileEntityGearbox() {
		this(GearboxTypes.WOOD);
	}

	public GearboxTypes getGearboxType() {
		return type != null ? type : GearboxTypes.WOOD;
	}

	public GearboxTypes getBearingTier() {
		return bearingTier != null ? bearingTier : type;
	}

	@SideOnly(Side.CLIENT)
	public void setData(GearboxTypes type, int ratio) {
		this.type = type;
		this.ratio = ratio;
	}

	public void setMaterialFromItem(ItemStack is) {
		type = GearboxTypes.getMaterialFromGearboxItem(is);
		bearingTier = type;
		this.syncAllData(true);
	}

	public void setBearingTier(GearboxTypes tier) {
		bearingTier = tier;
		this.syncAllData(true);
	}

	private int getBearingTierOffset() {
		return this.getBearingTier().material.ordinal()-type.material.ordinal();
	}

	public int getMaxLubricant() {
		return type.getMaxLubricant();
	}

	public int getDamage() {
		return damage;
	}

	public double getDamagedPowerFactor() {
		return Math.pow(0.99, damage);
	}

	public int getDamagePercent() {
		return this.getDamagePercent(damage);
	}

	@Override
	protected void readFromSplitter(World world, int x, int y, int z, TileEntitySplitter spl) { //Complex enough to deserve its own function
		int sratio = spl.getRatioFromMode();
		if (sratio == 0)
			return;
		boolean favorbent = false;
		if (sratio < 0) {
			favorbent = true;
			sratio = -sratio;
		}
		if (reduction) {
			if (x == spl.getWriteX() && z == spl.getWriteZ()) { //We are the inline
				omega = spl.omega/ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2*ratio;
				}
				else if (favorbent) {
					torque = spl.torque/sratio*ratio;
				}
				else {
					torque = ratio*(int)(spl.torque*((sratio-1D)/(sratio)));
				}
			}
			else if (x == spl.getWriteX2() && z == spl.getWriteZ2()) { //We are the bend
				omega = spl.omega/ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2*ratio;
				}
				else if (favorbent) {
					torque = ratio*(int)(spl.torque*((sratio-1D)/(sratio)));
				}
				else {
					torque = spl.torque/sratio*ratio;
				}
			}
			else { //We are not one of its write-to blocks
				torque = 0;
				omega = 0;
				power = 0;
			}
		}
		else {
			if (x == spl.getWriteX() && z == spl.getWriteZ()) { //We are the inline
				omega = spl.omega*ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2/ratio;
				}
				else if (favorbent) {
					torque = spl.torque/sratio/ratio;
				}
				else {
					torque = (int)(spl.torque*((sratio-1D))/sratio)/(ratio);
				}
			}
			else if (x == spl.getWriteX2() && z == spl.getWriteZ2()) { //We are the bend
				omega = spl.omega*ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2/ratio;
				}
				else if (favorbent) {
					torque = (int)(spl.torque*((sratio-1D)/(sratio)))/ratio;
				}
				else {
					torque = spl.torque/sratio/ratio;
				}
			}
			else { //We are not one of its write-to blocks
				torque = 0;
				omega = 0;
				power = 0;
			}
		}
	}

	@Override
	protected void onPositiveRedstoneEdge() {
		//ratio = -ratio;
		//reduction = !reduction; DO NOT DO
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateTileEntity();
		tickcount++;
		this.getIOSides(world, x, y, z, meta);

		if ((world.getWorldTime()&31) == 0)
			ReikaWorldHelper.causeAdjacentUpdates(world, x, y, z);

		this.transferPower(world, x, y, z, meta);
		power = (long)omega*(long)torque;
		this.getLubeAndApplyDamage(world, x, y, z, meta);
		tempTimer.update();
		if (tempTimer.checkCap()) {
			this.updateTemperature(world, x, y, z, meta);
		}

		if (!world.isRemote && power == 0 && this.isLiving() && rand.nextInt(20) == 0) {
			if (damage > 0 && (!type.needsLubricant() || tank.getLevel() >= 25)) {
				this.repair(1);
				if (type.needsLubricant()) {
					tank.removeLiquid(25);
				}
			}
		}

		this.basicPowerReceiver();
	}

	private void getLubeAndApplyDamage(World world, int x, int y, int z, int metadata) {
		int oldlube = 0;
		if (type.needsLubricant() && omega > 0 && this.getBearingTier().material.ordinal() < MaterialRegistry.BEDROCK.ordinal()) {
			if (tank.isEmpty()) {
				if (!world.isRemote && damage < MAX_DAMAGE && rand.nextInt(40) == 0 && this.getTicksExisted() >= 100) {
					damage++;
					RotaryAchievements.DAMAGEGEARS.triggerAchievement(this.getPlacer());
				}
				if (rand.nextDouble()*rand.nextDouble() > this.getDamagedPowerFactor()) {
					if (type.material.isFlammable() && !world.isRemote)
						ReikaWorldHelper.ignite(world, x, y, z);
					world.spawnParticle("crit", xCoord+rand.nextFloat(), yCoord+rand.nextFloat(), zCoord+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
					if (rand.nextInt(5) == 0) {
						world.playSoundEffect(x+0.5, y+0.5, z+0.5, type.material.getDamageNoise(), 1F, 1F);
					}
				}
			}
			else if (!world.isRemote && this.consumesLubricant()) {
				if (tickcount >= 80) {
					tank.removeLiquid(Math.max(1, (int)(DifficultyEffects.LUBEUSAGE.getChance()*this.getLubricantConsumptionFactor())));
					tickcount = 0;
				}
			}
		}
	}

	public boolean consumesLubricant() {
		return type.consumesLubricant() && this.getBearingTier().material.ordinal() < MaterialRegistry.DIAMOND.ordinal();
	}

	private double getLubricantConsumptionFactor() {
		double base = type.getLubricantConsumeRate(omegain)*ReikaMathLibrary.logbase(omegain, 2)/4;
		if (type != this.getBearingTier()) {
			base *= this.getBearingLubricantFactor();
		}
		if (this.isLiving())
			base *= 4;
		return base;
	}

	public double getBearingLubricantFactor() {
		if (!this.getBearingTier().needsLubricant())
			return -1;
		if (!this.getBearingTier().consumesLubricant())
			return 0;
		int offset = this.getBearingTierOffset();
		//double pow = Math.pow(0.667, offset);
		//base *= pow;

		double add = offset > 0 ? -BEARINGREDUCTION*offset : -offset*BEARINGINCREASE;
		return Math.max(0.1, 1+add);
	}

	public void getIOSides(World world, int x, int y, int z, int metadata) {
		while (metadata > 3)
			metadata -= 4;
		super.getIOSides(world, x, y, z, metadata, false);
	}

	private void calculateRatio() {
		int tratio = 1+this.getBlockMetadata()/4;
		ratio = (int)ReikaMathLibrary.intpow(2, tratio);
	}

	@Override
	protected void readFromCross(TileEntityShaft cross) {
		if (cross.isWritingTo(this)) {
			if (reduction) {
				omegain = cross.readomega[0]/ratio;
				torquein = cross.readtorque[0]*ratio;
			}
			else {
				omegain = cross.readomega[0]*ratio;
				torquein = cross.readtorque[0]/ratio;
			}
		}
		else if (cross.isWritingTo2(this)) {
			if (reduction) {
				omegain = cross.readomega[1]/ratio;
				torquein = cross.readtorque[1]*ratio;
			}
			else {
				omegain = cross.readomega[1]*ratio;
				torquein = cross.readtorque[1]/ratio;
			}
		}
		else {
			omegain = torquein = 0;
			return; //not its output
		}
	}

	@Override
	protected void transferPower(World world, int x, int y, int z, int meta) {
		this.calculateRatio();
		if (worldObj.isRemote && !RotaryAux.getPowerOnClient)
			return;
		performRatio = true;
		omegain = torquein = 0;
		boolean isCentered = x == xCoord && y == yCoord && z == zCoord;
		int dx = x+read.offsetX;
		int dy = y+read.offsetY;
		int dz = z+read.offsetZ;
		MachineRegistry m = isCentered ? this.getMachine(read) : MachineRegistry.getMachine(world, dx, dy, dz);
		TileEntity te = isCentered ? this.getAdjacentTileEntity(read) : world.getTileEntity(dx, dy, dz);
		if (this.isProvider(te)) {
			if (m == MachineRegistry.SHAFT) {
				TileEntityShaft devicein = (TileEntityShaft)te;
				if (devicein.isCross()) {
					this.readFromCross(devicein);
					performRatio = false;
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
			else if (te instanceof SimpleProvider) {
				this.copyStandardPower(te);
			}
			else if (te instanceof ComplexIO) {
				ComplexIO pwr = (ComplexIO)te;
				ForgeDirection dir = this.getInputForgeDirection().getOpposite();
				omegain = pwr.getSpeedToSide(dir);
				torquein = pwr.getTorqueToSide(dir);
			}
			else if (te instanceof ShaftPowerEmitter) {
				ShaftPowerEmitter sp = (ShaftPowerEmitter)te;
				if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
					torquein = sp.getTorque();
					omegain = sp.getOmega();
				}
			}
			else if (m == MachineRegistry.SPLITTER) {
				TileEntitySplitter devicein = (TileEntitySplitter)te;
				if (devicein.isSplitting()) {
					this.readFromSplitter(world, x, y, z, devicein);
					//omegain = reduction ? omega*ratio : omega/ratio;
					//torquein = reduction ? torque/ratio : torque*ratio;
					performRatio = false;
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
		}
		else if (te instanceof WorldRift) {
			WorldRift sr = (WorldRift)te;
			WorldLocation loc = sr.getLinkTarget();
			if (loc != null)
				this.transferPower(loc.getWorld(), loc.xCoord, loc.yCoord, loc.zCoord, meta);
		}
		else {
			omega = 0;
			torque = 0;
			power = 0;
			return;
		}

		if (performRatio) {
			if (reduction) {
				omega = omegain / ratio;
				if (torquein <= RotaryConfig.torquelimit/ratio)
					torque = torquein * ratio;
				else {
					torque = RotaryConfig.torquelimit;
					world.spawnParticle("crit", x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
					world.playSoundEffect(x+0.5, y+0.5, z+0.5, type.material.getDamageNoise(), 0.1F, 1F);
				}
			}
			else {
				if (omegain <= RotaryConfig.omegalimit/ratio)
					omega = omegain * ratio;
				else {
					omega = RotaryConfig.omegalimit;
					world.spawnParticle("crit", x+rand.nextFloat(), y+rand.nextFloat(), z+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
					world.playSoundEffect(x+0.5, y+0.5, z+0.5, type.material.getDamageNoise(), 0.1F, 1F);
				}
				torque = torquein / ratio;
			}
		}
		torque *= this.getDamagedPowerFactor();
		int tempEff = temperature+Math.max(0, world.getSavedLightValue(EnumSkyBlock.Block, x, y, z)-10);
		if (torque*(long)omega >= 131072L) {
			tempEff += ReikaMathLibrary.logbase2(torque*(long)omega/131072L);
		}
		if (tempEff <= (type.material == MaterialRegistry.WOOD ? -15 : -20)) {
			if (type.needsLubricant()) {
				double c = -(tempEff+20)/40D;
				omega /= Math.pow(1.4, c);
			}
			else if (type.material == MaterialRegistry.WOOD) {
				double c = Math.min(1, ((-tempEff)-15)*0.025);
				if (c > 0 && ReikaRandomHelper.doWithChance(c)) {
					damage++;
				}
			}
		}
		if (torque <= 0)
			omega = 0;

		if (!type.material.isInfiniteStrength())
			this.testFailure();
	}

	public void fail(World world, int x, int y, int z) {
		failed = true;
		world.createExplosion(null, x+0.5, y+0.5, z+0.5, 1F, true);
		ItemStack item = null;
		switch(type) {
			case WOOD:
			case LIVINGWOOD:
				item = ItemStacks.sawdust.copy();
				break;
			case STONE:
			case LIVINGROCK:
				item = new ItemStack(Blocks.gravel, 1, 0);
				break;
			case STEEL:
				item = ItemStacks.scrap.copy();
				break;
			case TUNGSTEN:
				item = ItemStacks.scrap.copy();
				//item = ItemStacks.tungstenflakes.copy();
				break;
			case DIAMOND:
				item = new ItemStack(Items.diamond, 1, 0);
				break;
			case BEDROCK:
				item = ItemStacks.bedrockdust.copy();
				break;
		}
		for (int i = 0; i < this.getRatio(); i++) {
			ReikaItemHelper.dropItem(world, x+0.5, y+1.25, z+0.5, item);
		}
		world.setBlockToAir(x, y, z);
	}

	public boolean repair(int dmg) {
		if (damage <= 0)
			return false;
		damage -= dmg;
		if (damage < 0)
			damage = 0;
		failed = false;
		return true;
	}

	@SideOnly(Side.CLIENT)
	public void setDamage(int dmg) {
		damage = dmg;
	}

	public void testFailure() {
		if (ReikaEngLibrary.mat_rotfailure(type.material.rho, 0.0625, type.getOmegaForRotFailure(omega, omegain), type.material.tensile)) {
			this.fail(worldObj, xCoord, yCoord, zCoord);
		}
		else if (ReikaEngLibrary.mat_twistfailure(Math.max(torque, torquein), 0.0625, type.material.shear/16D)) {
			this.fail(worldObj, xCoord, yCoord, zCoord);
		}
	}

	public int getLubricantScaled(int par1)
	{
		if (this.getMaxLubricant() == 0)
			return 0;
		return tank.getLevel()*par1/this.getMaxLubricant();
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT)
	{
		super.writeSyncTag(NBT);
		NBT.setBoolean("reduction", reduction);
		NBT.setInteger("damage", damage);
		NBT.setBoolean("fail", failed);
		NBT.setInteger("temp", temperature);
		NBT.setString("bearing", bearingTier.name());

		tank.writeToNBT(NBT);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT) {
		super.readSyncTag(NBT);
		reduction = NBT.getBoolean("reduction");
		damage = NBT.getInteger("damage");
		failed = NBT.getBoolean("fail");
		temperature = NBT.getInteger("temp");
		if (NBT.hasKey("bearing"))
			bearingTier = GearboxTypes.valueOf(NBT.getString("bearing"));

		tank.readFromNBT(NBT);
	}

	@Override
	public void writeToNBT(NBTTagCompound NBT) {
		NBT.setString("geartype", type.name());
		super.writeToNBT(NBT);
	}

	@Override
	public void readFromNBT(NBTTagCompound NBT) {
		GearboxTypes gear = GearboxTypes.WOOD;
		if (NBT.hasKey("geartype")) {
			gear = GearboxTypes.valueOf(NBT.getString("geartype"));
		}
		else if (NBT.hasKey("type")) {
			int idx = NBT.getInteger("type");
			if (idx >= MaterialRegistry.TUNGSTEN.ordinal())
				idx++;
			MaterialRegistry mat = MaterialRegistry.matList[idx];
			gear = GearboxTypes.getFromMaterial(mat);
		}
		type = gear;
		super.readFromNBT(NBT);
	}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {
		if (!this.isInWorld()) {
			phi = 0;
			return;
		}
		phi += ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return this.getMaxLubricant() > 0 ? 15*tank.getLevel()/this.getMaxLubricant() : 0;
	}

	@Override
	public boolean canConnectToPipe(MachineRegistry m) {
		return m == MachineRegistry.HOSE || m == MachineRegistry.BEDPIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(MachineRegistry p, ForgeDirection side) {
		return side != (isFlipped ? ForgeDirection.DOWN : ForgeDirection.UP);
	}

	@Override
	public void onEMP() {}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		if (this.canFill(from, resource.getFluid())) {
			int space = this.getMaxLubricant()-this.getLubricant();
			if (space > 0) {
				if (resource.amount > space)
					resource = new FluidStack(resource.getFluid(), space);
				return tank.fill(resource, doFill);
			}
		}
		return 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return from != (isFlipped ? ForgeDirection.DOWN : ForgeDirection.UP) && fluid.equals(FluidRegistry.getFluid("rc lubricant")) && !this.isLiving();
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return new FluidTankInfo[]{tank.getInfo()};
	}

	public int getLubricant() {
		return tank.getLevel();
	}

	public void setLubricant(int amt) {
		tank.setContents(amt, FluidRegistry.getFluid("rc lubricant"));
	}

	public void fillWithLubricant() {
		this.setLubricant(this.getMaxLubricant());
	}

	public boolean canTakeLubricant(int amt) {
		return tank.getLevel()+amt <= this.getMaxLubricant();
	}

	public void addLubricant(int amt) {
		tank.addLiquid(amt, FluidRegistry.getFluid("rc lubricant"));
	}

	public void clearLubricant() {
		tank.empty();
	}

	@Override
	public Flow getFlowForSide(ForgeDirection side) {
		return side != (isFlipped ? ForgeDirection.DOWN : ForgeDirection.UP) ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public MachineRegistry getTile() {
		return MachineRegistry.GEARBOX;
	}

	@Override
	public void updateTemperature(World world, int x, int y, int z, int meta) {
		int Tamb = ReikaWorldHelper.getAmbientTemperatureAt(world, x, y, z);
		if (omega > 0 && type.generatesHeat(omega, Tamb)) {
			temperature++;
			ReikaSoundHelper.playSoundAtBlock(world, x, y, z, type.material.getDamageNoise(), 0.67F, 1);
		}
		if (temperature > 90 && rand.nextBoolean() && type.takesTemperatureDamage()) {
			damage++;
			ReikaSoundHelper.playSoundAtBlock(world, x, y, z, type.material.getDamageNoise(), 1, 1);
		}
		if (temperature > Tamb)
			temperature = Math.max(Tamb, temperature-(omega == 0 ? 2 : 1));
		else if (temperature < Tamb)
			temperature = Math.min(Tamb, temperature+3);

		if (temperature > 120) {
			this.overheat(world, x, y, z);
		}
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public int getThermalDamage() {
		return 0;
	}

	@Override
	public void overheat(World world, int x, int y, int z) {
		if (type.material.isFlammable() && !world.isRemote)
			ReikaWorldHelper.ignite(world, x, y, z);
	}

	public static int getDamagePercent(int val) {
		return (int)(100*(1-Math.pow(0.99, val)));
	}

	@Override
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public boolean allowHeatExtraction() {
		return false;
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}

	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return 1000;
	}

	@Override
	public NBTTagCompound getTagsToWriteToStack() {
		NBTTagCompound NBT = new NBTTagCompound();
		//if (this.getGearboxType().isDamageableGear())
		NBT.setInteger("damage", this.getDamage());
		NBT.setInteger("lube", this.getLubricant());
		NBT.setString("bearing", bearingTier.name());
		return NBT;
	}

	@Override
	public void setDataFromItemStackTag(NBTTagCompound tag) {
		if (tag != null) {
			damage = tag.getInteger("damage");
			this.setLubricant(tag.getInteger("lube"));
			if (tag.hasKey("bearing")) {
				try {
					bearingTier = GearboxTypes.valueOf(tag.getString("bearing"));
				}
				catch (Exception e) {
					RotaryCraft.logger.logError("Invalid gearbox item with data "+tag);
				}
			}
		}
	}

	@Override
	public ArrayList<NBTTagCompound> getCreativeModeVariants() {
		return new ArrayList();
	}

	@Override
	public ArrayList<String> getDisplayTags(NBTTagCompound NBT) {
		return new ArrayList();
	}

	public boolean isLiving() {
		return ModList.BOTANIA.isLoaded() && (type == GearboxTypes.LIVINGROCK || type == GearboxTypes.LIVINGWOOD);
	}

	@Override
	public int getCurrentMana() {
		return tank.getLevel();
	}

	@Override
	public boolean isFull() {
		return this.getLubricant()+150 >= this.getMaxLubricant(); //+150 to not have bursts sent and waste 95%
	}

	@Override
	public void recieveMana(int mana) {
		tank.addLiquid(Math.min(mana, this.getMaxLubricant()-this.getLubricant()), FluidRegistry.getFluid("rc lubricant"));
	}

	@Override
	public boolean canRecieveManaFromBursts() {
		return this.getGearboxType() == GearboxTypes.LIVINGROCK && !this.isFull();
	}

	public void repairCC(int tier) {
		//damage = 60;
		int mod = Math.max(1, 64/ReikaMathLibrary.intpow2(2, tier));
		if (this.getTicksExisted()%mod == 0) {
			int amt = Math.max(1, Math.min(damage/8, (int)(Math.sqrt(tier)/20D)));
			this.repair(amt);
		}
	}

	@ModDependent(ModList.BCTRANSPORT)
	public final ConnectOverride overridePipeConnection(PipeType type, ForgeDirection side) {
		return type == PipeType.FLUID && this.consumesLubricant() && this.canConnectToPipeOnSide(MachineRegistry.HOSE, side) ? ConnectOverride.CONNECT : ConnectOverride.DISCONNECT;
	}
}
