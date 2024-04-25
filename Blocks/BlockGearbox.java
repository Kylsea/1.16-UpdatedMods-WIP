/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Blocks;

import java.util.ArrayList;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import Reika.DragonAPI.Auxiliary.Trackers.KeyWatcher;
import Reika.DragonAPI.Auxiliary.Trackers.KeyWatcher.Key;
import Reika.DragonAPI.Libraries.ReikaNBTHelper;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.Auxiliary.ItemStacks;
import Reika.RotaryCraft.Base.BlockModelledMachine;
import Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity;
import Reika.RotaryCraft.Items.Tools.ItemDebug;
import Reika.RotaryCraft.Items.Tools.ItemMeter;
import Reika.RotaryCraft.Items.Tools.ItemScrewdriver;
import Reika.RotaryCraft.Registry.GearboxTypes;
import Reika.RotaryCraft.Registry.GearboxTypes.GearPart;
import Reika.RotaryCraft.Registry.ItemRegistry;
import Reika.RotaryCraft.Registry.MaterialRegistry;
import Reika.RotaryCraft.TileEntities.Transmission.TileEntityGearbox;

public class BlockGearbox extends BlockModelledMachine {

	public BlockGearbox(Material mat) {
		super(mat);
		//this.blockIndexInTexture = 8;
		//this.blockHardness = 0.5F;
	}

	@Override
	public RotaryCraftTileEntity createTileEntity(World world, int meta) {
		return new TileEntityGearbox();
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		TileEntityGearbox tg = (TileEntityGearbox)world.getTileEntity(x, y, z);
		if (tg == null)
			return 0;
		if (tg.getGearboxType().material.isFlammable())
			return 60;
		return 0;
	}

	@Override
	public float getExplosionResistance(Entity ent, World world, int x, int y, int z, double eX, double eY, double eZ)
	{
		TileEntityGearbox gbx = (TileEntityGearbox)world.getTileEntity(x, y, z);
		if (gbx == null)
			return 0;
		MaterialRegistry type = gbx.getGearboxType().material;
		switch(type) {
			case WOOD:
				return 5F;
			case STONE:
				return 10F;
			case STEEL:
				return 15F;
			case TUNGSTEN:
			case DIAMOND:
				return 30F;
			case BEDROCK:
				return 90F;
		}
		return 0;
	}

	@Override
	public float getPlayerRelativeBlockHardness(EntityPlayer ep, World world, int x, int y, int z)
	{
		TileEntityGearbox gbx = (TileEntityGearbox)world.getTileEntity(x, y, z);
		if (gbx == null)
			return 0.01F;
		int mult = 1;
		if (ep.inventory.getCurrentItem() != null) {
			if (ep.inventory.getCurrentItem().getItem() == ItemRegistry.BEDPICK.getItemInstance())
				mult = 2;
		}
		if (this.canHarvest(world, ep, x, y, z))
			return mult*0.2F/(gbx.getGearboxType().ordinal()+1);
		return 0.01F/(gbx.getGearboxType().ordinal()+1);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean harv)
	{
		if (this.canHarvest(world, player, x, y, z))
			this.harvestBlock(world, player, x, y, z, 0);
		return world.setBlockToAir(x, y, z);
	}

	public boolean canHarvest(World world, EntityPlayer player, int x, int y, int z)
	{
		if (player.capabilities.isCreativeMode)
			return false;
		TileEntityGearbox gbx = (TileEntityGearbox)world.getTileEntity(x, y, z);
		if (gbx == null)
			return false;
		MaterialRegistry type = gbx.getGearboxType().material;
		return type.isHarvestablePickaxe(player.inventory.getCurrentItem());
	}

