/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Auxiliary.Interfaces;

import net.minecraft.item.ItemStack;

public interface HandbookEntry {

	public ItemStack getTabIcon();

	public String getData();

	public String getNotes(int subpage);

	public boolean sameTextAllSubpages();

	public String getTitle();

	public boolean hasMachineRender();

	public boolean hasSubpages();

	public int getScreen();

	public int getPage();

	public boolean isConfigDisabled();

}
