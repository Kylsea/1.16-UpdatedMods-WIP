/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Production;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;

import Reika.DragonAPI.DragonOptions;
import Reika.DragonAPI.ModList;
import Reika.DragonAPI.ASM.APIStripper.Strippable;
import Reika.DragonAPI.Base.BlockTieredResource;
import Reika.DragonAPI.Base.BlockTileEnum;
import Reika.DragonAPI.Interfaces.Block.SemiUnbreakable;
import Reika.DragonAPI.Interfaces.TileEntity.GuiController;
import Reika.DragonAPI.Interfaces.TileEntity.PartialInventory;
import Reika.DragonAPI.Libraries.ReikaEnchantmentHelper;
import Reika.DragonAPI.Libraries.ReikaInventoryHelper;
import Reika.DragonAPI.Libraries.ReikaNBTHelper.NBTTypes;
import Reika.DragonAPI.Libraries.ReikaPlayerAPI;
import Reika.DragonAPI.Libraries.ReikaSpawnerHelper;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.DragonAPI.Libraries.IO.ReikaSoundHelper;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaEngLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.DragonAPI.Libraries.World.ReikaBlockHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.DragonAPI.ModInteract.ItemHandlers.TwilightForestHandler;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.API.Event.BorerDigEvent;
import Reika.RotaryCraft.API.Interfaces.IgnoredByBorer;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Auxiliary.MachineEnchantmentHandler;
import Reika.RotaryCraft.Auxiliary.Interfaces.DiscreteFunction;
import Reika.RotaryCraft.Auxiliary.Interfaces.EnchantableMachine;
import Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity;
import Reika.RotaryCraft.Base.TileEntity.TileEntityBeamMachine;
import Reika.RotaryCraft.Blocks.BlockMiningPipe;
import Reika.RotaryCraft.Registry.BlockRegistry;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.DurationRegistry;
import Reika.RotaryCraft.Registry.ItemRegistry;
import Reika.RotaryCraft.Registry.MachineRegistry;
import Reika.RotaryCraft.Registry.RotaryAchievements;
import Reika.RotaryCraft.Registry.SoundRegistry;

import buildcraft.api.tiles.IHasWork;

@Strippable("buildcraft.api.tiles.IHasWork")
public class TileEntityBorer extends TileEntityBeamMachine implements EnchantableMachine, GuiController, DiscreteFunction, IHasWork {

	private final MachineEnchantmentHandler enchantments = new MachineEnchantmentHandler().addFilter(Enchantment.fortune).addFilter(Enchantment.efficiency).addFilter(Enchantment.silkTouch).addFilter(Enchantment.sharpness);

	private int pipemeta2 = 0;

	public boolean drops = true;

	private int reqpow;
	private int mintorque;

	/** Power required to break a block, per 0.1F hardness */
	public static final int DIGPOWER = (int)(64*ConfigRegistry.getBorerPowerMult());

	private static final int genRange = ConfigRegistry.BORERGEN.getValue();

	private static int anticipationDistance = -1;

	private int step = 1;

	public boolean[][] cutShape = new boolean[7][5]; // 7 cols, 5 rows

	private boolean jammed = false;

	private boolean nodig = false;
	private boolean isMiningAir = false;

	private boolean hitProtection = false;
	private int notifiedPlayer = 0;

	private int durability = ConfigRegistry.BORERMAINTAIN.getState() ? 256 : Integer.MAX_VALUE;

	private int soundtick = 0;

	@Override
	protected void onFirstTick(World world, int x, int y, int z) {
		if (anticipationDistance < 0)
			anticipationDistance = Math.max(2, Math.max(genRange, this.getServerViewDistance()));
	}

	private int getServerViewDistance() {
		MinecraftServer s = MinecraftServer.getServer();
		return s != null ? s.getConfigurationManager().getViewDistance() : 0;
	}

	@Override
	public int getTextureStateForSide(int s) {
		switch(this.getBlockMetadata()) {
			case 0:
				return s == 4 ? this.getActiveTexture() : 0;
			case 1:
				return s == 5 ? this.getActiveTexture() : 0;
			case 3:
				return s == 2 ? this.getActiveTexture() : 0;
			case 2:
				return s == 3 ? this.getActiveTexture() : 0;
		}
		return 0;
	}

	@Override
	public void onRedirect() {
		this.reset();
	}

