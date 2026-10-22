/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). The proxies and the GUI handler of
 * 1.12 are replaced by menu types and a client-only event subscriber (client/ClientSetup).
 */
package com.blamejared.primalchests;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

@Mod(MOD_ID)
public class PrimalChests {

    public PrimalChests() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PRegistry.register(modBus);
        modBus.addListener(this::addCreativeTabItems);
    }

    /**
     * 1.12 put both chests in the Decorations tab, which held the chests; in 1.20 chests live in Functional Blocks.
     */
    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(PRegistry.PRIMAL_CHEST_ITEM);
            event.accept(PRegistry.PRIMAL_CHEST_ADVANCED_ITEM);
        }
    }
}
