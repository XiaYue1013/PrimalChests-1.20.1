package com.blamejared.primalchests.dev;

import com.blamejared.primalchests.ChestTier;
import com.blamejared.primalchests.PRegistry;
import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

/**
 * Development only (excluded from the release jar): with {@code -Dprimalchests.serverSmokeTest=true} a dedicated
 * server logs what PrimalChests loaded, runs for 200 ticks and stops itself.
 * <p>
 * It also checks that chest contents survive a restart: the first run fills two chests next to spawn, saves and
 * leaves a marker file; the next run loads the world from disk, compares the contents and removes the marker.
 */
@Mod.EventBusSubscriber(modid = MOD_ID)
public final class ServerSmokeTest {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("primalchests.serverSmokeTest");
    private static final String MARKER = "primalchests_restart_test.txt";
    private static int ticks = -1;
    private static boolean wroteChests = false;

    private ServerSmokeTest() {}

    private static BlockPos chestPos(ServerLevel level, ChestTier tier) {
        BlockPos spawn = level.getSharedSpawnPos();
        return new BlockPos(spawn.getX() + tier.ordinal() * 2, 250, spawn.getZ());
    }

    private static ItemStack expected(ChestTier tier, int slot) {
        if(slot == 0) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 2);
            sword.setHoverName(Component.literal("Restart " + tier.getName()));
            sword.setDamageValue(42);
            return sword;
        }
        return switch(slot % 3) {
            case 0 -> new ItemStack(Items.OAK_PLANKS, slot);
            case 1 -> new ItemStack(Items.ENDER_PEARL, 16);
            default -> new ItemStack(Items.REDSTONE, 64 - slot);
        };
    }

    /**
     * Changed after the explicit save, so only marking the chunk dirty and the normal shutdown save can keep it.
     */
    private static ItemStack expectedAfterChange(ChestTier tier, int slot) {
        if(slot == tier.getSize() - 1) {
            return new ItemStack(Items.DIAMOND, 7);
        }
        ItemStack stack = expected(tier, slot);
        if(slot == 1) {
            stack.shrink(5);
        }
        return stack;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if(!ENABLED) {
            return;
        }
        MinecraftServer server = event.getServer();
        var recipes = server.getRecipeManager();
        LOGGER.info("Server smoke test: dedicated={} blocks={},{} menus={},{} recipes={},{}",
                server.isDedicatedServer(), PRegistry.PRIMAL_CHEST.getId(), PRegistry.PRIMAL_CHEST_ADVANCED.getId(),
                PRegistry.PRIMAL_CHEST_MENU.getId(), PRegistry.PRIMAL_CHEST_ADVANCED_MENU.getId(),
                recipes.byKey(new ResourceLocation(MOD_ID, "primal_chests_primalchest")).isPresent(),
                recipes.byKey(new ResourceLocation(MOD_ID, "primal_chests_primalchestadvanced")).isPresent());

        ServerLevel level = server.overworld();
        Path marker = server.getServerDirectory().toPath().resolve(MARKER);
        try {
            if(!Files.exists(marker)) {
                for(ChestTier tier : ChestTier.values()) {
                    BlockPos pos = chestPos(level, tier);
                    Block block = tier == ChestTier.PRIMAL ? PRegistry.PRIMAL_CHEST.get() : PRegistry.PRIMAL_CHEST_ADVANCED.get();
                    level.setBlockAndUpdate(pos, block.defaultBlockState());
                    ItemStackHandler handler = ((PrimalChestBlockEntity) level.getBlockEntity(pos)).getItemStackHandler();
                    for(int i = 0; i < tier.getSize(); i++) {
                        handler.setStackInSlot(i, expected(tier, i));
                    }
                }
                server.saveEverything(false, true, true);
                Files.writeString(marker, "written");
                wroteChests = true;
                LOGGER.info("Server smoke test: restart test chests written and saved, start the server again to verify");
            } else {
                boolean ok = true;
                for(ChestTier tier : ChestTier.values()) {
                    BlockPos pos = chestPos(level, tier);
                    if(!(level.getBlockEntity(pos) instanceof PrimalChestBlockEntity chest) || chest.getTier() != tier) {
                        LOGGER.error("Server smoke test: RESTART FAIL, no {} at {}", tier.getName(), pos);
                        ok = false;
                        continue;
                    }
                    ItemStackHandler handler = chest.getItemStackHandler();
                    for(int i = 0; i < tier.getSize(); i++) {
                        if(!ItemStack.matches(handler.getStackInSlot(i), expectedAfterChange(tier, i))) {
                            LOGGER.error("Server smoke test: RESTART FAIL, {} slot {} is {} expected {}", tier.getName(), i, handler.getStackInSlot(i), expectedAfterChange(tier, i));
                            ok = false;
                        }
                    }
                    LOGGER.info("Server smoke test: {} at {} loaded with {} slots", tier.getName(), pos, handler.getSlots());
                }
                LOGGER.info("Server smoke test: restart test {}", ok ? "PASSED, contents identical after restart" : "FAILED");
                Files.delete(marker);
            }
        } catch(Exception e) {
            LOGGER.error("Server smoke test: restart test error", e);
        }
        ticks = 0;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if(!ENABLED || ticks < 0 || event.phase != TickEvent.Phase.END) {
            return;
        }
        ++ticks;
        if(ticks == 100 && wroteChests) {
            ServerLevel level = event.getServer().overworld();
            for(ChestTier tier : ChestTier.values()) {
                ItemStackHandler handler = ((PrimalChestBlockEntity) level.getBlockEntity(chestPos(level, tier))).getItemStackHandler();
                handler.extractItem(1, 5, false);
                handler.setStackInSlot(tier.getSize() - 1, ItemStack.EMPTY);
                handler.insertItem(tier.getSize() - 1, new ItemStack(Items.DIAMOND, 7), false);
            }
            LOGGER.info("Server smoke test: changed the chests after saving, relying on the shutdown save");
        }
        if(ticks == 200) {
            LOGGER.info("Server smoke test: ran 200 ticks, stopping");
            event.getServer().halt(false);
        }
    }
}
