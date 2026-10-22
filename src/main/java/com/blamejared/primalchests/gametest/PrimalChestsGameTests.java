package com.blamejared.primalchests.gametest;

import com.blamejared.primalchests.ChestTier;
import com.blamejared.primalchests.PRegistry;
import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import com.blamejared.primalchests.blocks.BlockPrimalChest;
import com.blamejared.primalchests.inventory.PrimalChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

/**
 * In-game verification of both chests, run with {@code gradlew runGameTestServer}.
 * The arena is an 11x5x11 structure with a stone floor; GameTest places it so the floor is at relative y=1.
 */
@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public class PrimalChestsGameTests {

    private static final String ARENA = "arena";
    private static final BlockPos POS = new BlockPos(5, 2, 5);

    // ---------------------------------------------------------------- helpers

    private static Block block(ChestTier tier) {
        return tier == ChestTier.PRIMAL ? PRegistry.PRIMAL_CHEST.get() : PRegistry.PRIMAL_CHEST_ADVANCED.get();
    }

    private static Item item(ChestTier tier) {
        return tier == ChestTier.PRIMAL ? PRegistry.PRIMAL_CHEST_ITEM.get() : PRegistry.PRIMAL_CHEST_ADVANCED_ITEM.get();
    }

    private static PrimalChestBlockEntity place(GameTestHelper helper, ChestTier tier, BlockPos pos) {
        helper.setBlock(pos, block(tier));
        BlockEntity be = helper.getBlockEntity(pos);
        if(!(be instanceof PrimalChestBlockEntity chest)) {
            throw new IllegalStateException("Expected a primal chest block entity at " + pos + " but was " + be);
        }
        helper.assertTrue(chest.getTier() == tier, "Wrong tier " + chest.getTier());
        helper.assertTrue(be.getType() == tier.getBlockEntityType(), "Wrong block entity type " + be.getType());
        return chest;
    }

    private static PrimalChestMenu menu(PrimalChestBlockEntity chest, Player player) {
        return new PrimalChestMenu(chest.getTier(), 1, player.getInventory(), chest.getItemStackHandler(), chest);
    }

    private static int count(IItemHandler handler, Item item) {
        int total = 0;
        for(int i = 0; i < handler.getSlots(); i++) {
            if(handler.getStackInSlot(i).is(item)) {
                total += handler.getStackInSlot(i).getCount();
            }
        }
        return total;
    }

    /**
     * Everything a player can reach while the menu is open: the chest, the player inventory and the carried stack.
     */
    private static int count(AbstractContainerMenu menu, Player player, IItemHandler chest, Item item) {
        return count(chest, item) + player.getInventory().countItem(item) + (menu.getCarried().is(item) ? menu.getCarried().getCount() : 0);
    }

    private static int droppedCount(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(11, 7, 11).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void fill(IItemHandler handler, Item item, int count) {
        for(int i = 0; i < handler.getSlots(); i++) {
            handler.insertItem(i, new ItemStack(item, count), false);
        }
    }

    // ---------------------------------------------------------------- storage

    private static void capacity(GameTestHelper helper, ChestTier tier) {
        PrimalChestBlockEntity chest = place(helper, tier, POS);
        IItemHandler handler = chest.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.getSlots() == tier.getSize(), "Expected " + tier.getSize() + " slots, got " + handler.getSlots());
        for(Direction side : Direction.values()) {
            IItemHandler sided = chest.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(IllegalStateException::new);
            helper.assertTrue(sided == handler, "Every side must expose the same inventory, " + side + " did not");
        }

        // Normal stack limits: 64 cobblestone, 16 ender pearls, 1 sword per slot
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.ENDER_PEARL, 20), false).getCount() == 4, "Ender pearls stack to 16");
        helper.assertTrue(handler.insertItem(1, new ItemStack(Items.DIAMOND_SWORD, 2), false).getCount() == 1, "Swords do not stack");
        helper.assertTrue(handler.insertItem(2, ItemStack.EMPTY, false).isEmpty(), "Inserting nothing gives nothing back");
        helper.assertTrue(handler.extractItem(3, 64, false).isEmpty(), "An empty slot gives nothing");
        for(int i = 2; i < handler.getSlots(); i++) {
            ItemStack rest = handler.insertItem(i, new ItemStack(Items.COBBLESTONE, 70), false);
            helper.assertTrue(rest.getCount() == 6, "Slot " + i + " should take 64 of 70, left " + rest);
        }
        for(int i = 0; i < handler.getSlots(); i++) {
            ItemStack rest = handler.insertItem(i, new ItemStack(Items.COBBLESTONE), false);
            helper.assertTrue(rest.getCount() == 1, "A full chest must not accept more (slot " + i + ")");
        }
        helper.assertTrue(count(handler, Items.COBBLESTONE) == 64 * (tier.getSize() - 2), "Wrong cobblestone total");
        ItemStack taken = handler.extractItem(5, 100, false);
        helper.assertTrue(taken.getCount() == 64 && handler.getStackInSlot(5).isEmpty(), "Extracting more than there is gives the whole stack");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void primalChestHolds9Stacks(GameTestHelper helper) {
        capacity(helper, ChestTier.PRIMAL);
    }

    @GameTest(template = ARENA)
    public static void advancedChestHolds14Stacks(GameTestHelper helper) {
        capacity(helper, ChestTier.ADVANCED);
    }

    @GameTest(template = ARENA)
    public static void blockProperties(GameTestHelper helper) {
        for(ChestTier tier : ChestTier.values()) {
            Block block = block(tier);
            BlockState state = block.defaultBlockState();
            helper.assertTrue(state.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 2.5F, "Hardness must be 2.5");
            helper.assertTrue(block.getExplosionResistance() == 2.5F, "Explosion resistance must be 2.5");
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), "Axes must be effective");
            helper.assertTrue(state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED, "Rendered by the block entity renderer");
            AABB box = state.getShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty()).bounds();
            helper.assertTrue(box.equals(new AABB(0.0625, 0, 0.0625, 0.9375, 0.875, 0.9375)), "Wrong bounding box " + box);
            helper.assertTrue(state.getValue(BlockPrimalChest.FACING) == Direction.NORTH, "Default facing is north, as the 1.12 tile entity");
            helper.assertTrue(item(tier).getDescriptionId().equals("block.primalchests." + tier.getName()), "Wrong item name key");
            // 1.12 had no comparator output for these chests
            helper.assertTrue(!state.hasAnalogOutputSignal(), "No comparator output");
            // Spectators could not open them in 1.12 (no IInventory), so no menu provider either
            helper.assertTrue(state.getMenuProvider(helper.getLevel(), helper.absolutePos(POS)) == null, "No spectator menu provider");
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- menu

    /**
     * Collects the top-left corners of every slot interior (16x16 of #8B8B8B) of a GUI texture above the player inventory.
     */
    private static Set<String> slotsDrawnOn(ResourceLocation texture) throws Exception {
        String path = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
        Set<String> found = new HashSet<>();
        try(InputStream in = PrimalChestsGameTests.class.getResourceAsStream(path)) {
            BufferedImage image = ImageIO.read(in);
            for(int y = 1; y < 80; y++) {
                for(int x = 1; x < 176 - 16; x++) {
                    if(isSlotColor(image, x, y) && !isSlotColor(image, x - 1, y) && !isSlotColor(image, x, y - 1) && isSlotSquare(image, x, y)) {
                        found.add(x + "," + y);
                    }
                }
            }
        }
        return found;
    }

    private static boolean isSlotColor(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFFFFFF) == 0x8B8B8B;
    }

    private static boolean isSlotSquare(BufferedImage image, int x0, int y0) {
        for(int y = y0; y < y0 + 16; y++) {
            for(int x = x0; x < x0 + 16; x++) {
                if(!isSlotColor(image, x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void layout(GameTestHelper helper, ChestTier tier) throws Exception {
        PrimalChestBlockEntity chest = place(helper, tier, POS);
        Player player = helper.makeMockPlayer();
        PrimalChestMenu menu = menu(chest, player);
        int n = tier.getSize();
        helper.assertTrue(menu.slots.size() == n + 36, "Expected " + (n + 36) + " slots, got " + menu.slots.size());

        Set<String> menuSlots = new HashSet<>();
        for(int i = 0; i < n; i++) {
            Slot slot = menu.slots.get(i);
            int x = i < 9 ? 8 + 18 * i : 44 + 18 * (i - 9);
            int y = i < 9 ? 18 : 36;
            helper.assertTrue(slot.x == x && slot.y == y, "Chest slot " + i + " at " + slot.x + "," + slot.y + " expected " + x + "," + y);
            helper.assertTrue(slot.getContainerSlot() == i, "Chest slot " + i + " maps to index " + slot.getContainerSlot());
            menuSlots.add(slot.x + "," + slot.y);
        }
        for(int x = 0; x < 9; x++) {
            Slot slot = menu.slots.get(n + x);
            helper.assertTrue(slot.container == player.getInventory() && slot.getContainerSlot() == x && slot.y == 142, "Hotbar slot " + x + " is wrong");
        }
        for(int i = 0; i < 27; i++) {
            Slot slot = menu.slots.get(n + 9 + i);
            helper.assertTrue(slot.container == player.getInventory() && slot.getContainerSlot() == 9 + i && slot.y == 84 + (i / 9) * 18, "Inventory slot " + i + " is wrong");
        }

        // Every chest slot sits on a slot drawn by the original GUI texture, and the texture draws no other slot
        Set<String> drawn = slotsDrawnOn(tier.getGuiTexture());
        helper.assertTrue(drawn.equals(menuSlots), "GUI texture draws slots at " + drawn + " but the menu has " + menuSlots);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void primalMenuLayoutMatchesTexture(GameTestHelper helper) throws Exception {
        layout(helper, ChestTier.PRIMAL);
    }

    @GameTest(template = ARENA)
    public static void advancedMenuLayoutMatchesTexture(GameTestHelper helper) throws Exception {
        layout(helper, ChestTier.ADVANCED);
    }

    private static void shiftClick(GameTestHelper helper, ChestTier tier) {
        PrimalChestBlockEntity chest = place(helper, tier, POS);
        IItemHandler handler = chest.getItemStackHandler();
        Player player = helper.makeMockPlayer();
        PrimalChestMenu menu = menu(chest, player);
        int n = tier.getSize();
        player.getInventory().setItem(9, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 40));
        player.getInventory().setItem(20, new ItemStack(Items.DIRT, 30));

        // Player -> chest
        menu.clicked(n + 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(handler.getStackInSlot(0).getCount() == 64 && player.getInventory().getItem(9).isEmpty(), "64 dirt should move into chest slot 0");
        menu.clicked(n, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(handler.getStackInSlot(1).getCount() == 40, "40 hotbar dirt should go to chest slot 1, got " + handler.getStackInSlot(1));
        menu.clicked(n + 9 + 11, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(handler.getStackInSlot(1).getCount() == 64 && handler.getStackInSlot(2).getCount() == 6, "30 dirt should top up slot 1 then use slot 2");
        helper.assertTrue(count(menu, player, handler, Items.DIRT) == 134, "Dirt total changed to " + count(menu, player, handler, Items.DIRT));

        // Chest -> player: filled from the end of the slot list, which in this menu is the last main inventory slot
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().getItem(35).getCount() == 64 && handler.getStackInSlot(0).isEmpty(), "Chest slot 0 should go to inventory slot 35 as in 1.12");

        // A full chest takes nothing and nothing is lost
        fill(handler, Items.STONE, 64);
        int stoneBefore = count(menu, player, handler, Items.STONE);
        player.getInventory().setItem(30, new ItemStack(Items.STONE, 64));
        menu.clicked(n + 9 + 21, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().getItem(30).getCount() == 64, "Stone must stay in the player inventory when the chest is full");
        helper.assertTrue(count(menu, player, handler, Items.STONE) == stoneBefore + 64, "Stone total changed");
        helper.assertTrue(count(menu, player, handler, Items.DIRT) == 134, "Dirt total changed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void primalShiftClick(GameTestHelper helper) {
        shiftClick(helper, ChestTier.PRIMAL);
    }

    @GameTest(template = ARENA)
    public static void advancedShiftClick(GameTestHelper helper) {
        shiftClick(helper, ChestTier.ADVANCED);
    }

    @GameTest(template = ARENA)
    public static void mouseActionsKeepItemCounts(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.ADVANCED, POS);
        IItemHandler handler = chest.getItemStackHandler();
        Player player = helper.makeMockPlayer();
        PrimalChestMenu menu = menu(chest, player);
        int n = ChestTier.ADVANCED.getSize();
        player.getInventory().setItem(9, new ItemStack(Items.OAK_LOG, 33));
        player.getInventory().setItem(2, new ItemStack(Items.GOLD_INGOT, 7));

        // Left click picks up, right click on an empty slot places one, left click places the rest
        menu.clicked(n + 9, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 33, "Carried should be 33 logs");
        menu.clicked(13, 1, ClickType.PICKUP, player);
        helper.assertTrue(handler.getStackInSlot(13).getCount() == 1 && menu.getCarried().getCount() == 32, "Right click places a single item");
        menu.clicked(12, 0, ClickType.PICKUP, player);
        helper.assertTrue(handler.getStackInSlot(12).getCount() == 32 && menu.getCarried().isEmpty(), "Left click places the stack");

        // Right click on a stack takes half (rounded up)
        menu.clicked(12, 1, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 16 && handler.getStackInSlot(12).getCount() == 16, "Right click takes half");

        // Dragging spreads the carried stack evenly over the slots
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, player);
        for(int slot : new int[] {0, 1, 2, 3}) {
            menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, player);
        }
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, player);
        for(int slot = 0; slot < 4; slot++) {
            helper.assertTrue(handler.getStackInSlot(slot).getCount() == 4, "Drag should put 4 logs in slot " + slot + ", got " + handler.getStackInSlot(slot));
        }
        helper.assertTrue(menu.getCarried().isEmpty(), "All carried logs were placed");

        // Double click collects the same item from the menu into the carried stack
        menu.clicked(13, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 1, "Picked the single log");
        menu.clicked(13, 0, ClickType.PICKUP_ALL, player);
        helper.assertTrue(menu.getCarried().getCount() == 33, "Double click should collect all 33 logs, got " + menu.getCarried());

        // Number key swaps a chest slot with a hotbar slot
        menu.clicked(13, 0, ClickType.PICKUP, player);
        menu.clicked(13, 2, ClickType.SWAP, player);
        helper.assertTrue(handler.getStackInSlot(13).is(Items.GOLD_INGOT) && player.getInventory().getItem(2).is(Items.OAK_LOG), "Swap with hotbar slot 3");
        helper.assertTrue(player.getInventory().getItem(2).getCount() == 33 && handler.getStackInSlot(13).getCount() == 7, "Swap kept the counts");

        // Swapping with an empty hotbar slot takes the stack out
        menu.clicked(13, 5, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(5).getCount() == 7 && handler.getStackInSlot(13).isEmpty(), "Swap into an empty hotbar slot");

        helper.assertTrue(count(menu, player, handler, Items.OAK_LOG) == 33, "Log total changed");
        helper.assertTrue(count(menu, player, handler, Items.GOLD_INGOT) == 7, "Gold total changed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void twoPlayersShareOneInventory(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.PRIMAL, POS);
        IItemHandler handler = chest.getItemStackHandler();
        Player alice = helper.makeMockPlayer();
        Player bob = helper.makeMockPlayer();
        PrimalChestMenu aliceMenu = menu(chest, alice);
        PrimalChestMenu bobMenu = menu(chest, bob);
        alice.getInventory().setItem(9, new ItemStack(Items.EMERALD, 10));
        int n = ChestTier.PRIMAL.getSize();

        aliceMenu.clicked(n + 9, 0, ClickType.QUICK_MOVE, alice);
        helper.assertTrue(bobMenu.slots.get(0).getItem().getCount() == 10, "Bob sees what Alice put in");
        bobMenu.clicked(0, 0, ClickType.PICKUP, bob);
        helper.assertTrue(bobMenu.getCarried().getCount() == 10, "Bob took the emeralds");
        aliceMenu.clicked(0, 0, ClickType.PICKUP, alice);
        helper.assertTrue(aliceMenu.getCarried().isEmpty(), "Alice cannot take them as well");
        aliceMenu.clicked(0, 0, ClickType.QUICK_MOVE, alice);
        int total = count(handler, Items.EMERALD) + alice.getInventory().countItem(Items.EMERALD) + bob.getInventory().countItem(Items.EMERALD)
                + aliceMenu.getCarried().getCount() + bobMenu.getCarried().getCount();
        helper.assertTrue(total == 10, "Emerald total is " + total);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void rightClickOpensMenuAndClosesWhenBroken(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.ADVANCED, POS);
        chest.getItemStackHandler().insertItem(0, new ItemStack(Items.IRON_INGOT, 20), false);
        // A server player whose packets go nowhere (the vanilla mock server player needs a network channel)
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "primal-test"));
        Vec3 near = Vec3.atBottomCenterOf(helper.absolutePos(POS.north(2)));
        player.moveTo(near.x, near.y, near.z, 0, 0);

        helper.useBlock(POS, player);
        helper.assertTrue(player.containerMenu instanceof PrimalChestMenu menu && menu.getTier() == ChestTier.ADVANCED, "Right click should open the advanced menu, got " + player.containerMenu);
        PrimalChestMenu menu = (PrimalChestMenu) player.containerMenu;
        helper.assertTrue(menu.stillValid(player), "Menu is valid next to the chest");
        helper.assertTrue(menu.getType() == PRegistry.PRIMAL_CHEST_ADVANCED_MENU.get(), "Wrong menu type");
        helper.assertTrue(menu.slots.get(0).getItem().getCount() == 20, "The menu shows the chest contents");

        // Too far away: invalid
        player.moveTo(near.x, near.y, near.z - 10, 0, 0);
        helper.assertTrue(!menu.stillValid(player), "Menu must close 10 blocks away");
        player.moveTo(near.x, near.y, near.z, 0, 0);
        helper.assertTrue(menu.stillValid(player), "Valid again when back");

        helper.getLevel().destroyBlock(helper.absolutePos(POS), true);
        helper.assertTrue(!menu.stillValid(player), "Menu must be invalid once the chest is gone");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 0 && menu.getCarried().isEmpty(), "The broken chest's items must not be taken through the menu");
        // What ServerPlayer.tick does every tick (FakePlayer does not tick)
        if(!player.containerMenu.stillValid(player)) {
            player.closeContainer();
        }
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "The menu should be closed, still " + player.containerMenu);
        helper.succeedWhen(() -> helper.assertTrue(droppedCount(helper, Items.IRON_INGOT) == 20, "20 iron should be dropped, found " + droppedCount(helper, Items.IRON_INGOT)));
    }

    // ---------------------------------------------------------------- breaking, saving

    private static void breaking(GameTestHelper helper, ChestTier tier) {
        PrimalChestBlockEntity chest = place(helper, tier, POS);
        IItemHandler handler = chest.getItemStackHandler();
        fill(handler, Items.BRICK, 64);
        handler.extractItem(0, 64, false);
        handler.insertItem(0, new ItemStack(Items.EGG, 3), false);
        int bricks = 64 * (tier.getSize() - 1);

        helper.getLevel().destroyBlock(helper.absolutePos(POS), true);
        helper.assertTrue(count(handler, Items.BRICK) == 0 && count(handler, Items.EGG) == 0, "The inventory must be empty after dropping");
        helper.assertBlockNotPresent(block(tier), POS);
        // Wait a few ticks: more items appearing would be a duplication
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(droppedCount(helper, Items.BRICK) == bricks, "Expected " + bricks + " bricks, found " + droppedCount(helper, Items.BRICK));
            helper.assertTrue(droppedCount(helper, Items.EGG) == 3, "Expected 3 eggs, found " + droppedCount(helper, Items.EGG));
            helper.assertTrue(droppedCount(helper, item(tier)) == 1, "The chest itself drops once, found " + droppedCount(helper, item(tier)));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void primalBreakDropsEverythingOnce(GameTestHelper helper) {
        breaking(helper, ChestTier.PRIMAL);
    }

    @GameTest(template = ARENA)
    public static void advancedBreakDropsEverythingOnce(GameTestHelper helper) {
        breaking(helper, ChestTier.ADVANCED);
    }

    @GameTest(template = ARENA)
    public static void replacingTheBlockDropsContents(GameTestHelper helper) {
        // 1.12 dropped the contents in breakBlock, which also ran when the block was replaced (e.g. /setblock)
        PrimalChestBlockEntity chest = place(helper, ChestTier.PRIMAL, POS);
        chest.getItemStackHandler().insertItem(4, new ItemStack(Items.APPLE, 12), false);
        helper.setBlock(POS, Blocks.AIR);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(droppedCount(helper, Items.APPLE) == 12, "Apples should drop when the chest is replaced");
            helper.assertTrue(droppedCount(helper, PRegistry.PRIMAL_CHEST_ITEM.get()) == 0, "Replacing is not mining, the chest item must not drop");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void explosionDropsTheContents(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.ADVANCED, POS);
        chest.getItemStackHandler().insertItem(13, new ItemStack(Items.GOLD_BLOCK, 9), false);
        chest.getItemStackHandler().insertItem(0, new ItemStack(Items.IRON_BLOCK, 4), false);
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(POS));
        helper.getLevel().explode(null, center.x, center.y, center.z, 2.0F, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        helper.assertBlockNotPresent(PRegistry.PRIMAL_CHEST_ADVANCED.get(), POS);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(droppedCount(helper, Items.GOLD_BLOCK) == 9 && droppedCount(helper, Items.IRON_BLOCK) == 4, "The contents survive the explosion");
            helper.assertTrue(droppedCount(helper, PRegistry.PRIMAL_CHEST_ADVANCED_ITEM.get()) <= 1, "The chest drops at most once");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void rotatingKeepsContents(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.PRIMAL, POS);
        chest.getItemStackHandler().insertItem(0, new ItemStack(Items.APPLE, 5), false);
        BlockPos abs = helper.absolutePos(POS);
        helper.getLevel().setBlockAndUpdate(abs, helper.getLevel().getBlockState(abs).setValue(BlockPrimalChest.FACING, Direction.EAST));
        helper.assertTrue(helper.getBlockEntity(POS) == chest, "Changing the facing keeps the block entity");
        helper.assertTrue(count(chest.getItemStackHandler(), Items.APPLE) == 5, "Changing the facing keeps the items");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(droppedCount(helper, Items.APPLE) == 0, "Nothing may drop when only the facing changes");
            helper.succeed();
        });
    }

    private static void saving(GameTestHelper helper, ChestTier tier) {
        PrimalChestBlockEntity chest = place(helper, tier, POS);
        ItemStackHandler handler = chest.getItemStackHandler();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 3);
        sword.setHoverName(net.minecraft.network.chat.Component.literal("Named"));
        sword.setDamageValue(17);
        handler.insertItem(0, sword.copy(), false);
        handler.insertItem(tier.getSize() - 1, new ItemStack(Items.REDSTONE, 63), false);
        handler.insertItem(4, new ItemStack(Items.SNOWBALL, 16), false);

        CompoundTag tag = chest.saveWithFullMetadata();
        helper.assertTrue(tag.contains("items") && tag.getCompound("items").getInt("Size") == tier.getSize(), "Saved under \"items\" like 1.12: " + tag);
        BlockEntity loaded = BlockEntity.loadStatic(chest.getBlockPos(), chest.getBlockState(), tag);
        helper.assertTrue(loaded instanceof PrimalChestBlockEntity, "Loading gives a primal chest, got " + loaded);
        ItemStackHandler copy = ((PrimalChestBlockEntity) loaded).getItemStackHandler();
        helper.assertTrue(copy.getSlots() == tier.getSize(), "Loaded size");
        for(int i = 0; i < tier.getSize(); i++) {
            helper.assertTrue(ItemStack.matches(copy.getStackInSlot(i), handler.getStackInSlot(i)), "Slot " + i + " differs after loading: " + copy.getStackInSlot(i));
        }
        helper.assertTrue(ItemStack.matches(copy.getStackInSlot(0), sword), "Enchantments, name and damage survive");

        // A tag claiming another size (edited, or from another mod) cannot change the capacity
        tag.getCompound("items").putInt("Size", 27);
        PrimalChestBlockEntity resized = (PrimalChestBlockEntity) BlockEntity.loadStatic(chest.getBlockPos(), chest.getBlockState(), tag);
        helper.assertTrue(resized.getItemStackHandler().getSlots() == tier.getSize(), "Capacity must stay " + tier.getSize());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void primalSaveAndLoad(GameTestHelper helper) {
        saving(helper, ChestTier.PRIMAL);
    }

    @GameTest(template = ARENA)
    public static void advancedSaveAndLoad(GameTestHelper helper) {
        saving(helper, ChestTier.ADVANCED);
    }

    @GameTest(template = ARENA)
    public static void changesMarkTheChunkForSaving(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.PRIMAL, POS);
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(POS));
        Player player = helper.makeMockPlayer();
        PrimalChestMenu menu = menu(chest, player);

        chunk.setUnsaved(false);
        chest.getItemStackHandler().insertItem(0, new ItemStack(Items.DIRT, 10), false);
        helper.assertTrue(chunk.isUnsaved(), "Inserting through the capability must mark the chunk");

        // Merging into an existing stack changes it in place, which the 1.12 chest never saved
        chunk.setUnsaved(false);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT, 5));
        menu.clicked(ChestTier.PRIMAL.getSize() + 9, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(chest.getItemStackHandler().getStackInSlot(0).getCount() == 15, "Merged");
        helper.assertTrue(chunk.isUnsaved(), "Shift-click merging must mark the chunk");

        chunk.setUnsaved(false);
        menu.clicked(0, 1, ClickType.PICKUP, player);
        helper.assertTrue(chunk.isUnsaved(), "Taking half a stack must mark the chunk");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void capabilityInvalidatedOnRemoval(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.ADVANCED, POS);
        List<LazyOptional<IItemHandler>> caps = new ArrayList<>();
        caps.add(chest.getCapability(ForgeCapabilities.ITEM_HANDLER, null));
        for(Direction side : Direction.values()) {
            caps.add(chest.getCapability(ForgeCapabilities.ITEM_HANDLER, side));
        }
        helper.assertTrue(caps.stream().allMatch(LazyOptional::isPresent), "Present on every side");
        helper.setBlock(POS, Blocks.AIR);
        helper.assertTrue(caps.stream().noneMatch(LazyOptional::isPresent), "Invalidated after the chest is removed");
        helper.succeed();
    }

    // ---------------------------------------------------------------- placement, pistons, hoppers

    @GameTest(template = ARENA)
    public static void placementFacesThePlayer(GameTestHelper helper) {
        Direction[] looking = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for(int i = 0; i < looking.length; i++) {
            BlockPos floor = new BlockPos(2 + i * 2, 1, 5);
            Player player = helper.makeMockPlayer();
            player.setYRot(looking[i].toYRot());
            ItemStack stack = new ItemStack(PRegistry.PRIMAL_CHEST_ITEM.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos abs = helper.absolutePos(floor);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
            BlockState placed = helper.getBlockState(floor.above());
            helper.assertTrue(placed.is(PRegistry.PRIMAL_CHEST.get()), "Chest not placed for " + looking[i] + ": " + placed);
            helper.assertTrue(placed.getValue(BlockPrimalChest.FACING) == looking[i].getOpposite(), "Looking " + looking[i] + " should face " + looking[i].getOpposite() + ", got " + placed);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void pistonsCannotMoveTheChest(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.PRIMAL, POS);
        chest.getItemStackHandler().insertItem(0, new ItemStack(Items.DIAMOND, 3), false);
        BlockPos piston = POS.west();
        helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(20, () -> {
            helper.assertBlockPresent(PRegistry.PRIMAL_CHEST.get(), POS);
            helper.assertTrue(helper.getBlockEntity(POS) == chest, "Same block entity");
            helper.assertTrue(count(chest.getItemStackHandler(), Items.DIAMOND) == 3 && droppedCount(helper, Items.DIAMOND) == 0, "Contents untouched");
            helper.succeed();
        });
    }

    private static void hoppers(GameTestHelper helper, ChestTier tier) {
        // hopper -> chest -> hopper
        BlockPos chestPos = POS.above();
        PrimalChestBlockEntity chest = place(helper, tier, chestPos);
        chest.getItemStackHandler().insertItem(tier.getSize() - 1, new ItemStack(Items.DIAMOND, 3), false);
        helper.setBlock(chestPos.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(POS, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity top = (HopperBlockEntity) helper.getBlockEntity(chestPos.above());
        HopperBlockEntity bottom = (HopperBlockEntity) helper.getBlockEntity(POS);
        top.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        helper.succeedWhen(() -> {
            int cobble = 0, diamonds = 0;
            for(int i = 0; i < bottom.getContainerSize(); i++) {
                ItemStack s = bottom.getItem(i);
                if(s.is(Items.COBBLESTONE)) cobble += s.getCount();
                if(s.is(Items.DIAMOND)) diamonds += s.getCount();
            }
            helper.assertTrue(top.isEmpty(), "Top hopper should empty into the chest");
            helper.assertTrue(cobble == 5 && diamonds == 3, "Bottom hopper has " + cobble + " cobblestone and " + diamonds + " diamonds");
            helper.assertTrue(count(chest.getItemStackHandler(), Items.COBBLESTONE) + count(chest.getItemStackHandler(), Items.DIAMOND) == 0, "Chest should be emptied");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void primalWorksWithHoppers(GameTestHelper helper) {
        hoppers(helper, ChestTier.PRIMAL);
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void advancedWorksWithHoppers(GameTestHelper helper) {
        hoppers(helper, ChestTier.ADVANCED);
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void hopperStopsAtAFullChest(GameTestHelper helper) {
        PrimalChestBlockEntity chest = place(helper, ChestTier.PRIMAL, POS);
        fill(chest.getItemStackHandler(), Items.STONE, 64);
        helper.setBlock(POS.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity top = (HopperBlockEntity) helper.getBlockEntity(POS.above());
        top.setItem(0, new ItemStack(Items.STONE, 10));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(top.getItem(0).getCount() == 10, "The hopper must keep its items, has " + top.getItem(0));
            helper.assertTrue(count(chest.getItemStackHandler(), Items.STONE) == 64 * 9, "The chest stays at 9 stacks");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- recipes

    private static TransientCraftingContainer grid(Item center, Item... planks) {
        TransientCraftingContainer grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 3, 3);
        for(int i = 0; i < 9; i++) {
            grid.setItem(i, new ItemStack(i == 4 ? center : planks[i % planks.length]));
        }
        return grid;
    }

    private static Optional<CraftingRecipe> craft(GameTestHelper helper, TransientCraftingContainer grid) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
    }

    private static void assertCrafts(GameTestHelper helper, TransientCraftingContainer grid, Item result, String recipe) {
        Optional<CraftingRecipe> found = craft(helper, grid);
        helper.assertTrue(found.isPresent(), "No recipe for " + recipe);
        ItemStack out = found.get().assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(out.is(result) && out.getCount() == 1, recipe + " gave " + out);
        helper.assertTrue(found.get().getId().getNamespace().equals(MOD_ID), recipe + " matched " + found.get().getId());
    }

    @GameTest(template = ARENA)
    public static void recipesMatchTheOriginal(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        Optional<? extends Recipe<?>> primal = manager.byKey(new ResourceLocation(MOD_ID, "primal_chests_primalchest"));
        Optional<? extends Recipe<?>> advanced = manager.byKey(new ResourceLocation(MOD_ID, "primal_chests_primalchestadvanced"));
        helper.assertTrue(primal.isPresent() && advanced.isPresent(), "Both recipes keep their 1.12 ids");

        Item primalChest = PRegistry.PRIMAL_CHEST_ITEM.get();
        Item advancedChest = PRegistry.PRIMAL_CHEST_ADVANCED_ITEM.get();
        // "chest" in the 1.12 ore dictionary was the chest, the trapped chest and the ender chest; "plankWood" any planks
        assertCrafts(helper, grid(Items.CHEST, Items.OAK_PLANKS), primalChest, "chest + oak planks");
        assertCrafts(helper, grid(Items.TRAPPED_CHEST, Items.BIRCH_PLANKS), primalChest, "trapped chest + birch planks");
        assertCrafts(helper, grid(Items.ENDER_CHEST, Items.SPRUCE_PLANKS, Items.CRIMSON_PLANKS), primalChest, "ender chest + mixed planks");
        assertCrafts(helper, grid(primalChest, Items.OAK_PLANKS, Items.JUNGLE_PLANKS), advancedChest, "primal chest + planks");

        helper.assertTrue(craft(helper, grid(advancedChest, Items.OAK_PLANKS)).isEmpty(), "An advanced chest in the middle crafts nothing");
        helper.assertTrue(craft(helper, grid(Items.CHEST, Items.OAK_LOG)).isEmpty(), "Logs are not planks");
        helper.succeed();
    }
}