	@Override
	public void harvestBlock(World world, EntityPlayer ep, int x, int y, int z, int meta) {
		if (!this.canHarvest(world, ep, x, y, z))
			return;
		TileEntityGearbox gbx = (TileEntityGearbox)world.getTileEntity(x, y, z);
		if (gbx != null) {
			ItemStack todrop = gbx.getGearboxType().getGearboxItem(gbx.getRatio());
			ReikaNBTHelper.combineNBT(todrop.stackTagCompound, gbx.getTagsToWriteToStack());
			if (gbx.isUnHarvestable()) {
				todrop = ReikaItemHelper.getSizedItemStack(ItemStacks.scrap, 2+par5Random.nextInt(12));
			}
			ReikaItemHelper.dropItem(world, x+0.5, y+0.5, z+0.5, todrop);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer ep, int par6, float par7, float par8, float par9)
	{
		if (RotaryCraft.instance.isLocked())
			return false;
		TileEntityGearbox tile = (TileEntityGearbox)world.getTileEntity(x, y, z);
		//if (ep.isSneaking()) {
		if (ep.getCurrentEquippedItem() != null && KeyWatcher.instance.isKeyDown(ep, Key.LCTRL) && ep.getCurrentEquippedItem().getItem() == Items.bucket) {
			tile.clearLubricant();
			return true;
		}
		//}

		if (ep.getCurrentEquippedItem() != null && (ep.getCurrentEquippedItem().getItem() instanceof ItemScrewdriver || ep.getCurrentEquippedItem().getItem() instanceof ItemMeter || ep.getCurrentEquippedItem().getItem() instanceof ItemDebug)) {
			return false;
		}
		if (tile != null) {
			ItemStack fix = tile.getGearboxType().getPart(GearPart.GEAR);
			ItemStack held = ep.getCurrentEquippedItem();
			if (held != null) {
				if ((ReikaItemHelper.matchStacks(fix, held))) {
					boolean flag = tile.repair(1 + 20 * tile.getRandom().nextInt(18 - tile.getRatio()));
					if (flag && !ep.capabilities.isCreativeMode) {
						int num = held.stackSize;
						if (num > 1)
							ep.inventory.setInventorySlotContents(ep.inventory.currentItem, ReikaItemHelper.getSizedItemStack(fix, num-1));
						else
							ep.inventory.setInventorySlotContents(ep.inventory.currentItem, null);
					}
					return true;
				}
				else if (ReikaItemHelper.matchStacks(held, ItemStacks.lubebucket)) {
					if (tile.getGearboxType().needsLubricant()) {
						int amt = 1000*held.stackSize;
						if (tile.canTakeLubricant(amt)) {
							tile.addLubricant(amt);
							if (!ep.capabilities.isCreativeMode)
								ep.setCurrentItemOrArmor(0, new ItemStack(Items.bucket, held.stackSize, 0));
						}
					}
					return true;
				}
				else if (GearPart.BEARING.isItemOfType(held)) {
					GearboxTypes material = GearboxTypes.getMaterialFromCraftingItem(held);
					if (tile.getGearboxType().acceptsBearingUpgrade(material) && tile.getBearingTier() != material) {
						if (tile.getBearingTier() != tile.getGearboxType())
							ReikaItemHelper.dropItem(world, x+0.5, y+0.5, z+0.5, tile.getBearingTier().getPart(GearPart.BEARING));
						tile.setBearingTier(material);
						if (!ep.capabilities.isCreativeMode) {
							int num = held.stackSize;
							if (num > 1)
								ep.inventory.setInventorySlotContents(ep.inventory.currentItem, ReikaItemHelper.getSizedItemStack(held, num-1));
							else
								ep.inventory.setInventorySlotContents(ep.inventory.currentItem, null);
						}
					}
					return true;
				}
			}
		}

		return super.onBlockActivated(world, x, y, z, ep, par6, par7, par8, par9);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess par1iBlockAccess, int x, int y, int z) {
		this.setFullBlockBounds();
	}

	@Override
	public final ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
	{
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
		TileEntityGearbox gbx = (TileEntityGearbox)world.getTileEntity(x, y, z);
		ItemStack is = gbx.getGearboxType().getGearboxItem(gbx.getRatio());
		ReikaNBTHelper.combineNBT(is.stackTagCompound, gbx.getTagsToWriteToStack());
		ret.add(is);
		return ret;
	}
}
