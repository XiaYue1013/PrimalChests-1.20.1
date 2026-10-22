/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). Replaces ClientProxy; only loaded on
 * the physical client.
 */
package com.blamejared.primalchests.client;

import com.blamejared.primalchests.PRegistry;
import com.blamejared.primalchests.client.gui.PrimalChestScreen;
import com.blamejared.primalchests.client.model.PrimalChestModel;
import com.blamejared.primalchests.client.render.PrimalChestRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(PRegistry.PRIMAL_CHEST_MENU.get(), PrimalChestScreen::new);
            MenuScreens.register(PRegistry.PRIMAL_CHEST_ADVANCED_MENU.get(), PrimalChestScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PrimalChestModel.LAYER, PrimalChestModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PRegistry.PRIMAL_CHEST_TILE.get(), PrimalChestRenderer::new);
        event.registerBlockEntityRenderer(PRegistry.PRIMAL_CHEST_ADVANCED_TILE.get(), PrimalChestRenderer::new);
    }
}
