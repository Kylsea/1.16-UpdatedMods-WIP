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
import java.util.HashSet;
import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import Reika.DragonAPI.Interfaces.Block.ConnectedTextureGlass;
import Reika.RotaryCraft.ConnectedGlassRenderer;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.Base.BlockBasic;
import Reika.RotaryCraft.Registry.ItemRegistry;

public class BlockBlastGlass extends BlockBasic implements ConnectedTextureGlass {

	private final ArrayList<Integer> allDirs = new ArrayList();
	private IIcon[] edges = new IIcon[10];

	public BlockBlastGlass() {
		super(Material.glass);
		this.setHardness(10F);
		this.setResistance(6000F);
		this.setLightLevel(0F);
		this.setStepSound(soundTypeGlass);
		this.setCreativeTab(RotaryCraft.instance.isLocked() ? null : RotaryCraft.tabRotary);
		this.setHarvestLevel("pickaxe", 3);
		//this.blockIndexInTexture = 74;

		for (int i = 1; i < 10; i++) {
			allDirs.add(i);
		}
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity e)
	{
		return false;
	}

	@Override
	public float getExplosionResistance(Entity e, World world, int x, int y, int z, double eX, double eY, double eZ)
	{
		return 6000F;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess iba, int x, int y, int z, int side) {
		ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[side];
		return iba.getBlock(x+dir.offsetX, y+dir.offsetY, z+dir.offsetZ) != this;
	}

	@Override
	public int getRenderType() {
		return RotaryCraft.proxy.connectedRender;
	}

	@Override
	public boolean canRenderInPass(int pass) {
		ConnectedGlassRenderer.renderPass = pass;
		return pass == 0;
	}

	@Override
	public boolean canDropFromExplosion(Explosion par1Explosion) {
		return false;
	}

	@Override
	public boolean canHarvestBlock(EntityPlayer ep, int meta)
	{
		ItemStack item = ep.inventory.getCurrentItem();
		if (item == null)
			return false;
		if (item.getItem() == Items.diamond_pickaxe || item.getItem() == ItemRegistry.BEDPICK.getItemInstance())
			return true;
		return item.getItem().canHarvestBlock(Blocks.obsidian, item);
	}

	/** This block can only be destroyed by the wither explosions - this in effect makes it witherproof */
	@Override
	public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion ex) {
		world.setBlock(x, y, z, this);
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int damageDropped(int par1)
	{
		return 0;
	}

	@Override
	public int quantityDropped(Random par1Random)
	{
		return 1;
	}

	@Override
	public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		return 74;
	}

	@Override
	public IIcon getIcon(int s, int meta) {
		return icons[0][s];
	}

	@Override
	public void registerBlockIcons(IIconRegister ico) {
		if (RotaryCraft.instance.isLocked())
			return;
		for (int i = 0; i < 6; i++)
			icons[0][i] = ico.registerIcon("RotaryCraft:obsidiglass");

		for (int i = 0; i < 10; i++) {
			edges[i] = ico.registerIcon("rotarycraft:glass/glass_"+i);
		}
	}

	public HashSet<Integer> getEdgesForFace(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		HashSet<Integer> li = new HashSet();
		li.addAll(allDirs);

		if (world.getBlockMetadata(x, y, z) == 1) //clear version
			li.remove(5); //glass tex

		if (face.offsetX != 0) { //test YZ
			//sides; removed if have adjacent on side
			if (world.getBlock(x, y, z+1) == this)
				li.remove(2);
			if (world.getBlock(x, y, z-1) == this)
				li.remove(8);
			if (world.getBlock(x, y+1, z) == this)
				li.remove(4);
			if (world.getBlock(x, y-1, z) == this)
				li.remove(6);

			//Corners; only removed if have adjacent on side AND corner
			if (world.getBlock(x, y+1, z+1) == this && !li.contains(4) && !li.contains(2))
				li.remove(1);
			if (world.getBlock(x, y-1, z-1) == this && !li.contains(6) && !li.contains(8))
				li.remove(9);
			if (world.getBlock(x, y+1, z-1) == this && !li.contains(4) && !li.contains(8))
				li.remove(7);
			if (world.getBlock(x, y-1, z+1) == this && !li.contains(2) && !li.contains(6))
				li.remove(3);
		}
		if (face.offsetY != 0) { //test XZ
			//sides; removed if have adjacent on side
			if (world.getBlock(x, y, z+1) == this)
				li.remove(2);
			if (world.getBlock(x, y, z-1) == this)
				li.remove(8);
			if (world.getBlock(x+1, y, z) == this)
				li.remove(4);
			if (world.getBlock(x-1, y, z) == this)
				li.remove(6);

			//Corners; only removed if have adjacent on side AND corner
			if (world.getBlock(x+1, y, z+1) == this && !li.contains(4) && !li.contains(2))
				li.remove(1);
			if (world.getBlock(x-1, y, z-1) == this && !li.contains(6) && !li.contains(8))
				li.remove(9);
			if (world.getBlock(x+1, y, z-1) == this && !li.contains(4) && !li.contains(8))
				li.remove(7);
			if (world.getBlock(x-1, y, z+1) == this && !li.contains(2) && !li.contains(6))
				li.remove(3);
		}
		if (face.offsetZ != 0) { //test XY
			//sides; removed if have adjacent on side
			if (world.getBlock(x, y+1, z) == this)
				li.remove(4);
			if (world.getBlock(x, y-1, z) == this)
				li.remove(6);
			if (world.getBlock(x+1, y, z) == this)
				li.remove(2);
			if (world.getBlock(x-1, y, z) == this)
				li.remove(8);

			//Corners; only removed if have adjacent on side AND corner
			if (world.getBlock(x+1, y+1, z) == this && !li.contains(2) && !li.contains(4))
				li.remove(1);
			if (world.getBlock(x-1, y-1, z) == this && !li.contains(8) && !li.contains(6))
				li.remove(9);
			if (world.getBlock(x+1, y-1, z) == this && !li.contains(2) && !li.contains(6))
				li.remove(3);
			if (world.getBlock(x-1, y+1, z) == this && !li.contains(4) && !li.contains(8))
				li.remove(7);
		}
		return li;
	}

	public IIcon getIconForEdge(IBlockAccess world, int x, int y, int z, int edge) {
		return edges[edge];
	}

	public IIcon getIconForEdge(int itemMeta, int edge) {
		return edges[edge];
	}

	@Override
	public boolean renderCentralTextureForItem(int meta) {
		return true;
	}
}
