/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Base;

import java.util.List;
import java.util.Locale;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import Reika.ChromatiCraft.API.Interfaces.EnchantableItem;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.RotaryCraft.Registry.ConfigRegistry;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ItemChargedTool extends ItemRotaryTool implements EnchantableItem {

	public ItemChargedTool(int index) {
		super(index);
		hasSubtypes = true;
		//this.setMaxDamage(0);
	}

	@Override
	public abstract ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer ep);

	@Override
	public boolean onItemUse(ItemStack is, EntityPlayer ep, World world, int x, int y, int z, int s, float a, float b, float c) {
		if (super.onItemUse(is, ep, world, x, y, z, s, a, b, c))
			return true;
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public final void getSubItems(Item par1, CreativeTabs par2CreativeTabs, List par3List) //Adds the metadata blocks to the creative inventory
	{
		par3List.add(new ItemStack(par1, 1, 32000));
	}

	protected final void noCharge() {
		if (ConfigRegistry.CLEARCHAT.getState())
			ReikaChatHelper.clearChat();
		ReikaChatHelper.write("Tool charge is depleted!");
	}

	protected final void warnCharge(ItemStack is) {
		if (ConfigRegistry.CLEARCHAT.getState())
			ReikaChatHelper.clearChat();
		if (is.getItemDamage() == 2) {
			ReikaChatHelper.write("Tool charge is very low (2 kJ)!");
		}
		if (is.getItemDamage() == 4) {
			ReikaChatHelper.write("Tool charge is low (4 kJ)!");
		}
		if (is.getItemDamage() == 16) {
			ReikaChatHelper.write("Tool charge is low (16 kJ)!");
		}
		if (is.getItemDamage() == 32) {
			ReikaChatHelper.write("Tool charge is low (32 kJ)!");
		}
	}

	@Override
	public final String getItemStackDisplayName(ItemStack is) {
		return super.getItemStackDisplayName(is);
	}

	public Result getEnchantValidity(Enchantment e, ItemStack is) {
		return e.getName().toLowerCase(Locale.ENGLISH).contains("soulbound") ? Result.ALLOW : Result.DEFAULT;
	}

	public final EnumEnchantmentType getEnchantingCategory() {
		return null;
	}

}
