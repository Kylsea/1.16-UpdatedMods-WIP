/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Weaponry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import Reika.ChromatiCraft.TileEntity.Networking.TileEntityCrystalPylon;
import Reika.DragonAPI.ModList;
import Reika.DragonAPI.Instantiable.Data.BlockStruct.BlockArray;
import Reika.DragonAPI.Instantiable.Data.Collections.ClassNameCache;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import Reika.DragonAPI.Instantiable.Data.Immutable.WorldLocation;
import Reika.DragonAPI.Libraries.ReikaAABBHelper;
import Reika.DragonAPI.Libraries.ReikaEntityHelper;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.DragonAPI.Libraries.Java.ReikaRandomHelper;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.DragonAPI.Libraries.Rendering.ReikaColorAPI;
import Reika.DragonAPI.ModRegistry.InterfaceCache;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.API.Interfaces.EMPControl;
import Reika.RotaryCraft.Auxiliary.EMPTileWatcher;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Auxiliary.Interfaces.RangedEffect;
import Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPowerReceiver;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.MachineRegistry;
import Reika.RotaryCraft.Registry.PacketRegistry;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeType;

public class TileEntityEMP extends TileEntityPowerReceiver implements RangedEffect {

	public static final long BLAST_ENERGY = (long)(4.184e9);

	private ArrayList<Coordinate> blocks = new ArrayList();
	private BlockArray check  = new BlockArray();

	private static ClassNameCache blacklist = new ClassNameCache();

	private static HashSet<WorldLocation> shutdownLocations = new HashSet();

	private boolean loading = true;
	private boolean canLoad = true;

	private long energy;

	public static final int MAX_RANGE = 64;

	@SideOnly(Side.CLIENT)
	public EMPEffect effectRender;

	private boolean fired = false;

	@SideOnly(Side.CLIENT)
	public static class EMPEffect {

		public static final int EXPAND_LIFESPAN = 20;
		public static final int FADE_LIFESPAN = 10;
		private int age;

		private EMPEffect() {
			age = 0;
		}

		public boolean tick() {
			age++;
			return age > EXPAND_LIFESPAN+FADE_LIFESPAN;
		}

		public double getRadius(float ptick) {
			if (age >= EXPAND_LIFESPAN)
				return MAX_RANGE+2*(age-EXPAND_LIFESPAN+ptick);
			return Math.min(MAX_RANGE, 0.25+(age+ptick)*MAX_RANGE/(double)EXPAND_LIFESPAN);
		}

		public float getBrightness() {
			return age <= EXPAND_LIFESPAN ? 1 : 1-((age-EXPAND_LIFESPAN)/(float)FADE_LIFESPAN);
		}

		public int getColor1() {
			return ReikaColorAPI.getColorWithBrightnessMultiplier(0xffBEFFFF, this.getBrightness());
		}

		public int getColor2() {
			return ReikaColorAPI.getColorWithBrightnessMultiplier(0xff47F2E2, this.getBrightness());
		}

	}

	static { //this list is horribly incomplete
		addEntry(TileEntityChest.class);
		addEntry(TileEntityEnderChest.class);
		addEntry(TileEntityHopper.class);
		addEntry(TileEntityDropper.class);
		addEntry(TileEntityDispenser.class);
		addEntry(TileEntityBrewingStand.class);
		addEntry(TileEntityEnchantmentTable.class);
		addEntry(TileEntityEndPortal.class);
		addEntry(TileEntitySign.class);
		addEntry(TileEntitySkull.class);

		addEntry("buildcraft.factory.TileTank", ModList.BCFACTORY);
		addEntry("buildcraft.transport.PipeTransport", ModList.BCTRANSPORT);

		addEntry("thermalexpansion.Blocks.conduit.TileConduitRoot", ModList.THERMALEXPANSION);

		addEntry("ic2.core.Blocks.wiring.TileEntityCable", ModList.IC2);

		addEntry("codechicken.enderstorage.common.TileFrequencyOwner", ModList.ENDERSTORAGE);

		addEntry("thaumcraft.common.tiles.*", ModList.THAUMCRAFT);

		addEntry("forestry.core.tiles.TileNaturalistChest", ModList.FORESTRY);
		addEntry("forestry.core.tiles.TileMill", ModList.FORESTRY);
		addEntry("forestry.apiculture.multiblock.TileAlvearyPlain", ModList.FORESTRY);
		addEntry("forestry.apiculture.tiles.*", ModList.FORESTRY);
		addEntry("forestry.aboriculture.tiles.TileTreeContainer", ModList.FORESTRY);
		addEntry("forestry.factory.tiles.TileWorktable", ModList.FORESTRY);

		//addEntry("Reika.FurryKingdoms.TileEntities.TileEntityFlag", ModList.FURRYKINGDOMS);

		addEntry("Reika.ExpandedRedstone.TileEntities.*", ModList.EXPANDEDREDSTONE);
		addEntry("Reika.ElectriCraft.TileEntities.TileEntityWire", ModList.ELECTRICRAFT);
	}

