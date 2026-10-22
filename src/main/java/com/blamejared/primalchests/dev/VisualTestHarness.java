package com.blamejared.primalchests.dev;

import com.blamejared.primalchests.ChestTier;
import com.blamejared.primalchests.PRegistry;
import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import com.blamejared.primalchests.blocks.BlockPrimalChest;
import com.blamejared.primalchests.client.gui.PrimalChestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

/**
 * Development only (excluded from the release jar): with {@code -Dprimalchests.visualTest=true} the client creates a
 * superflat world, places the chests, opens them with real right clicks, takes screenshots into run/screenshots
 * and quits. Sounds heard by the client and the screens opened are written to the log.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public final class VisualTestHarness {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("primalchests.visualTest");
    private static final String WORLD = "pc_visual_test";
    private static boolean worldRequested = false;
    private static int ticks = -1;
    private static BlockPos base;

    private VisualTestHarness() {}

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if(!ENABLED || worldRequested || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        try {
            Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
            if(Files.exists(dir)) {
                try(Stream<Path> walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
            }
        } catch(Exception e) {
            LOGGER.error("Visual test: could not delete old world", e);
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(42L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        if(ENABLED && ticks >= 0 && event.getSound() != null) {
            LOGGER.info("Visual test: sound {} at tick {}", event.getSound().getLocation(), ticks);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if(!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if(mc.player == null || mc.level == null || server == null) {
            return;
        }
        ticks++;
        if(mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        mc.getToasts().clear();
        switch(ticks) {
            case 60 -> server.execute(() -> build(server));
            case 100 -> {
                CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FUNCTIONAL_BLOCKS);
                LOGGER.info("Visual test: functional blocks tab has primal={} advanced={}",
                        tab.getDisplayItems().stream().anyMatch(st -> st.is(PRegistry.PRIMAL_CHEST_ITEM.get())),
                        tab.getDisplayItems().stream().anyMatch(st -> st.is(PRegistry.PRIMAL_CHEST_ADVANCED_ITEM.get())));
            }
            // Front row faces the camera (south), the others show every facing
            case 120 -> look(server, new Vec3(0.5, 3.2, -2.5), new Vec3(0.5, 0.2, 5.5));
            case 160 -> shot(mc, "pc_01_overview");
            case 170 -> look(server, new Vec3(0.5, 3.0, 13.5), new Vec3(0.5, 0.2, 5.5));
            case 210 -> shot(mc, "pc_02_overview_back");
            case 220 -> look(server, new Vec3(0.5, 1.9, 1.6), new Vec3(0.5, 0.5, 4.5));
            case 260 -> shot(mc, "pc_03_primal_closeup");
            case 270 -> look(server, new Vec3(0.5, 1.9, 4.6), new Vec3(0.5, 0.5, 7.5));
            case 310 -> shot(mc, "pc_04_advanced_closeup");
            case 320 -> look(server, new Vec3(-2.0, 1.6, 4.0), new Vec3(-3.5, 0.5, 4.5));
            case 360 -> shot(mc, "pc_05_compare_vanilla");
            // Right click the primal chest like a player
            case 370 -> look(server, new Vec3(0.5, 1.9, 2.6), new Vec3(0.5, 0.6, 4.1));
            case 400 -> rightClick(mc, base.offset(0, 0, 4));
            case 410 -> {
                logScreen(mc, "after opening the primal chest");
                hover(mc, 0);
            }
            case 420 -> shot(mc, "pc_06_primal_gui");
            case 425 -> mc.player.closeContainer();
            case 435 -> logScreen(mc, "after closing with the close button/escape");
            // The advanced chest
            case 440 -> look(server, new Vec3(0.5, 1.9, 5.6), new Vec3(0.5, 0.6, 7.1));
            case 470 -> rightClick(mc, base.offset(0, 0, 7));
            case 480 -> {
                logScreen(mc, "after opening the advanced chest");
                hover(mc, 13);
            }
            case 490 -> shot(mc, "pc_07_advanced_gui");
            // Walk away (teleport 12 blocks) with the menu open: the server closes it
            case 495 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ() - 12, player.getYRot(), player.getXRot());
            });
            case 510 -> logScreen(mc, "after walking 12 blocks away");
            // Breaking progress on the block entity rendering
            case 515 -> {
                look(server, new Vec3(0.5, 1.9, 1.6), new Vec3(0.5, 0.5, 4.5));
                server.execute(() -> server.overworld().destroyBlockProgress(4711, base.offset(0, 0, 4), 6));
            }
            case 550 -> shot(mc, "pc_08_breaking_progress");
            case 555 -> server.execute(() -> server.overworld().destroyBlockProgress(4711, base.offset(0, 0, 4), -1));
            // Traditional Chinese
            case 560 -> {
                mc.getLanguageManager().setSelected("zh_tw");
                mc.options.languageCode = "zh_tw";
                mc.reloadResourcePacks();
            }
            case 640 -> look(server, new Vec3(0.5, 1.9, 5.6), new Vec3(0.5, 0.6, 7.1));
            case 670 -> rightClick(mc, base.offset(0, 0, 7));
            case 680 -> hover(mc, 13);
            case 690 -> shot(mc, "pc_09_advanced_gui_zh_tw");
            case 695 -> {
                mc.player.closeContainer();
                mc.getLanguageManager().setSelected("en_us");
                mc.options.languageCode = "en_us";
                mc.reloadResourcePacks();
            }
            default -> {}
        }
        int jeiStart = 780;
        boolean jei = ModList.get().isLoaded("jei");
        if(jei && ticks >= jeiStart && ticks < jeiStart + 2 * 40) {
            int step = (ticks - jeiStart) / 40;
            if((ticks - jeiStart) % 40 == 0) {
                JeiShots.show(step);
            } else if((ticks - jeiStart) % 40 == 30) {
                shot(mc, "pc_jei_" + step);
            }
        } else if(ticks == jeiStart + (jei ? 2 * 40 : 0)) {
            LOGGER.info("Visual test: done, stopping the client");
            mc.stop();
        }
    }

    private static void logScreen(Minecraft mc, String when) {
        String screen = mc.screen == null ? "none" : mc.screen.getClass().getSimpleName();
        String extra = "";
        if(mc.screen instanceof PrimalChestScreen chest) {
            var menu = chest.getMenu();
            long filled = menu.slots.stream().limit(menu.getTier().getSize()).filter(s -> s.hasItem()).count();
            extra = " title='" + chest.getTitle().getString() + "' tier=" + menu.getTier() + " slots=" + menu.slots.size() + " filledChestSlots=" + filled;
        }
        LOGGER.info("Visual test: screen {} {}{}", when, screen, extra);
    }

    /**
     * Puts the mouse over a chest slot so the screenshot shows its tooltip (dev tool: sets the private position).
     */
    private static void hover(Minecraft mc, int slot) {
        if(!(mc.screen instanceof PrimalChestScreen chest)) {
            return;
        }
        var s = chest.getMenu().slots.get(slot);
        double scale = mc.getWindow().getGuiScale();
        double x = (chest.getGuiLeft() + s.x + 8) * scale;
        double y = (chest.getGuiTop() + s.y + 8) * scale;
        try {
            Field xpos = MouseHandler.class.getDeclaredField("xpos");
            Field ypos = MouseHandler.class.getDeclaredField("ypos");
            xpos.setAccessible(true);
            ypos.setAccessible(true);
            xpos.setDouble(mc.mouseHandler, x);
            ypos.setDouble(mc.mouseHandler, y);
        } catch(ReflectiveOperationException e) {
            LOGGER.error("Visual test: could not move the mouse", e);
        }
    }

    private static void rightClick(Minecraft mc, BlockPos pos) {
        mc.player.getInventory().selected = 8;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.2, -0.45), Direction.NORTH, pos, false);
        var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        LOGGER.info("Visual test: right clicked {} -> {}", mc.level.getBlockState(pos), result);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> LOGGER.info("Visual test: {}", msg.getString()));
    }

    /**
     * Puts the player's eyes at {@code eye} looking at {@code target} (both relative to the scene base).
     */
    private static void look(MinecraftServer server, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            Vec3 pos = Vec3.atLowerCornerOf(base).add(eye).subtract(0, 1.62, 0);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(player.serverLevel(), pos.x, pos.y, pos.z, yaw, pitch);
        });
    }

    private static void place(ServerLevel level, BlockPos pos, Block block, Direction facing) {
        level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(BlockPrimalChest.FACING, facing));
    }

    private static void build(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        base = player.blockPosition();
        level.setDayTime(6000);

        Block primal = PRegistry.PRIMAL_CHEST.get();
        Block advanced = PRegistry.PRIMAL_CHEST_ADVANCED.get();
        // Row z+4: primal chests, row z+7: advanced chests; the middle ones face south (towards the camera)
        place(level, base.offset(0, 0, 4), primal, Direction.SOUTH);
        place(level, base.offset(2, 0, 4), primal, Direction.EAST);
        place(level, base.offset(4, 0, 4), primal, Direction.NORTH);
        place(level, base.offset(-2, 0, 4), primal, Direction.WEST);
        place(level, base.offset(0, 0, 7), advanced, Direction.SOUTH);
        place(level, base.offset(2, 0, 7), advanced, Direction.EAST);
        place(level, base.offset(4, 0, 7), advanced, Direction.NORTH);
        place(level, base.offset(-2, 0, 7), advanced, Direction.WEST);
        // A vanilla chest for comparison
        level.setBlockAndUpdate(base.offset(-4, 0, 4), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 3);
        sword.setHoverName(Component.literal("Primal Blade"));
        ItemStack[] items = {sword, new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.COBBLESTONE, 37), new ItemStack(Items.ENDER_PEARL, 16),
                new ItemStack(Items.APPLE, 5), new ItemStack(Items.IRON_INGOT, 12), new ItemStack(Items.REDSTONE, 64), new ItemStack(Items.TORCH, 20),
                new ItemStack(Items.WHEAT_SEEDS, 3), new ItemStack(Items.GOLD_INGOT, 9), new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.BREAD, 7),
                new ItemStack(Items.BOOK), new ItemStack(Items.EMERALD, 48)};
        ItemStackHandler primalItems = ((PrimalChestBlockEntity) level.getBlockEntity(base.offset(0, 0, 4))).getItemStackHandler();
        ItemStackHandler advancedItems = ((PrimalChestBlockEntity) level.getBlockEntity(base.offset(0, 0, 7))).getItemStackHandler();
        for(int i = 0; i < ChestTier.PRIMAL.getSize(); i++) {
            primalItems.setStackInSlot(i, items[i].copy());
        }
        for(int i = 0; i < ChestTier.ADVANCED.getSize(); i++) {
            advancedItems.setStackInSlot(i, items[ChestTier.ADVANCED.getSize() - 1 - i].copy());
        }

        // Hotbar: the item models; slot 9 stays empty for the right clicks
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(PRegistry.PRIMAL_CHEST_ITEM.get()));
        player.getInventory().setItem(1, new ItemStack(PRegistry.PRIMAL_CHEST_ADVANCED_ITEM.get()));
        player.getInventory().setItem(2, new ItemStack(Items.CHEST));
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 10));
        player.getInventory().selected = 0;
        LOGGER.info("Visual test: scene built at {}", base);
    }
}
