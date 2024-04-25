/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Items.Tools;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import Reika.DragonAPI.Instantiable.Data.Immutable.DecimalPosition;
import Reika.DragonAPI.Libraries.MathSci.ReikaVectorHelper;
import Reika.RotaryCraft.Base.ItemRotaryTool;
import Reika.RotaryCraft.Base.TileEntity.TileEntityLaunchCannon;

public class ItemTarget extends ItemRotaryTool {

	public ItemTarget(int tex) {
		super(tex);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep) {
		MovingObjectPosition mov = null;//= ReikaPlayerAPI.getLookedAtBlock(512);
		for (float i = 0; i <= 512; i += 0.5) {
			DecimalPosition xyz = ReikaVectorHelper.getPlayerLookCoords(ep, i);
			if (!xyz.isEmpty(world)) {
				mov = xyz.asMovingPosition(0, ep.getLookVec());
				break;
			}
		}
		//ReikaChatHelper.write(mov);
		if (mov != null) {
			int x = mov.blockX;
			int y = mov.blockY;
			int z = mov.blockZ;
			//ReikaChatHelper.writeBlockAtCoords(world, x, y, z);
			int range = 16;
			for (int i = -range; i <= range; i++) {
				for (int j = -range; j <= range; j++) {
					for (int k = -range; k <= range; k++) {
						TileEntity te = world.getTileEntity((int)ep.posX+i, (int)ep.posY+j, (int)ep.posZ+k);
						if (te instanceof TileEntityLaunchCannon) {
							TileEntityLaunchCannon tc = (TileEntityLaunchCannon)te;
							if (tc.targetMode) {
								tc.target[0] = x;
								tc.target[1] = y;
								tc.target[2] = z;
							}
						}
					}
				}
			}
		}
		return is;
	}

}