	private static void addEntry(Class<? extends TileEntity> cl) {
		blacklist.add(cl.getName());
		RotaryCraft.logger.log("Adding "+cl.getName()+" to the EMP immunity list");
	}

	private static void addEntry(String name, ModList mod) {
		if (!mod.isLoaded())
			return;
		blacklist.add(name);
		RotaryCraft.logger.log("Adding "+name+" to the EMP immunity list");
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {
		if (world.isRemote) {
			if (effectRender != null) {
				if (effectRender.tick())
					effectRender = null;
			}
		}
	}

	@Override
	public MachineRegistry getTile() {
		return MachineRegistry.EMP;
	}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateTileEntity();
		tickcount++;

		this.getPowerBelow();

		if (fired)
			return;

		if (power >= MINPOWER)
			energy += power;

		if (canLoad && check.isEmpty()) {
			int r = this.getRange();
			for (int i = x-r; i <= x+r; i++) {
				for (int k = z-r; k <= z+r; k++) {
					check.addBlockCoordinate(i, y, k);
					loading = true;
				}
			}
		}

		//ReikaJavaLibrary.pConsoleSideOnly(check.getSize(), Side.SERVER);
		//ReikaJavaLibrary.pConsole(blocks.size(), Side.SERVER);

		if (!world.isRemote)
			this.createListing();

		if (loading) {
			for (int i = 0; i < 6; i++) {
				double dx = rand.nextDouble();
				double dz = rand.nextDouble();
				world.spawnParticle("portal", x-0.5+dx*2, y+rand.nextDouble()+0.4, z-0.5+dz*2, -1+dx*2, 0.2, -1+dz*2);
			}
		}

		//power = (long)BLAST_ENERGY+800;

		if (energy/20L >= BLAST_ENERGY && !loading) {
			//if (world.isRemote)
			//	this.initEffect();
			//else
			this.fire(world, x, y, z);
		}
	}

	@SideOnly(Side.CLIENT)
	public void initEffect() {
		effectRender = new EMPEffect();
	}

	private void createListing() {
		World world = worldObj;
		int num = 1+8*ConfigRegistry.EMPLOAD.getValue();
		for (int i = 0; i < num && loading; i++) {
			int index = rand.nextInt(check.getSize());
			Coordinate b = check.getNthBlock(index);
			int x = b.xCoord;
			int z = b.zCoord;
			for (int y = 0; y < world.provider.getHeight(); y++) {
				TileEntity te = world.getTileEntity(x, y, z);
				if (this.canAffect(te)) {
					blocks.add(new Coordinate(te));
				}
			}
			check.remove(b.xCoord, b.yCoord, b.zCoord);
			if (check.isEmpty()) {
				loading = false;
				canLoad = false;
			}
		}
	}

	private boolean canAffect(TileEntity te) {
		if (te == null || te.isInvalid())
			return false;
		if (te instanceof IEnergyReceiver)
			return true;
		if (te instanceof IEnergyProvider)
			return true;
		if (InterfaceCache.IC2POWERTILE.instanceOf(te))
			return true;
		if (InterfaceCache.NODE.instanceOf(te))
			return true;
		if (ModList.CHROMATICRAFT.isLoaded() && te instanceof TileEntityCrystalPylon)
			return true;
		if (te instanceof RotaryCraftTileEntity) {
			return true;
		}
		return false;
	}

