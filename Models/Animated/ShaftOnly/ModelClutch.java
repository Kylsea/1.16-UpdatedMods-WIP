/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
// Date: 11/02/2013 9:54:57 PM
// Template version 1.1
// Java generated by Techne
// Keep in mind that you still need to fill in some blanks
// - ZeuX

package Reika.RotaryCraft.Models.Animated.ShaftOnly;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.tileentity.TileEntity;

import Reika.DragonAPI.Instantiable.Rendering.LODModelPart;
import Reika.RotaryCraft.Base.RotaryModelBase;


public class ModelClutch extends RotaryModelBase
{
	//fields
	LODModelPart Shape1;
	LODModelPart Shape2;
	LODModelPart Shape3;
	LODModelPart Shape4;
	LODModelPart Shape5;
	LODModelPart Shape6;
	LODModelPart Shape7;
	LODModelPart Shape8;
	LODModelPart Shape9;
	LODModelPart Shape10;
	LODModelPart Shape11;
	LODModelPart Shape12;
	LODModelPart Shape13;
	LODModelPart Shape14;
	LODModelPart Shape15;

	public ModelClutch()
	{
		textureWidth = 128;
		textureHeight = 32;

		Shape1 = new LODModelPart(this, 0, 0);
		Shape1.addBox(0F, 0F, 0F, 16, 1, 16);
		Shape1.setRotationPoint(-8F, 23F, -8F);
		Shape1.setTextureSize(128, 32);
		Shape1.mirror = true;
		this.setRotation(Shape1, 0F, 0F, 0F);
		Shape2 = new LODModelPart(this, 64, 0);
		Shape2.addBox(0F, 0F, 0F, 1, 12, 16);
		Shape2.setRotationPoint(7F, 11F, -8F);
		Shape2.setTextureSize(128, 32);
		Shape2.mirror = true;
		this.setRotation(Shape2, 0F, 0F, 0F);
		Shape2.mirror = false;
		Shape3 = new LODModelPart(this, 64, 0);
		Shape3.addBox(0F, 0F, 0F, 1, 12, 16);
		Shape3.setRotationPoint(-8F, 11F, -8F);
		Shape3.setTextureSize(128, 32);
		Shape3.mirror = true;
		this.setRotation(Shape3, 0F, 0F, 0F);
		Shape3.mirror = false;
		Shape4 = new LODModelPart(this, 0, 17);
		Shape4.addBox(0F, 0F, 0F, 14, 6, 1);
		Shape4.setRotationPoint(-7F, 17F, 7F);
		Shape4.setTextureSize(128, 32);
		Shape4.mirror = true;
		this.setRotation(Shape4, 0F, 0F, 0F);
		Shape4.mirror = false;
		Shape5 = new LODModelPart(this, 0, 17);
		Shape5.addBox(0F, 0F, 0F, 14, 6, 1);
		Shape5.setRotationPoint(-7F, 17F, -8F);
		Shape5.setTextureSize(128, 32);
		Shape5.mirror = true;
		this.setRotation(Shape5, 0F, 0F, 0F);
		Shape5.mirror = false;
		Shape6 = new LODModelPart(this, 30, 17);
		Shape6.addBox(0F, 0F, 0F, 1, 3, 1);
		Shape6.setRotationPoint(6F, 14F, 7F);
		Shape6.setTextureSize(128, 32);
		Shape6.mirror = true;
		this.setRotation(Shape6, 0F, 0F, 0F);
		Shape6.mirror = false;
		Shape7 = new LODModelPart(this, 30, 17);
		Shape7.addBox(0F, 0F, 0F, 1, 3, 1);
		Shape7.setRotationPoint(6F, 14F, -8F);
		Shape7.setTextureSize(128, 32);
		Shape7.mirror = true;
		this.setRotation(Shape7, 0F, 0F, 0F);
		Shape7.mirror = false;
		Shape8 = new LODModelPart(this, 30, 17);
		Shape8.addBox(0F, 0F, 0F, 1, 3, 1);
		Shape8.setRotationPoint(-7F, 14F, 7F);
		Shape8.setTextureSize(128, 32);
		Shape8.mirror = true;
		this.setRotation(Shape8, 0F, 0F, 0F);
		Shape8.mirror = false;
		Shape9 = new LODModelPart(this, 30, 17);
		Shape9.addBox(0F, 0F, 0F, 1, 3, 1);
		Shape9.setRotationPoint(-7F, 14F, -8F);
		Shape9.setTextureSize(128, 32);
		Shape9.mirror = true;
		this.setRotation(Shape9, 0F, 0F, 0F);
		Shape9.mirror = false;
		Shape10 = new LODModelPart(this, 42, 17);
		Shape10.addBox(0F, 0F, 0F, 1, 3, 10);
		Shape10.setRotationPoint(7F, 8F, -5F);
		Shape10.setTextureSize(128, 32);
		Shape10.mirror = true;
		this.setRotation(Shape10, 0F, 0F, 0F);
		Shape10.mirror = false;
		Shape11 = new LODModelPart(this, 42, 17);
		Shape11.addBox(0F, 0F, 0F, 1, 3, 10);
		Shape11.setRotationPoint(-8F, 8F, -5F);
		Shape11.setTextureSize(128, 32);
		Shape11.mirror = true;
		this.setRotation(Shape11, 0F, 0F, 0F);
		Shape11.mirror = false;
		Shape12 = new LODModelPart(this, 0, 27);
		Shape12.addBox(0F, 0F, 0F, 17, 2, 2);
		Shape12.setRotationPoint(-8.5F, 15F, -1F);
		Shape12.setTextureSize(128, 32);
		Shape12.mirror = true;
		this.setRotation(Shape12, 0F, 0F, 0F);
		Shape12.mirror = false;
		Shape13 = new LODModelPart(this, 0, 27);
		Shape13.addBox(0F, 0F, 0F, 17, 2, 2);
		Shape13.setRotationPoint(-8.5F, 16F, -1.4F);
		Shape13.setTextureSize(128, 32);
		Shape13.mirror = true;
		this.setRotation(Shape13, 0.7853982F, 0F, 0F);
		Shape13.mirror = false;
		Shape14 = new LODModelPart(this, 104, 0);
		Shape14.addBox(0F, 0F, 0F, 6, 6, 6);
		Shape14.setRotationPoint(-3F, 13F, -3F);
		Shape14.setTextureSize(128, 32);
		Shape14.mirror = true;
		this.setRotation(Shape14, 0F, 0F, 0F);
		Shape15 = new LODModelPart(this, 95, 0);
		Shape15.addBox(0F, 0F, 0F, 8, 4, 8);
		Shape15.setRotationPoint(-4F, 19F, -4F);
		Shape15.setTextureSize(128, 32);
		Shape15.mirror = true;
		this.setRotation(Shape15, 0F, 0F, 0F);
	}

	@Override
	public void renderAll(TileEntity te, ArrayList li, float phi, float theta)
	{
		Shape1.render(te, f5);
		Shape2.render(te, f5);
		Shape3.render(te, f5);
		Shape4.render(te, f5);
		Shape5.render(te, f5);
		Shape6.render(te, f5);
		Shape7.render(te, f5);
		Shape8.render(te, f5);
		Shape9.render(te, f5);
		Shape10.render(te, f5);
		Shape11.render(te, f5);
		GL11.glTranslated(0, 1, 0);
		GL11.glRotatef(phi, 1, 0, 0);
		GL11.glTranslated(0, -1, 0);
		Shape12.render(te, f5);
		Shape13.render(te, f5);
		GL11.glTranslated(0, 1, 0);
		GL11.glRotatef(-phi, 1, 0, 0);
		GL11.glTranslated(0, -1, 0);
		Shape14.render(te, f5);
		Shape15.render(te, f5);
	}

}