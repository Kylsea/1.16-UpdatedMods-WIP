/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.ModInterface.Lua;

import net.minecraft.tileentity.TileEntity;

import Reika.DragonAPI.ModInteract.Lua.LuaMethod;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPiping;

public class LuaGetPipe extends LuaMethod {

	public LuaGetPipe() {
		super("getPipe", TileEntityPiping.class);
	}

	@Override
	protected Object[] invoke(TileEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		TileEntityPiping p = (TileEntityPiping) te;
		return new Object[]{p.getFluidType().getLocalizedName(), p.getFluidLevel()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the pipe contents.\nArgs: None\nReturns: [Fluid name, amount]";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