	private void fire(World world, int x, int y, int z) {
		fired = true;
		ReikaPacketHelper.sendDataPacketWithRadius(RotaryCraft.packetChannel, PacketRegistry.EMPEFFECT.ordinal(), this, 128);
		for (int i = 0; i < blocks.size(); i++) {
			TileEntity te = blocks.get(i).getTileEntity(world);
			if (ModList.CHROMATICRAFT.isLoaded() && te instanceof TileEntityCrystalPylon)
				((TileEntityCrystalPylon)te).onEMP(this);
			else if (InterfaceCache.NODE.instanceOf(te))
				this.chargeNode((INode)te);
			else
				this.shutdownTE(te);
		}
		this.affectEntities(world, x, y, z);
		//destroySelf(world, x, y, z);
	}

	private void destroySelf(World world, int x, int y, int z) {
		world.setBlockToAir(x, y, z);
		world.createExplosion(null, x+0.5, y+0.5, z+0.5, 3F, true);
		if (ReikaRandomHelper.doWithChance(50)) {
			ReikaItemHelper.dropItem(world, x+0.5, y+0.5, z+0.5, this.getTile().getCraftedProduct());
		}
		else if (ReikaRandomHelper.doWithChance(50)) {
			ArrayList<ItemStack> items = new ArrayList<ItemStack>();
			items.add(new ItemStack(Items.nether_star));
			items.add(new ItemStack(Items.diamond, 9, 0));
			items.add(new ItemStack(Items.gold_ingot, 32, 0));
			items.add(ReikaItemHelper.getSizedItemStack(ItemStacks.scrap, rand.nextInt(16)));
			ReikaItemHelper.dropItems(world, x+0.5, y+0.5, z+0.5, items);
		}
		else {
			ReikaItemHelper.dropItem(world, x+0.5, y+0.5, z+0.5, ReikaItemHelper.getSizedItemStack(ItemStacks.scrap, 8+rand.nextInt(16)));
		}
	}

	private void affectEntities(World world, int x, int y, int z) {
		AxisAlignedBB box = ReikaAABBHelper.getBlockAABB(x, y, z).expand(128, 64, 128);
		List<Entity> li = world.getEntitiesWithinAABB(Entity.class, box);
		for (Entity e : li) {
			if (InterfaceCache.BCROBOT.instanceOf(e)) {
				world.createExplosion(e, e.posX, e.posY, e.posZ, 3, false);
				e.setDead();
			}
			else if (e instanceof EntityLivingBase) {
				if (ReikaEntityHelper.isEntityWearingPoweredArmor((EntityLivingBase)e)) {
					for (int i = 1; i <= 4; i++) {
						e.setCurrentItemOrArmor(i, null);
					}
					float f = (float)ReikaRandomHelper.getRandomBetween(3D, 10D);
					world.newExplosion(e, e.posX, e.posY, e.posZ, f, true, true);
					e.motionX += ReikaRandomHelper.getRandomPlusMinus(0, 1.5);
					e.motionZ += ReikaRandomHelper.getRandomPlusMinus(0, 1.5);
					e.motionY += -ReikaRandomHelper.getRandomBetween(0.125, 1);
				}
			}
		}
	}

	private void chargeNode(INode te) {
		//ReikaJavaLibrary.pConsole(te.getNodeType()+":"+te.getAspects().aspects);
		te.setNodeVisBase(Aspect.ENERGY, (short)32000);
		te.setNodeVisBase(Aspect.WEAPON, (short)32000);
		te.setNodeVisBase(Aspect.MECHANISM, (short)32000);

		te.addToContainer(Aspect.ENERGY, (short)8000);
		te.addToContainer(Aspect.WEAPON, (short)1000);
		te.addToContainer(Aspect.MECHANISM, (short)2000);
		switch(te.getNodeType()) {
			case UNSTABLE:
				if (rand.nextInt(3) > 0)
					te.setNodeType(NodeType.DARK);
				else
					te.setNodeType(NodeType.PURE);
				break;
			case DARK:
				te.setNodeType(rand.nextBoolean() ? NodeType.TAINTED : NodeType.HUNGRY);
				break;
			case NORMAL:
				te.setNodeType(NodeType.UNSTABLE);
				break;
			default:
				break;
		}
		//ReikaJavaLibrary.pConsole(te.getNodeType()+":"+te.getAspects().aspects);
	}

