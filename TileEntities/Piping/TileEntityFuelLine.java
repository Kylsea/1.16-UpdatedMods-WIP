/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Piping;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import Reika.RotaryCraft.Auxiliary.Interfaces.PumpablePipe;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPiping;
import Reika.RotaryCraft.Registry.MachineRegistry;

public class TileEntityFuelLine extends TileEntityPiping implements PumpablePipe {

	private int fuel = 0;
	private Fluid fluid;

	private boolean isAcceptableFuel(Fluid f) {
		if (f.equals(FluidRegistry.getFluid("rc jet fuel")))
			return true;
		if (f.equals(FluidRegistry.getFluid("rc ethanol")))
			return true;
		if (f.equals(FluidRegistry.getFluid("bioethanol")))
			return true;
		if (f.equals(FluidRegistry.getFluid("ethanol")))
			return true;
		if (f.equals(FluidRegistry.getFluid("fuel")))
			return true;
		if (f.equals(FluidRegistry.getFluid("rocket fuel")))
			return true;
		if (f.equals(FluidRegistry.getFluid("rc lifbe fuel")))
			return true;
		if (f.equals(FluidRegistry.getFluid("rc lifbe fuel preheat")))
			return true;
		if (f.equals(FluidRegistry.getFluid("turbofuel")))
			return true;
		return false;
	}

	@Override
	public MachineRegistry getTile() {
		return MachineRegistry.FUELLINE;
	}

	@Override
	public boolean canConnectToPipe(MachineRegistry m, ForgeDirection dir) {
		return m == MachineRegistry.FUELLINE || m == MachineRegistry.VALVE || m == MachineRegistry.SEPARATION || m == MachineRegistry.BYPASS || m == MachineRegistry.SUCTION;
	}

	@Override
	public IIcon getBlockIcon() {
		return Blocks.obsidian.getIcon(0, 0);
	}

	@Override
	public boolean hasLiquid() {
		return fuel > 0;
	}

	@Override
	public Fluid getFluidType() {
		return fluid;
	}

	@Override
	public int getFluidLevel() {
		return fuel;
	}

	@Override
	protected void setFluid(Fluid f) {
		fluid = f;
	}

	@Override
	protected void setLevel(int amt) {
		fuel = amt;
	}

	@Override
	protected boolean interactsWithMachines() {
		return true;
	}

	@Override
	protected void onIntake(TileEntity te) {

	}

	@Override
	public boolean isValidFluid(Fluid f) {
		return this.isAcceptableFuel(f);
	}

	@Override
	public boolean canReceiveFromPipeOn(ForgeDirection side) {
		return true;
	}

	@Override
	public boolean canEmitToPipeOn(ForgeDirection side) {
		return true;
	}

	@Override
	public Block getPipeBlockType() {
		return Blocks.obsidian;
	}

	@Override
	public boolean canIntakeFromIFluidHandler(ForgeDirection side) {
		return side.offsetY != 0;
	}

	@Override
	public boolean canOutputToIFluidHandler(ForgeDirection side) {
		return side.offsetY == 0;
	}
	/*
	@Override
	public boolean canTransferTo(PumpablePipe p, ForgeDirection dir) {
		if (p instanceof TileEntityFuelLine) {
			Fluid f = ((TileEntityFuelLine)p).fluid;
			return f != null ? f.equals(fluid) : true;
		}
		return false;
	}

	@Override
	public void transferFrom(PumpablePipe from, int amt) {
		((TileEntityFuelLine)from).fuel -= amt;
		fluid = ((TileEntityFuelLine)from).fluid;
		fuel += amt;
	}
	 */
}
