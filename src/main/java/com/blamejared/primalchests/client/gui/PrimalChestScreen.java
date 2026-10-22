/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). GuiPrimalChest and
 * GuiPrimalChestAdvanced are merged here; textures, size and labels are unchanged (the titles became translatable).
 */
package com.blamejared.primalchests.client.gui;

import com.blamejared.primalchests.inventory.PrimalChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PrimalChestScreen extends AbstractContainerScreen<PrimalChestMenu> {

    private final Inventory player;

    public PrimalChestScreen(PrimalChestMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.player = invPlayer;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 4210752, false);
        // 1.0.3 labels the player inventory with the player's name instead of "Inventory"
        graphics.drawString(font, player.player.getDisplayName(), 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = (width - imageWidth) / 2;
        int j = (height - imageHeight) / 2;
        graphics.blit(menu.getTier().getGuiTexture(), i, j, 0, 0, imageWidth, 166);
    }
}
