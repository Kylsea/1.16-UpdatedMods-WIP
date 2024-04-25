/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;

import Reika.DragonAPI.Instantiable.Data.Maps.MultiMap;
import Reika.DragonAPI.Instantiable.Recipe.ItemMatch;
import Reika.DragonAPI.Libraries.ReikaInventoryHelper;
import Reika.DragonAPI.Libraries.Java.ReikaRandomHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Base.TileEntity.InventoriedPoweredLiquidIO;
import Reika.RotaryCraft.Registry.DifficultyEffects;
import Reika.RotaryCraft.Registry.MachineRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityFuelConverter extends InventoriedPoweredLiquidIO {

	public static final int CAPACITY = 5*FluidContainerRegistry.BUCKET_VOLUME;

	private static final MultiMap<String, FuelConversion> conversionMap = new MultiMap();
	private static final MultiMap<String, FuelConversion> conversionOutputMap = new MultiMap();

	public static FuelConversion addRecipe(String in, String out, int speed, int fluidRatio, double itemConsumeChance, ItemMatch... items) {
		return new FuelConversion(in, out, speed, fluidRatio, itemConsumeChance, items);
	}

	public static Collection<FuelConversion> getByInput(ItemStack is) {
		Collection<FuelConversion> li = new ArrayList();
		for (FuelConversion c : conversionMap.allValues(false)) {
			if (c.isValidItem(is))
				li.add(c);
		}
		return li;
	}

	public static Collection<FuelConversion> getByInput(Fluid f) {
		return Collections.unmodifiableCollection(conversionMap.get(f.getName()));
	}

	public static Collection<FuelConversion> getAllRecipes() {
		return Collections.unmodifiableCollection(conversionMap.allValues(false));
	}

	public static Collection<FuelConversion> getByOutput(Fluid f) {
		return Collections.unmodifiableCollection(conversionOutputMap.get(f.getName()));
	}

	public static final class FuelConversion {

		public static final FuelConversion BCFUEL = new FuelConversion("fuel", "rc jet fuel", 1, 4, DifficultyEffects.CONSUMEFRAC.getChance()/100D*1.5, new ItemMatch(Items.blaze_powder), new ItemMatch(ItemStacks.netherrackdust), new ItemMatch(ItemStacks.tar), new ItemMatch(Items.magma_cream), new ItemMatch(ReikaItemHelper.pinkDye));
		public static final FuelConversion KEROSENE = new FuelConversion("kerosene", "rc jet fuel", 1, 4, DifficultyEffects.CONSUMEFRAC.getChance()/100D, new ItemMatch(Items.blaze_powder), new ItemMatch(ItemStacks.netherrackdust), new ItemMatch(ItemStacks.tar), new ItemMatch(Items.magma_cream), new ItemMatch(ReikaItemHelper.pinkDye));

		public final Fluid input;
		public final Fluid output;

		public final int speedFactor;
		public final int fluidRatio;

		public final double itemConsumptionChance;

		private final ItemMatch[] ingredients;

		private UsablilityCondition condition;

		private FuelConversion(String in, String out, int sp, int r, double f, ItemMatch... items) {
			input = FluidRegistry.getFluid(in);
			output = FluidRegistry.getFluid(out);

			speedFactor = sp;
			fluidRatio = r;

			itemConsumptionChance = f;

			ingredients = items;

			this.register();
		}

		public FuelConversion setUsability(UsablilityCondition c) {
			condition = c;
			return this;
		}

		private void register() {
			if (input != null && output != null) {
				String n = input.getName();
				conversionMap.addValue(n, this);
				conversionOutputMap.addValue(output.getName(), this);
			}
		}

		@Override
		public String toString() {
			return input+">"+output+" x "+itemConsumptionChance+" @ "+fluidRatio+":1 %"+itemConsumptionChance;
		}

		public boolean isValid() {
			return input != null && output != null;
		}

		public boolean isValidItem(ItemStack is) {
			for (int i = 0; i < ingredients.length; i++) {
				if (ingredients[i].match(is))
					return true;
			}
			return false;
		}

		@SideOnly(Side.CLIENT)
		public Collection<ItemStack> getIngredientsForDisplay() {
			Collection<ItemStack> c = new ArrayList();
			for (ItemMatch m : ingredients) {
				c.add(m.getCycledItem());
			}
			return c;
		}

		public String getCondition() {
			return condition == null ? null : condition.getDescription();
		}
	}

	public static interface UsablilityCondition {

		public boolean isUsable(TileEntityFuelConverter te);
		public String getDescription();

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
	public MachineRegistry getTile() {
		return MachineRegistry.FUELENHANCER;
	}

	@Override
	public boolean hasModelTransparency() {
		return true;
	}

	@Override
	public int getRedstoneOverride() {
		if (input.isEmpty())
			return 15;
		if (output.isFull())
			return 15;
		return 0;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateTileEntity();
		tickcount++;
		this.getPowerBelow();

		//ReikaJavaLibrary.pConsole(input+":"+output);

		//ReikaJavaLibrary.pConsoleSideOnly("BC: "+this.getBCFuel()+"    JET: "+this.getJetFuel(), Side.CLIENT);

		if (power < MINPOWER)
			return;
		if (omega < MINSPEED)
			return;
		if (world.isRemote)
			return;

		Collection<FuelConversion> c = this.getConversionOptions();
		if (c != null) {
			int boost = 1+ReikaMathLibrary.logbase2(omega/MINSPEED)/2;
			for (FuelConversion fc : c) {
				int spd = boost*fc.speedFactor;
				if (this.getInputLevel() >= fc.fluidRatio*spd && (fc.condition == null || fc.condition.isUsable(this)) && output.canTakeIn(spd) && this.hasItems(fc)) {
					input.removeLiquid(fc.fluidRatio*spd);
					output.addLiquid(spd, fc.output);
					//ReikaJavaLibrary.pConsole(omega+"/"+MINSPEED+">"+boost+">"+spd);
					this.consumeItems(fc);
					break;
				}
			}
		}
	}

	private Collection<FuelConversion> getConversionOptions() {
		return !input.isEmpty() ? conversionMap.get(input.getActualFluid().getName()) : null;
	}

	private boolean hasItems(FuelConversion c) {
		for (int i = 0; i < c.ingredients.length; i++) {
			if (!ReikaInventoryHelper.checkForItemStack(c.ingredients[i], inv)) {
				return false;
			}
		}
		return true;
	}

	private void consumeItems(FuelConversion c) {
		for (int i = 0; i < c.ingredients.length; i++) {
			if (ReikaRandomHelper.doWithChance(c.itemConsumptionChance))
				ReikaInventoryHelper.decrStack(ReikaInventoryHelper.locateInInventory(c.ingredients[i], inv), inv);
		}
	}

	@Override
	public int getSizeInventory() {
		return 9;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack is) {
		return !getByInput(is).isEmpty();
	}

	public double getLiquidModelOffset(boolean in) {
		return in ? 10/16D : 1/16D;
	}

	public boolean canExtractItem(int i, ItemStack itemstack, int j) {
		return false;
	}

	@Override
	public boolean canConnectToPipe(MachineRegistry m) {
		return m == MachineRegistry.FUELLINE || m.isStandardPipe();
	}

	public Fluid getInputFluidType() {
		return input.getActualFluid();
	}

	public Fluid getOutputFluidType() {
		return output.getActualFluid();
	}

	@Override
	public Fluid getInputFluid() {
		return null;
	}

	@Override
	public boolean isValidFluid(Fluid f) {
		return conversionMap.get(f.getName()) != null;
	}

	@Override
	public boolean canOutputTo(ForgeDirection to) {
		return to.offsetY == 0;
	}

	@Override
	public boolean canReceiveFrom(ForgeDirection from) {
		return from == ForgeDirection.UP;
	}

	@Override
	public int getCapacity() {
		return CAPACITY;
	}

	@Override
	public boolean canIntakeFromPipe(MachineRegistry p) {
		return p.isStandardPipe() || p == MachineRegistry.FUELLINE;
	}

	@Override
	public boolean canOutputToPipe(MachineRegistry p) {
		return p == MachineRegistry.FUELLINE;
	}

}
