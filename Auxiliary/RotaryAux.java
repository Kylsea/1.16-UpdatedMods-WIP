/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Auxiliary;

import java.awt.Color;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import Reika.DragonAPI.ModList;
import Reika.DragonAPI.ASM.DependentMethodStripper.ModDependent;
import Reika.DragonAPI.Instantiable.Data.Collections.OneWayCollections.OneWaySet;
import Reika.DragonAPI.Libraries.Java.ReikaObfuscationHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaEngLibrary;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.DragonAPI.ModInteract.DeepInteract.ReikaThaumHelper;
import Reika.DragonAPI.ModInteract.ItemHandlers.MekToolHandler;
import Reika.DragonAPI.ModInteract.ItemHandlers.RedstoneArsenalHandler;
import Reika.DragonAPI.ModInteract.ItemHandlers.ThaumItemHelper;
import Reika.DragonAPI.ModInteract.ItemHandlers.TinkerToolHandler;
import Reika.RotaryCraft.GuiHandler;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.API.Interfaces.EnvironmentalHeatSource;
import Reika.RotaryCraft.API.Interfaces.EnvironmentalHeatSource.SourceType;
import Reika.RotaryCraft.API.Power.ShaftMachine;
import Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity;
import Reika.RotaryCraft.Base.TileEntity.TileEntityEngine;
import Reika.RotaryCraft.Base.TileEntity.TileEntityIOMachine;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.GuiRegistry;
import Reika.RotaryCraft.Registry.ItemRegistry;
import Reika.RotaryCraft.Registry.MachineRegistry;
import Reika.RotaryCraft.TileEntities.Transmission.TileEntitySplitter;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class RotaryAux {

	public static int blockModel;

	public static final Color[] sideColors = {Color.CYAN, Color.BLUE, Color.YELLOW, Color.BLACK, new Color(255, 120, 0), Color.MAGENTA};
	public static final String[] sideColorNames = {"CYAN", "BLUE", "YELLOW", "BLACK", "ORANGE", "MAGENTA"};

	public static final boolean getPowerOnClient = ConfigRegistry.POWERCLIENT.getState() || ReikaObfuscationHelper.isDeObfEnvironment();

	public static final double tungstenDensity = ReikaEngLibrary.rhoiron*0.8+0.2*ReikaEngLibrary.rhotungsten;

	private static Set<Class<? extends TileEntity>> shaftPowerBlacklist = new OneWaySet<Class<? extends TileEntity>>();

	static {
		//addShaftBlacklist("example.author.unauthorizedconverter.teclass");
	}

	public static boolean isBlacklistedIOMachine(TileEntity te) {
		return shaftPowerBlacklist.contains(te.getClass());
	}

	private static void addShaftBlacklist(String name) {
		Class cl;
		try {
			cl = Class.forName(name);
			shaftPowerBlacklist.add(cl);
			RotaryCraft.logger.log("Disabling "+name+" for shaft power. Destructive compatibility.");
		}
		catch (ClassNotFoundException e) {

		}
	}

	public static final boolean hasGui(World world, int x, int y, int z, EntityPlayer ep) {
		MachineRegistry m = MachineRegistry.getMachine(world, x, y, z);
		if (m == MachineRegistry.ENGINE) {
			TileEntityEngine te = (TileEntityEngine)world.getTileEntity(x, y, z);
			if (te == null)
				return false;
			if (te.getEngineType() == null)
				return false;
			if (!te.getEngineType().hasGui())
				return false;
			return true;
		}
		if (m == MachineRegistry.SPLITTER) {
			TileEntitySplitter te = (TileEntitySplitter)world.getTileEntity(x, y, z);
			return (te.getBlockMetadata() >= 8);
		}
		if (m == MachineRegistry.SCREEN)
			return !ep.isSneaking();
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			Object GUI = GuiHandler.instance.getClientGuiElement(GuiRegistry.MACHINE.ordinal(), ep, world, x, y, z);
			if (GUI != null)
				return true;
		}
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
			Object GUI = GuiHandler.instance.getServerGuiElement(GuiRegistry.MACHINE.ordinal(), ep, world, x, y, z);
			if (GUI != null)
				return true;
		}
		return false;
	}

	public static int get4SidedMetadataFromPlayerLook(EntityLivingBase ep) {
		int i = MathHelper.floor_double((ep.rotationYaw * 4F) / 360F + 0.5D);
		while (i > 3)
			i -= 4;
		while (i < 0)
			i += 4;
		switch (i) {
			case 0:
				return 2;
			case 1:
				return 1;
			case 2:
				return 3;
			case 3:
				return 0;
		}
		return -1;
	}

	public static int get6SidedMetadataFromPlayerLook(EntityLivingBase ep) {
		if (MathHelper.abs(ep.rotationPitch) < 60) {
			int i = MathHelper.floor_double((ep.rotationYaw * 4F) / 360F + 0.5D);
			while (i > 3)
				i -= 4;
			while (i < 0)
				i += 4;
			switch (i) {
				case 0:
					return 2;
				case 1:
					return 1;
				case 2:
					return 3;
				case 3:
					return 0;
			}
		}
		else { //Looking up/down
			if (ep.rotationPitch > 0)
				return 4; //set to up
			else
				return 5; //set to down
		}
		return -1;
	}

	public static int get2SidedMetadataFromPlayerLook(EntityLivingBase ep) {
		int i = MathHelper.floor_double((ep.rotationYaw * 4F) / 360F + 0.5D);
		while (i > 3)
			i -= 4;
		while (i < 0)
			i += 4;

		switch (i) {
			case 0:
				return 0;
			case 1:
				return 1;
			case 2:
				return 0;
			case 3:
				return 1;
		}
		return -1;
	}

	public static void flipXMetadatas(TileEntity t) {
		if (!(t instanceof RotaryCraftTileEntity))
			return;
		RotaryCraftTileEntity te = (RotaryCraftTileEntity)t;
		int m = te.getBlockMetadata();
		switch (m) {
			case 0:
				te.setBlockMetadata(1);
				break;
			case 1:
				te.setBlockMetadata(0);
				break;
		}
	}

	public static void flipZMetadatas(TileEntity t) {
		if (!(t instanceof RotaryCraftTileEntity))
			return;
		RotaryCraftTileEntity te = (RotaryCraftTileEntity)t;
		int m = te.getBlockMetadata();
		switch (m) {
			case 2:
				te.setBlockMetadata(3);
				break;
			case 3:
				te.setBlockMetadata(2);
				break;
		}
	}

	public static boolean canHarvestSteelMachine(EntityPlayer ep) {
		if (ep.capabilities.isCreativeMode)
			return false;
		ItemStack eitem = ep.inventory.getCurrentItem();
		if (eitem == null)
			return false;
		if (TinkerToolHandler.getInstance().isHammer(eitem))
			return false;
		if (TinkerToolHandler.getInstance().isPick(eitem) && TinkerToolHandler.getInstance().isStoneOrBetter(eitem))
			return true;
		if (MekToolHandler.getInstance().isPickTypeTool(eitem) && !MekToolHandler.getInstance().isWood(eitem))
			return true;
		if (eitem.getItem() == RedstoneArsenalHandler.getInstance().pickID) {
			return RedstoneArsenalHandler.getInstance().pickLevel > 0;
		}
		//if (!(eitem.getItem() instanceof ItemPickaxe))
		//	return false;
		if (eitem.getItem().canHarvestBlock(Blocks.iron_ore, eitem))
			return true;
		return false;
	}

	public static boolean shouldSetFlipped(World world, int x, int y, int z) {
		boolean softBelow = ReikaWorldHelper.softBlocks(world, x, y-1, z);
		boolean softAbove = ReikaWorldHelper.softBlocks(world, x, y+1, z);
		if (!softAbove && softBelow) {
			return true;
		}
		return false;
	}

	public static boolean isMuffled(TileEntity te) {
		return isMuffled(te.worldObj, te.xCoord, te.yCoord, te.zCoord);
	}

	public static boolean isMuffled(World world, int x, int y, int z) {
		if (isMufflingBlock(world, x, y+1, z) && isMufflingBlock(world, x, y-1, z)) {
			return true;
		}
		return false;
	}

	public static boolean isMufflingBlock(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		return b.getMaterial() == Material.cloth || b == Block.getBlockFromName("Rockwool");
	}

	public static boolean isNextToIce(World world, int x, int y, int z) {
		if (ReikaWorldHelper.checkForAdjMaterial(world, x, y, z, Material.ice) != null)
			return true;
		Block b = world.getBlock(x, y-1, z);
		if (b instanceof EnvironmentalHeatSource) {
			EnvironmentalHeatSource ehs = (EnvironmentalHeatSource)b;
			return ehs.isActive(world, x, y, z) && ehs.getSourceType(world, x, y, z).isCold();
		}
		return false;
	}

	public static boolean isNextToWater(World world, int x, int y, int z) {
		if (ReikaWorldHelper.checkForAdjMaterial(world, x, y, z, Material.water) != null)
			return true;
		for (int i = 1; i <= 2; i++) {
			Block b = world.getBlock(x, y-i, z);
			if (b instanceof EnvironmentalHeatSource) {
				EnvironmentalHeatSource ehs = (EnvironmentalHeatSource)b;
				return ehs.isActive(world, x, y-i, z) && ehs.getSourceType(world, x, y-i, z) == SourceType.WATER;
			}
		}
		return false;
	}

	public static boolean isNextToFire(World world, int x, int y, int z) {
		if (ReikaWorldHelper.checkForAdjBlock(world, x, y, z, Blocks.fire) != null)
			return true;
		for (int i = 1; i <= 2; i++) {
			Block b = world.getBlock(x, y-i, z);
			if (b instanceof EnvironmentalHeatSource) {
				EnvironmentalHeatSource ehs = (EnvironmentalHeatSource)b;
				return ehs.isActive(world, x, y-i, z) && ehs.getSourceType(world, x, y-i, z) == SourceType.FIRE;
			}
		}
		return false;
	}

	public static boolean isNextToLava(World world, int x, int y, int z) {
		if (ReikaWorldHelper.checkForAdjMaterial(world, x, y, z, Material.lava) != null)
			return true;
		for (int i = 1; i <= 2; i++) {
			Block b = world.getBlock(x, y-i, z);
			if (b instanceof EnvironmentalHeatSource) {
				EnvironmentalHeatSource ehs = (EnvironmentalHeatSource)b;
				return ehs.isActive(world, x, y-i, z) && ehs.getSourceType(world, x, y-i, z) == SourceType.LAVA;
			}
		}
		return false;
	}

	public static boolean isAboveFire(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y-1, z);
		if (b == Blocks.fire)
			return true;
		for (int i = 1; i <= 2; i++) {
			b = world.getBlock(x, y-i, z);
			if (b instanceof EnvironmentalHeatSource) {
				EnvironmentalHeatSource ehs = (EnvironmentalHeatSource)b;
				return ehs.isActive(world, x, y-i, z) && ehs.getSourceType(world, x, y-i, z) == SourceType.FIRE;
			}
		}
		return false;
	}

	public static boolean isAboveLava(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y-1, z);
		if (b.getMaterial() == Material.lava)
			return true;
		for (int i = 1; i <= 2; i++) {
			b = world.getBlock(x, y-i, z);
			if (b instanceof EnvironmentalHeatSource) {
				EnvironmentalHeatSource ehs = (EnvironmentalHeatSource)b;
				return ehs.isActive(world, x, y-i, z) && ehs.getSourceType(world, x, y-i, z) == SourceType.LAVA;
			}
		}
		return false;
	}

	public static String formatTemperature(double temp) {
		String unit = "C";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit = "F";
			temp = temp*1.8+32;
		}
		return String.format("%.0f%s", temp, unit);
	}

	public static String formatPressure(double press) {
		String unit = "Pa";
		if (OldTextureLoader.instance.loadOldTextures()) {
			//unit = "bar";
			//press /= 10130;
			unit = "psi";
			press *= 0.145;
		}
		else {
			press *= 1000;
		}
		double val = ReikaMathLibrary.getThousandBase(press);
		String sg = ReikaEngLibrary.getSIPrefix(press);
		return String.format("%.3f%s%s", val, sg, unit);
	}

	public static String formatTorque(double t) {
		String unit = "Nm";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit = "ft-lb";
			t *= 0.738;
		}
		double val = ReikaMathLibrary.getThousandBase(t);
		String sg = ReikaEngLibrary.getSIPrefix(t);
		return String.format("%.0f %s%s", val, sg, unit);
	}

	public static String formatSpeed(double s) {
		String unit = "rad/s";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit = "rpm";
			s *= 9.55;
		}
		double val = ReikaMathLibrary.getThousandBase(s);
		String sg = ReikaEngLibrary.getSIPrefix(s);
		return String.format("%.0f %s%s", val, sg, unit);
	}

	public static String formatPower(double p) {
		String unit = "W";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit = "hp";
			p /= 745.7;
		}
		double val = ReikaMathLibrary.getThousandBase(p);
		String sg = ReikaEngLibrary.getSIPrefix(p);
		return String.format("%.3f%s%s", val, sg, unit);
	}

	public static String formatEnergy(double e) {
		String unit = "J";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit = "ft-lb";
			e /= 1.356;
		}
		double val = ReikaMathLibrary.getThousandBase(e);
		String sg = ReikaEngLibrary.getSIPrefix(e);
		return String.format("%.3f%s%s", val, sg, unit);
	}

	public static String formatPowerIO(TileEntityIOMachine te) {
		return formatPowerIO(te.omega, te.power);
	}

	public static String formatPowerIO(ShaftMachine te) {
		return formatPowerIO(te.getOmega(), te.getPower());
	}

	public static String formatPowerIO(double speed, double power) {
		String unit1 = "W";
		String unit2 = "rad/s";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit1 = "hp";
			power /= 745.7;
			unit2 = "rpm";
			speed *= 9.55;
		}
		double valp = ReikaMathLibrary.getThousandBase(power);
		String sgp = ReikaEngLibrary.getSIPrefix(power);
		return String.format("%.3f%s%s @ %.0f %s", valp, sgp, unit1, speed, unit2);
	}

	public static String formatTorqueSpeedPowerForBook(String text, double torque, double speed, double power) {
		boolean old = OldTextureLoader.instance.loadOldTextures();
		String powerunit = old ? "hp" : "W";
		String torqueunit = old ? "ft-lb" : "Nm";
		String speedunit = old ? "rpm" : "rad/s";
		if (old) {
			speed *= 9.55;
			torque *= 0.738;
			power /= 745.7;
		}
		else {
			//speedunit = ReikaEngLibrary.getSIPrefix(speed)+speedunit;
			//torqueunit = ReikaEngLibrary.getSIPrefix(torque)+torqueunit;
			powerunit = ReikaEngLibrary.getSIPrefix(power)+powerunit;
			//speed = ReikaMathLibrary.getThousandBase(speed);
			//torque = ReikaMathLibrary.getThousandBase(torque);
			power = ReikaMathLibrary.getThousandBase(power);
		}
		text = text.replace("$SPEED_UNIT$", speedunit);
		text = text.replace("$POWER_UNIT$", powerunit);
		text = text.replace("$TORQUE_UNIT$", torqueunit);
		String ret = String.format(text, (int)torque, (int)speed, power);
		return ret;
	}

	public static String formatSingleValueForBook(String text, double value, int torqueSpeedPowerSelector) {
		boolean old = OldTextureLoader.instance.loadOldTextures();
		String unit = null;
		switch(torqueSpeedPowerSelector) {
			case 0:
				if (old)
					value *= 0.738;
				unit = old ? "ft-lb" : "Nm";
				break;
			case 1:
				if (old)
					value *= 9.55;
				unit = old ? "rpm" : "rad/s";
				break;
			case 2:
				if (old)
					value /= 745.7;
				unit = old ? "hp" : "W";
				break;
		}
		if (torqueSpeedPowerSelector == 2) {
			unit = ReikaEngLibrary.getSIPrefix(value)+unit;
			value = ReikaMathLibrary.getThousandBase(value);
		}
		text = text.replace("$SPEED_UNIT$", unit);
		text = text.replace("$POWER_UNIT$", unit);
		text = text.replace("$TORQUE_UNIT$", unit);
		String ret = null;
		if (torqueSpeedPowerSelector == 2) {
			ret = String.format(text, value);
		}
		else {
			ret = String.format(text, Integer.valueOf((int)value));
		}
		return ret;
	}

	public static String formatValuesForBook(String text, Object[] vals) {
		if (OldTextureLoader.instance.loadOldTextures()) {

		}
		return String.format(text, vals);
	}

	public static String formatDistance(double dist) {
		String unit = "m";
		if (OldTextureLoader.instance.loadOldTextures()) {
			unit = "ft";
			dist *= 3.28;
		}
		double val = ReikaMathLibrary.getThousandBase(dist);
		String sg = ReikaEngLibrary.getSIPrefix(dist);
		return String.format("%.3f%s%s", val, sg, unit);
	}

	public static String formatLiquidAmount(double amt) {
		String unit = "mB";
		if (OldTextureLoader.instance.loadOldTextures()) {
			amt *= 0.264;
			unit = "gal";
		}
		return String.format("%.0f%s", amt, unit);
	}

	public static String formatLiquidAmountWithSI(double amt) {
		String unit = "B";
		if (OldTextureLoader.instance.loadOldTextures()) {
			amt *= 0.264;
			unit = "gal";
		}
		double val = ReikaMathLibrary.getThousandBase(amt);
		String sg = ReikaEngLibrary.getSIPrefix(amt);
		return String.format("%.3f%s%s", val, sg, unit);
	}

	public static String formatLiquidFillFraction(double amt, double capacity) {
		String unit = "mB";
		if (OldTextureLoader.instance.loadOldTextures()) {
			amt *= 0.264;
			capacity *= 0.264;
			unit = "gal";
		}
		return String.format("%.0f/%.0f %s", amt, capacity, unit);
	}

	public static ItemStack getShaftCrossItem() {
		ItemStack is = ItemRegistry.SHAFT.getStackOf();
		is.stackTagCompound = new NBTTagCompound();
		is.stackTagCompound.setBoolean("cross", true);
		return is;
	}

	public static boolean isShaftCross(ItemStack is) {
		return ItemRegistry.SHAFT.matchItem(is) && is.stackTagCompound != null && is.stackTagCompound.getBoolean("cross");
	}

	public static boolean isHoldingScrewdriver(EntityPlayer ep) {
		ItemStack is = ep.getCurrentEquippedItem();
		if (ModList.THAUMCRAFT.isLoaded() && isScrewFocusWand(is))
			return true;
		return ItemRegistry.SCREWDRIVER.matchItem(is);
	}

	@ModDependent(ModList.THAUMCRAFT)
	private static boolean isScrewFocusWand(ItemStack is) {
		return is != null && is.getItem() == ThaumItemHelper.ItemEntry.WAND.getItem().getItem() && ReikaThaumHelper.getWandFocus(is) == ItemRegistry.SCREWFOCUS.getItemInstance();
	}
}