	private void shutdownTE(TileEntity te) {
		if (te == null)
			return;
		if (this.isBlacklisted(te))
			return;
		ReikaPacketHelper.sendDataPacketToEntireServer(RotaryCraft.packetChannel, PacketRegistry.SPARKLOC.ordinal(), te.worldObj.provider.dimensionId, te.xCoord, te.yCoord, te.zCoord, 1);
		if (te instanceof RotaryCraftTileEntity) {
			RotaryCraftTileEntity rc = (RotaryCraftTileEntity)te;
			if (!rc.isShutdown())
				rc.onEMP();
		}
		else if (te instanceof EMPControl) {
			((EMPControl)te).onHitWithEMP(this);
		}
		else {
			addShutdownLocation(te);
		}/*
		else if (ConfigRegistry.ATTACKBLOCKS.getState())
			this.shutdownFallback(te);*/
	}

	private boolean isBlacklisted(TileEntity te) {
		return blacklist.contains(te.getClass());
	}

	private void shutdownFallback(TileEntity te) {
		//shutdownLocations.add(new WorldLocation(te));

		int x = te.xCoord;
		int y = te.yCoord;
		int z = te.zCoord;
		Block id = worldObj.getBlock(x, y, z);
		int meta = worldObj.getBlockMetadata(x, y, z);
		this.dropMachine(worldObj, x, y, z);
		/*
		;
		ItemStack[] inv;
		if (te instanceof IInventory) {
			IInventory ii = (IInventory)te;
			inv = new ItemStack[ii.getSizeInventory()];
			for (int i = 0; i < inv.length; i++) {
				inv[i] = ii.getStackInSlot(i);
			}
		}
		else {
			inv = new ItemStack[0];
		}
		worldObj.setBlock(x, y, z, BlockRegistry.DEADMACHINE.getBlock());
		TileEntityDeadMachine dead = (TileEntityDeadMachine)worldObj.getTileEntity(x, y, z);
		dead.setBlock(b, id, meta);
		dead.setInvSize(inv.length);
		for (int i = 0; i < inv.length; i++) {
			dead.setInventorySlotContents(i, inv[i]);
		}*/
	}

	public static boolean isShutdown(TileEntity te) {
		return !shutdownLocations.isEmpty() && shutdownLocations.contains(new WorldLocation(te));
	}

	public static boolean isShutdown(World world, int x, int y, int z) {
		return !shutdownLocations.isEmpty() && shutdownLocations.contains(new WorldLocation(world, x, y, z));
	}

	public static void resetCoordinate(World world, int x, int y, int z) {
		if (shutdownLocations.remove(new WorldLocation(world, x, y, z)))
			ReikaPacketHelper.sendDataPacketToEntireServer(RotaryCraft.packetChannel, PacketRegistry.SPARKLOC.ordinal(), world.provider.dimensionId, x, y, z, 0);
		if (shutdownLocations.isEmpty())
			EMPTileWatcher.instance.unregisterTileWatcher();
	}

	private static void addShutdownLocation(TileEntity te) {
		shutdownLocations.add(new WorldLocation(te));
		EMPTileWatcher.instance.registerTileWatcher();
	}

	private void dropMachine(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		;
		if (b != null) {
			//ReikaItemHelper.dropItems(world, x+0.5, y+0.5, z+0.5, b.getDrops(world, x, y, z, meta, 0));
			b.dropBlockAsItem(world, x, y, z, meta, 0);
		}
		world.setBlockToAir(x, y, z);
	}

	@Override
	public int getRange() {
		return 64;
	}

	@Override
	public int getMaxRange() {
		return MAX_RANGE;
	}

	public boolean isLoading() {
		return loading;
	}

	public boolean usable() {
		return !fired;
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT)
	{
		super.writeSyncTag(NBT);
		NBT.setBoolean("load", loading);
		NBT.setBoolean("cload", canLoad);
		NBT.setBoolean("fire", fired);
		NBT.setLong("e", energy);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT)
	{
		super.readSyncTag(NBT);
		loading = NBT.getBoolean("load");
		canLoad = NBT.getBoolean("cload");
		fired = NBT.getBoolean("fire");

		energy = NBT.getLong("e");
	}

	public void updateListing() {
		canLoad = true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public final double getMaxRenderDistanceSquared()
	{
		return 16384D;
	}

	@Override
	public final AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

}
