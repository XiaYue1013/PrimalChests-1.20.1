/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). ContainerPrimalChest and
 * ContainerPrimalChestAdvanced are merged here; the slot layout, slot order and shift-click logic are unchanged.
 */
package com.blamejared.primalchests.inventory;

import com.blamejared.primalchests.ChestTier;
import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class PrimalChestMenu extends AbstractContainerMenu {

    private final ChestTier tier;
    private final int chestSlots;
    @Nullable
    private final PrimalChestBlockEntity tile;

    /**
     * Client side: the contents arrive through the menu sync, like the vanilla chest menu.
     */
    public static PrimalChestMenu client(ChestTier tier, int id, Inventory invPlayer) {
        return new PrimalChestMenu(tier, id, invPlayer, new ItemStackHandler(tier.getSize()), null);
    }

    public PrimalChestMenu(ChestTier tier, int id, Inventory invPlayer, IItemHandler itemHandler, @Nullable PrimalChestBlockEntity tile) {
        super(tier.getMenuType(), id);
        this.tier = tier;
        this.chestSlots = itemHandler.getSlots();
        this.tile = tile;

        // First row of 9, then the remaining slots (the 5 of the advanced chest) centered on the second row
        for(int i = 0; i < Math.min(9, chestSlots); i++) {
            addSlot(new ChestSlot(itemHandler, i, 8 + 18 * i, 18));
        }
        for(int i = 9; i < chestSlots; i++) {
            addSlot(new ChestSlot(itemHandler, i, 8 + 18 * (i - 9) + 36, 36));
        }

        // The hotbar comes before the main inventory in the slot list, as in 1.12
        for(int x = 0; x < 9; x++) {
            addSlot(new Slot(invPlayer, x, 8 + 18 * x, 142));
        }
        for(int y = 0; y < 3; y++) {
            for(int x = 0; x < 9; x++) {
                addSlot(new Slot(invPlayer, x + y * 9 + 9, 8 + 18 * x, 84 + y * 18));
            }
        }
    }

    public ChestTier getTier() {
        return tier;
    }

    @Override
    public boolean stillValid(Player player) {
        // Closes the menu once the chest is gone or the player walked away (8 blocks with the default reach)
        return tile == null || Container.stillValidBlockEntity(tile, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if(slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if(index < chestSlots) {
                if(!moveItemStackTo(itemstack1, chestSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if(!moveItemStackTo(itemstack1, 0, chestSlots, false)) {
                return ItemStack.EMPTY;
            }

            if(itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    /**
     * Stacks are often changed in place (merging, splitting); this makes sure the chest gets saved afterwards.
     */
    private class ChestSlot extends SlotItemHandler {

        ChestSlot(IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if(tile != null) {
                tile.setChanged();
            }
        }
    }
}
