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

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;

import Reika.DragonAPI.Base.CoreContainer;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.Auxiliary.Interfaces.RangedEffect;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPowerReceiver;
import Reika.RotaryCraft.Registry.PacketRegistry;
import Reika.RotaryCraft.TileEntities.Weaponry.TileEntityContainment;
import Reika.RotaryCraft.TileEntities.Weaponry.TileEntityForceField;

public class GuiBasicRange extends GuiPowerOnlyMachine
{
	private int range;
	private GuiTextField input;

	public GuiBasicRange(EntityPlayer p5ep, TileEntityPowerReceiver te)
	{
		super(new CoreContainer(p5ep, te), te);
		pwr = te;
		ySize = 46;
		ep = p5ep;
		range = ((RangedEffect)pwr).getRange();
	}

	@Override
	public void initGui() {
		super.initGui();
		int j = (width - xSize) / 2+8;
		int k = (height - ySize) / 2 - 12;
		input = new GuiTextField(fontRendererObj, j+xSize/2-6, k+33, 26, 16);
		input.setFocused(false);
		input.setMaxStringLength(3);
	}

	@Override
	protected void keyTyped(char c, int i){
		super.keyTyped(c, i);
		input.textboxKeyTyped(c, i);
	}

	@Override
	protected void mouseClicked(int i, int j, int k){
		super.mouseClicked(i, j, k);
		input.mouseClicked(i, j, k);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (input.getText().isEmpty()) {
			return;
		}
		if (!(input.getText().matches("^[0-9 ]+$"))) {
			range = 0;
			input.deleteFromCursor(-1);
			if (pwr instanceof TileEntityForceField)
				ReikaPacketHelper.sendPacketToServer(RotaryCraft.packetChannel, PacketRegistry.FORCE.ordinal(), pwr, range);
			else if (pwr instanceof TileEntityContainment)
				ReikaPacketHelper.sendPacketToServer(RotaryCraft.packetChannel, PacketRegistry.CONTAINMENT.ordinal(), pwr, range);
			return;
		}
		range = Integer.parseInt(input.getText());
		if (range >= 0) {
			if (pwr instanceof TileEntityForceField)
				ReikaPacketHelper.sendPacketToServer(RotaryCraft.packetChannel, PacketRegistry.FORCE.ordinal(), pwr, range);
			else if (pwr instanceof TileEntityContainment)
				ReikaPacketHelper.sendPacketToServer(RotaryCraft.packetChannel, PacketRegistry.CONTAINMENT.ordinal(), pwr, range);
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int a, int b)
	{
		super.drawGuiContainerForegroundLayer(a, b);
		fontRendererObj.drawString("Field Radius:", xSize/2-72, 25, 4210752);
		if (!input.isFocused()) {
			fontRendererObj.drawString(String.format("%d", ((RangedEffect)pwr).getRange()), xSize/2+6, 25, 0xffffffff);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3)
	{
		super.drawGuiContainerBackgroundLayer(par1, par2, par3);

		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		input.drawTextBox();
		int color = 4210752;
		if (range > ((RangedEffect)pwr).getMaxRange())
			color = 0xff0000;
		api.drawCenteredStringNoShadow(fontRendererObj, String.format("(%d)", ((RangedEffect)pwr).getRange()), j+xSize/2+58, k+25, color);
	}

	@Override
	protected String getGuiTexture() {
		return "rangegui";
	}
}