	public boolean repair() {
		if (durability > 0)
			return false;
		durability = ConfigRegistry.BORERMAINTAIN.getState() ? 256 : Integer.MAX_VALUE;
		return true;
	}

	public boolean isJammed() {
		return jammed;
	}

	public void reset() {
		step = 1;
		this.syncAllData(true);
	}

	public int getHeadX() {
		return xCoord+facing.offsetX*step;
	}

	public int getHeadZ() {
		return zCoord+facing.offsetZ*step;
	}

	@Override
	protected int getActiveTexture() {
		return power > 0 && power >= reqpow && torque >= mintorque ? 1 : 0;
	}

	private void setJammed(boolean jam) {
		boolean old = jammed;
		jammed = jam;
		if (old != jammed) {
			ReikaWorldHelper.causeAdjacentUpdates(worldObj, xCoord, yCoord, zCoord);
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateTileEntity();

		tickcount++;
		this.getIOSides(world, x, y, z, meta);
		this.getPower(false);

		if (enchantments.hasEnchantments()) {
			for (int i = 0; i < 6; i++) {
				world.spawnParticle("portal", -0.5+x+2*rand.nextDouble(), y+rand.nextDouble(), -0.5+z+2*rand.nextDouble(), 0, 0, 0);
			}
		}

		power = (long)omega*(long)torque;
		if (power <= 0) {
			this.setJammed(false);
			this.reset();
			return;
		}

		if (hitProtection && notifiedPlayer < 10) {
			if (world.getTotalWorldTime()%100 == 0) {
				EntityPlayer ep = this.getPlacer();
				if (ep != null) {
					notifiedPlayer++;
					int hx = this.getHeadX();
					int hz = this.getHeadZ();
					String sg = "Your "+this+" has hit a protected area at "+hx+", "+hz+" and has jammed.";
					ReikaChatHelper.sendChatToPlayer(ep, sg);
				}
			}
		}

		if (durability <= 0) {
			if (tickcount%5 == 0) {
				world.playSoundEffect(x+0.5, y+0.5, z+0.5, "mob.blaze.hit", 0.75F, 0.05F);
				for (int i = 0; i < 6; i++) {
					world.spawnParticle("smoke", x+rand.nextDouble(), y+1+rand.nextDouble()*0.2, z+rand.nextDouble(), 0, 0, 0);
					world.spawnParticle("crit", x+rand.nextDouble(), y+1+rand.nextDouble()*0.2, z+rand.nextDouble(), 0, 0, 0);
				}
			}
			return;
		}

		nodig = true;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				if (cutShape[i][j]) {
					nodig = false;
					i = j = 7;
				}
			}
		}

		if (jammed && tickcount%5 == 0) {
			world.playSoundEffect(x+0.5, y+0.5, z+0.5, "mob.blaze.hit", 0.75F, 1F);
			for (int i = 0; i < 6; i++) {
				world.spawnParticle("smoke", x+rand.nextDouble(), y+1+rand.nextDouble()*0.2, z+rand.nextDouble(), 0, 0, 0);
				world.spawnParticle("crit", x+rand.nextDouble(), y+1+rand.nextDouble()*0.2, z+rand.nextDouble(), 0, 0, 0);
			}
		}

		if (nodig)
			return;
		if (omega <= 0)
			return;

		if (tickcount == 1 || step == 1) {
			isMiningAir = this.checkMiningAir(world, x, y, z, meta);
		}

		//ReikaJavaLibrary.pConsole(isMiningAir+":"+tickcount+"/"+this.getOperationTime(), Side.SERVER);

		if (soundtick > 0)
			soundtick--;

