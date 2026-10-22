/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). The two chests were separate,
 * duplicated classes in 1.12; this enum holds what differed between them.
 */
package com.blamejared.primalchests;

import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

public enum ChestTier {
    /**
     * Primal Chest: one row of 9 slots.
     */
    PRIMAL("primal_chest", 9),
    /**
     * Advanced Primal Chest: a row of 9 slots and a centered row of 5 below it.
     */
    ADVANCED("primal_chest_advanced", 14);

    private final String name;
    private final int size;
    private final ResourceLocation guiTexture;
    private final ResourceLocation modelTexture;

    ChestTier(String name, int size) {
        this.name = name;
        this.size = size;
        this.guiTexture = new ResourceLocation(MOD_ID, "textures/guis/" + name + ".png");
        this.modelTexture = new ResourceLocation(MOD_ID, "textures/models/" + name + ".png");
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public ResourceLocation getGuiTexture() {
        return guiTexture;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    public String getContainerTitleKey() {
        return "container." + MOD_ID + "." + name;
    }

    public BlockEntityType<PrimalChestBlockEntity> getBlockEntityType() {
        return switch(this) {
            case PRIMAL -> PRegistry.PRIMAL_CHEST_TILE.get();
            case ADVANCED -> PRegistry.PRIMAL_CHEST_ADVANCED_TILE.get();
        };
    }

    public MenuType<?> getMenuType() {
        return switch(this) {
            case PRIMAL -> PRegistry.PRIMAL_CHEST_MENU.get();
            case ADVANCED -> PRegistry.PRIMAL_CHEST_ADVANCED_MENU.get();
        };
    }
}
