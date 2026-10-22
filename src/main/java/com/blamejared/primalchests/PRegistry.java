/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). Replaces PBlocks and the
 * GameRegistry/RegistryEvent registration of 1.12 with DeferredRegister.
 */
package com.blamejared.primalchests;

import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import com.blamejared.primalchests.blocks.BlockPrimalChest;
import com.blamejared.primalchests.inventory.PrimalChestMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

public final class PRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<BlockPrimalChest> PRIMAL_CHEST = BLOCKS.register("primal_chest", () -> new BlockPrimalChest(ChestTier.PRIMAL));
    public static final RegistryObject<BlockPrimalChest> PRIMAL_CHEST_ADVANCED = BLOCKS.register("primal_chest_advanced", () -> new BlockPrimalChest(ChestTier.ADVANCED));

    public static final RegistryObject<BlockItem> PRIMAL_CHEST_ITEM = ITEMS.register("primal_chest", () -> new BlockItem(PRIMAL_CHEST.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> PRIMAL_CHEST_ADVANCED_ITEM = ITEMS.register("primal_chest_advanced", () -> new BlockItem(PRIMAL_CHEST_ADVANCED.get(), new Item.Properties()));

    // Same names as the 1.12 tile entity registrations
    public static final RegistryObject<BlockEntityType<PrimalChestBlockEntity>> PRIMAL_CHEST_TILE = TILES.register("tile_primal_chest",
            () -> BlockEntityType.Builder.of((pos, state) -> new PrimalChestBlockEntity(ChestTier.PRIMAL, pos, state), PRIMAL_CHEST.get()).build(null));
    public static final RegistryObject<BlockEntityType<PrimalChestBlockEntity>> PRIMAL_CHEST_ADVANCED_TILE = TILES.register("tile_primal_chest_advanced",
            () -> BlockEntityType.Builder.of((pos, state) -> new PrimalChestBlockEntity(ChestTier.ADVANCED, pos, state), PRIMAL_CHEST_ADVANCED.get()).build(null));

    public static final RegistryObject<MenuType<PrimalChestMenu>> PRIMAL_CHEST_MENU = MENUS.register("primal_chest",
            () -> new MenuType<>((id, inv) -> PrimalChestMenu.client(ChestTier.PRIMAL, id, inv), FeatureFlags.VANILLA_SET));
    public static final RegistryObject<MenuType<PrimalChestMenu>> PRIMAL_CHEST_ADVANCED_MENU = MENUS.register("primal_chest_advanced",
            () -> new MenuType<>((id, inv) -> PrimalChestMenu.client(ChestTier.ADVANCED, id, inv), FeatureFlags.VANILLA_SET));

    private PRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TILES.register(modBus);
        MENUS.register(modBus);
    }
}