		if (!world.isRemote && tickcount >= this.getOperationTime() || (isMiningAir && tickcount%5 == 0)) {
			this.skipMiningPipes(world, x, y, z, meta, 0, 128);
			this.calcReqPowerSafe(world, x, y, z, meta);
			if (power >= reqpow && reqpow != -1) {
				this.setJammed(false);
				if (!world.isRemote) {
					for (int i = 0; i <= anticipationDistance; i++) {
						ReikaWorldHelper.forceGenAndPopulate(world, x+(step+16*i)*facing.offsetX, z+(step+16*i)*facing.offsetZ, genRange);
					}
					this.safeDig(world, x, y, z, meta);
					if (!isMiningAir) {
						if (soundtick == 0) {
							SoundRegistry.RUMBLE.playSoundAtBlock(this);
							soundtick = 5;
						}
						durability--;
					}
				}
			}
			else {
				this.setJammed(true);
			}
			tickcount = 0;
			isMiningAir = false;
		}
	}

	public String getCurrentRequiredPower() {
		if (reqpow < 0)
			return "Infinity - Blocked";
		double d1 = ReikaMathLibrary.getThousandBase(reqpow);
		double d2 = ReikaMathLibrary.getThousandBase(mintorque);
		String s1 = ReikaEngLibrary.getSIPrefix(reqpow);
		String s2 = ReikaEngLibrary.getSIPrefix(mintorque);
		return String.format("Required Power: %.3f%sW; Required Torque: %.3f%sNm", d1, s1, d2, s2);
	}

	private void safeDig(World world, int x, int y, int z, int meta) {
		try {
			this.dig(world, x, y, z, meta);
		}
		catch (RuntimeException e) {
			RotaryCraft.logger.logError(this+" triggered an exception mining a chunk, probably during worldgen!");
			e.printStackTrace();
		}
	}

	private boolean checkMiningAir(World world, int x, int y, int z, int meta) {
		int a = 0;
		if (meta > 1)
			a = 1;
		int b = 1-a;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				if (cutShape[i][j] || step == 1) {
					int xread = x+step*facing.offsetX+a*(i-3);
					int yread = y+step*facing.offsetY+(4-j);
					int zread = z+step*facing.offsetZ+b*(i-3);
					if (world.getBlock(xread, yread, zread) != Blocks.air) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void skipMiningPipes(World world, int x, int y, int z, int meta, int stepped, int max) {
		if (stepped >= max)
			return;
		int a = 0;
		if (meta > 1)
			a = 1;
		int b = 1-a;
		boolean allpipe = true;
		boolean haspipe = false;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				if (cutShape[i][j] || step == 1) {
					int xread = x+step*facing.offsetX+a*(i-3);
					int yread = y+step*facing.offsetY+(4-j);
					int zread = z+step*facing.offsetZ+b*(i-3);
					//ReikaJavaLibrary.pConsole(xread+","+yread+","+zread);
					if (world.getBlock(xread, yread, zread) == BlockRegistry.MININGPIPE.getBlockInstance()) {
						haspipe = true;
						int meta2 = world.getBlockMetadata(xread, yread, zread);
						ForgeDirection dir = BlockMiningPipe.getDirectionFromMeta(meta2);
						if (meta2 == 3 || Math.abs(dir.offsetX) == Math.abs(facing.offsetX) && Math.abs(dir.offsetZ) == Math.abs(facing.offsetZ)) {

						}
						else {
							allpipe = false;
						}
					}
				}
			}
		}
		if (haspipe && allpipe) {
			step++;
			this.skipMiningPipes(world, x, y, z, meta, stepped+1, max);
		}
	}

	private boolean ignoreBlockExistence(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		if (b == Blocks.air)
			return true;
		if (b.isAir(world, x, y, z))
			return true;
		if (ReikaBlockHelper.isLiquid(b))
			return true;
		if (b instanceof IgnoredByBorer)
			return ((IgnoredByBorer)b).ignoreHardness(world, world.provider.dimensionId, x, y, z, world.getBlockMetadata(x, y, z));
		return false;
	}

	private void calcReqPowerSafe(World world, int x, int y, int z, int metadata) {
		try {
			this.calcReqPower(world, x, y, z, metadata);
		}
		catch (RuntimeException e) {
			RotaryCraft.logger.logError(this+" triggered an exception mining a chunk, probably during worldgen!");
			e.printStackTrace();
			reqpow = -1;
		}
	}

	private void calcReqPower(World world, int x, int y, int z, int metadata) {
		reqpow = 0;
		mintorque = 0;
		int lowtorque = -1;
		int a = 0;
		if (metadata > 1)
			a = 1;
		int b = 1-a;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				if (cutShape[i][j] || step == 1) {
					int xread = x+step*facing.offsetX+a*(i-3);
					int yread = y+step*facing.offsetY+(4-j);
					int zread = z+step*facing.offsetZ+b*(i-3);
					this.reqPowAdd(world, xread, yread, zread);
					if (reqpow == -1)
						return;
				}
			}
		}

		lowtorque = mintorque;

		//ReikaJavaLibrary.pConsole(mintorque, Side.SERVER);

		if (torque < lowtorque)
			reqpow = -1;
	}


	private void reqPowAdd(World world, int xread, int yread, int zread) {
		if (step > 30000000) {
			reqpow = -1;
			return;
		}
		if (!this.ignoreBlockExistence(world, xread, yread, zread)) {
			Block id = world.getBlock(xread, yread, zread);
			int meta = world.getBlockMetadata(xread, yread, zread);
			float hard = id.getBlockHardness(world, xread, yread, zread);
			/*
			if (this.isMineableBedrock(world, xread, yread, zread)) {
				mintorque += PowerReceivers.BEDROCKBREAKER.getMinTorque();
				reqpow += PowerReceivers.BEDROCKBREAKER.getMinPower();
			}
			else */if (TwilightForestHandler.getInstance().isToughBlock(id)) {
				mintorque += 2048;
				reqpow += 65536;
			}
			else if (hard < 0) {
				reqpow = -1;
			}
			else if (id == BlockRegistry.DECO.getBlockInstance() && meta == ItemStacks.shieldblock.getItemDamage()) {
				reqpow = -1;
			}
			else if (id instanceof SemiUnbreakable && ((SemiUnbreakable)id).isUnbreakable(world, xread, yread, zread, meta)) {
				reqpow = -1;
			}
			else {
				reqpow += (int)(DIGPOWER*10*hard);
				int sharp = enchantments.getEnchantment(Enchantment.sharpness);
				mintorque += calculateTorqueForHardness(hard, sharp);
			}

			if (DragonOptions.DEBUGMODE.getState()) {
				RotaryCraft.logger.log(this+" mined block "+id+":"+meta+" at "+xread+", "+yread+", "+zread+"; pow="+reqpow+", torq="+mintorque);
			}
		}
	}

	public static int calculateTorqueForHardness(float hard, int sharp) {
		float c = 10-0.5F*sharp;
		int add = ReikaMathLibrary.ceilPseudo2Exp((int)(c*hard));
		if (sharp > 0) {
			add = Math.min(add, ReikaMathLibrary.intpow2(2, 10-sharp/3)); //clamp to 1024, halve for each three levels
		}
		return add;
	}

	public int getRequiredTorque() {
		return mintorque;
	}

	public long getRequiredPower() {
		return reqpow;
	}

	private void support(World world, int x, int y, int z, int metadata) {
		int a = 0;
		if (metadata > 1)
			a = 1;
		int b = 1-a;
		int xread;
		int yread;
		int zread;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				if (cutShape[i][j] || step == 1) {
					xread = x+step*facing.offsetX+a*(i-3);
					yread = y+step*facing.offsetY+(4-j);
					zread = z+step*facing.offsetZ+b*(i-3);
					Block id = world.getBlock(xread, yread+1, zread);
					if (id == Blocks.sand || id == Blocks.gravel)
						if (this.checkTop(i, j)) {
							if (id == Blocks.sand)
								world.setBlock(xread, yread+1, zread, Blocks.sandstone);
							else
								world.setBlock(xread, yread+1, zread, Blocks.stone);
						}
				}
			}
		}
	}

	private boolean checkTop(int i, int j) {
		while (j > 0) {
			j--;
			if (cutShape[i][j])
				return false;
		}
		return true;
	}

	private boolean dropBlocks(int xread, int yread, int zread, World world, int x, int y, int z, Block id, int meta) {
		if (ModList.TWILIGHT.isLoaded() && id == TwilightForestHandler.BlockEntry.MAZESTONE.getBlock())
			RotaryAchievements.CUTKNOT.triggerAchievement(this.getPlacer());
		if (id == Blocks.bedrock || id == Blocks.end_portal_frame)
			return false;
		if (!world.isRemote && !ReikaPlayerAPI.playerCanBreakAt((WorldServer)world, xread, yread, zread, id, meta, this.getServerPlacer())) {
			hitProtection = true;
			return false;
		}
		TileEntity tile = this.getTileEntity(xread, yread, zread);
		if (tile instanceof RotaryCraftTileEntity)
			return false;
		if (drops && id != Blocks.air) {
			/*
			if (this.isMineableBedrock(world, xread, yread, zread)) {
				ItemStack is = ReikaItemHelper.getSizedItemStack(ItemStacks.bedrockdust.copy(), DifficultyEffects.BEDROCKDUST.getInt());
				if (!this.chestCheck(world, x, y, z, is)) {
					ReikaItemHelper.dropItem(world, x+0.5, y+1.125, z+0.5, is, 3);
				}
				return true;
			}*/
			if (id == Blocks.mob_spawner) {
				TileEntityMobSpawner spw = (TileEntityMobSpawner)tile;
				if (spw != null) {
					ItemStack is = ItemRegistry.SPAWNER.getStackOf();
					ReikaSpawnerHelper.addMobNBTToItem(is, spw);
					if (!this.chestCheck(world, x, y, z, is))
						ReikaItemHelper.dropItem(world, x+0.5, y+1.125, z+0.5, is, 3);
					return true;
				}
			}
			if (tile instanceof IInventory) {
				IInventory ii = (IInventory)tile;
				List<ItemStack> contents = ReikaInventoryHelper.getWholeInventory(ii);
				ReikaInventoryHelper.clearInventory(ii);
				for (int i = 0; i < contents.size(); i++) {
					ItemStack is = contents.get(i);
					boolean fits = this.chestCheck(world, x, y, z, is);
					if (!fits) {
						ReikaItemHelper.dropItem(world, x+0.5, y+1.125, z+0.5, is, 3);
					}
				}
			}
			if (enchantments.getEnchantment(Enchantment.silkTouch) > 0 && this.canSilk(world, xread, yread, zread)) {
				ItemStack is = ReikaBlockHelper.getSilkTouch(world, xread, yread, zread, id, meta, this.getPlacer(), false);//new ItemStack(id, 1, ReikaBlockHelper.getSilkTouchMetaDropped(id, meta));
				if (!this.chestCheck(world, x, y, z, is)) {
					ReikaItemHelper.dropItem(world, x+0.5, y+1.125, z+0.5, is, 3);
				}
				return true;
			}
			int fortune = enchantments.getEnchantment(Enchantment.fortune);
			if (ModList.CHROMATICRAFT.isLoaded()) {
				//fortune += ChromatiAPI.getAPI().adjacency().getAdjacentUpgradeTier(worldObj, xCoord, yCoord, zCoord, CrystalElementAccessor.getByEnum("PURPLE"))/2;
			}
			Collection<ItemStack> items = id.getDrops(world, xread, yread, zread, meta, fortune);
			MinecraftForge.EVENT_BUS.post(new HarvestDropsEvent(xread, yread, zread, world, id, meta, fortune, 1, (ArrayList<ItemStack>)items, this.getPlacer(), false));
			if (id instanceof BlockTieredResource) {
				EntityPlayer ep = this.getPlacer();
				BlockTieredResource bt = (BlockTieredResource)id;
				boolean harvest = ep != null && bt.isPlayerSufficientTier(world, xread, yread, zread, ep);
				items = harvest ? bt.getHarvestResources(world, xread, yread, zread, fortune, ep) : bt.getNoHarvestResources(world, xread, yread, zread, fortune, ep);
			}
			else if (id instanceof BlockTileEnum) {
				items = ReikaJavaLibrary.makeListFrom(((BlockTileEnum)id).getMapping(world, xread, yread, zread).getCraftedProduct());
			}
			if (items != null) {
				for (ItemStack is : items) {
					if (!this.chestCheck(world, x, y, z, is)) {
						ReikaItemHelper.dropItem(world, x+0.5, y+1.125, z+0.5, is, 3);
					}
				}
			}
		}
		return true;
	}

	private boolean canSilk(World world, int x, int y, int z) {
		Block id = world.getBlock(x, y, z);
		if (id == Blocks.air)
			return false;
		if (id == Blocks.fire)
			return false;
		if (id == Blocks.cauldron)
			return false;
		if (id == Blocks.reeds)
			return false;
		if (id == Blocks.powered_comparator || id == Blocks.unpowered_comparator)
			return false;
		if (id == Blocks.powered_repeater || id == Blocks.unpowered_repeater)
			return false;
		if (id == Blocks.redstone_wire)
			return false;
		if (id == Blocks.piston_extension || id == Blocks.piston_head)
			return false;
		if (id == Blocks.wooden_door || id == Blocks.iron_door)
			return false;
		if (BlockRegistry.isTechnicalBlock(id))
			return false;
		if (id.isAir(world, x, y, z))
			return false;
		if (ReikaBlockHelper.isLiquid(id))
			return false;
		if (id instanceof BlockTieredResource)
			return false;
		if (id.hasTileEntity(world.getBlockMetadata(x, y, z)))
			return false;
		if (id instanceof BlockDoublePlant)
			return false;
		return true;
	}

	private boolean chestCheck(World world, int x, int y, int z, ItemStack is) {
		if (is == null)
			return false;
		if (world.isRemote)
			return false;
		for (int i = 0; i < 6; i++) {
			ForgeDirection dir = dirs[i];
			TileEntity te = this.getAdjacentTileEntity(dir);
			if (te instanceof IInventory) {
				boolean flag = true;
				if (te instanceof PartialInventory) {
					if (!((PartialInventory)te).hasInventory())
						flag = false;
				}
				if (flag) {
					if (ReikaInventoryHelper.addToIInv(is, (IInventory)te))
						return true;
				}
			}
		}
		return false;
	}

	private void dig(World world, int x, int y, int z, int metadata) {
		if (step == 1)
			RotaryAchievements.BORER.triggerAchievement(this.getPlacer());
		this.support(world, x, y, z, metadata);
		int a = 0;
		if (metadata > 1)
			a = 1;
		int b = 1-a;
		int xread;
		int yread;
		int zread;

		if (step == 1) {
			pipemeta2 = pipemeta;
			pipemeta = 3;
		}
		else if (pipemeta > 2 && pipemeta2 != 3)
			pipemeta = pipemeta2;

		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++) {
				if (cutShape[i][j] || step == 1) {
					xread = x+step*facing.offsetX+a*(i-3);
					yread = y+step*facing.offsetY+(4-j);
					zread = z+step*facing.offsetZ+b*(i-3);
					Block bk = world.getBlock(xread, yread, zread);
					if (this.dropBlocks(xread, yread, zread, world, x, y, z, bk, world.getBlockMetadata(xread, yread, zread))) {
						ReikaSoundHelper.playBreakSound(world, xread, yread, zread, bk);
						world.setBlock(xread, yread, zread, BlockRegistry.MININGPIPE.getBlockInstance(), pipemeta, 3);
					}
					else {
						step--;
					}
				}
			}
		}
		MinecraftForge.EVENT_BUS.post(new BorerDigEvent(this, step, x+step*facing.offsetX, y+step*facing.offsetY, z+step*facing.offsetZ, enchantments.hasEnchantment(Enchantment.silkTouch)));
		step++;
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT) {
		super.writeSyncTag(NBT);
		NBT.setInteger("step", step);
		NBT.setBoolean("jam", jammed);
		NBT.setInteger("dura", durability);

		NBT.setInteger("reqpow", reqpow);
		NBT.setInteger("reqtrq", mintorque);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT) {
		super.readSyncTag(NBT);
		step = NBT.getInteger("step");
		jammed = NBT.getBoolean("jam");
		durability = NBT.getInteger("dura");

		mintorque = NBT.getInteger("reqtrq");
		reqpow = NBT.getInteger("reqpow");
	}

	@Override
	public void writeToNBT(NBTTagCompound NBT) {
		super.writeToNBT(NBT);
		NBT.setBoolean("drops", drops);

		NBT.setTag("enchants", enchantments.writeToNBT());

		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++)
				NBT.setBoolean("cut"+String.valueOf(i*7+j), cutShape[i][j]);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound NBT) {
		super.readFromNBT(NBT);
		drops = NBT.getBoolean("drops");

		enchantments.readFromNBT(NBT.getTagList("enchants", NBTTypes.COMPOUND.ID));

		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 5; j++)
				cutShape[i][j] = NBT.getBoolean("cut"+String.valueOf(i*7+j));
		}
	}

	@Override
	protected void makeBeam(World world, int x, int y, int z, int meta) {}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	@Override
	public MachineRegistry getTile() {
		return MachineRegistry.BORER;
	}

	@Override
	public int getRedstoneOverride() {
		return this.isJammed() ? 15 : 0;
	}

	@Override
	public int getOperationTime() {
		int base = DurationRegistry.BORER.getOperationTime(omega);
		float ench = ReikaEnchantmentHelper.getEfficiencyMultiplier(enchantments.getEnchantment(Enchantment.efficiency));
		return (int)(base/ench);
	}

	@Override
	public MachineEnchantmentHandler getEnchantmentHandler() {
		return enchantments;
	}

	@Override
	public boolean hasWork() {
		return !nodig && !isMiningAir;
	}
}
