/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import Reika.RotaryCraft.Base.ItemRotaryTool;
import Reika.RotaryCraft.Entities.EntityGasMinecart;

public class ItemEthanolMinecart extends ItemRotaryTool {

	public ItemEthanolMinecart(int tex) {
		super(tex);
	}

	@Override
	public boolean onItemUse(ItemStack is, EntityPlayer ep, World world, int x, int y, int z, int s, float a, float b, float c) {
		if (super.onItemUse(is, ep, world, x, y, z, s, a, b, c))
			return true;
		Block id = world.getBlock(x, y, z);
		if (BlockRailBase.func_150051_a(id)) {
			if (!world.isRemote) {
				EntityGasMinecart cart = new EntityGasMinecart(world, x+0.5, y+0.5, z+0.5);
				world.spawnEntityInWorld(cart);
			}
			if (!ep.capabilities.isCreativeMode)
				--is.stackSize;
			return true;
		}
		return false;
	}

}
