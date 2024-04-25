/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Items.Tools.Charged;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import Reika.DragonAPI.Instantiable.Data.Immutable.DecimalPosition;
import Reika.DragonAPI.Libraries.ReikaFluidHelper;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaVectorHelper;
import Reika.DragonAPI.Libraries.World.ReikaBlockHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.RotaryCraft.Base.ItemChargedTool;
import Reika.RotaryCraft.Registry.ConfigRegistry;

public class ItemUltrasound extends ItemChargedTool {

	public ItemUltrasound(int tex) {
		super(tex);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep) {
		if (is.getItemDamage() <= 0) {
			this.noCharge();
			return is;
		}
		this.warnCharge(is);
		//ReikaChatHelper.writeString(String.format("%.3f", look.xCoord)+" "+String.format("%.3f", look.yCoord)+" "+String.format("%.3f", look.zCoord));
		boolean ores = false;
		boolean cave = false;
		boolean silver = false;
		boolean liq = false;
		boolean caveready = false;
		for (float i = 0; i <= 5; i += 0.2) {
			DecimalPosition xyz = ReikaVectorHelper.getPlayerLookCoords(ep, i);
			Block id = xyz.getBlock(world);
			int meta = xyz.getBlockMetadata(world);
			Fluid f = ReikaFluidHelper.lookupFluidForBlock(id);
			if (ReikaBlockHelper.isOre(id, meta) && !ores) {
				ores = true;
				ReikaChatHelper.write("Ore Detected!");
			}
			if (id == Blocks.monster_egg && !silver) {
				silver = true;
				ReikaChatHelper.write("Silverfish Detected!");
			}
			if (id != Blocks.air && !ReikaWorldHelper.softBlocks(world, MathHelper.floor_double(xyz.xCoord), MathHelper.floor_double(xyz.yCoord), MathHelper.floor_double(xyz.zCoord)))
				caveready = true;
			if (f != null && !liq) {
				liq = true;
				ReikaChatHelper.write(f.getLocalizedName(new FluidStack(f, 1000))+" Detected!");
			}
			if (caveready && ReikaWorldHelper.caveBlock(id) && !cave) {
				cave = true;
				ReikaChatHelper.write("Cave Detected!");
			}
			if (!ores && !silver && !cave && !liq) {
				if (ConfigRegistry.CLEARCHAT.getState())
					ReikaChatHelper.clearChat(); //clr
			}
		}
		return new ItemStack(is.getItem(), is.stackSize, is.getItemDamage()-1);
	}
}
