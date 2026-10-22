/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). TileEntityPrimalChest and
 * TileEntityPrimalChestAdvanced only differed in their slot count and are merged here. Changes from 1.12:
 * the facing is stored in the block state, the inventory marks the tile dirty when it changes (1.12 never did),
 * the item handler capability is a LazyOptional that is invalidated with the block entity, and the contents are no
 * longer sent to clients (nothing on the client reads them; open menus are synced by the menu itself).
 */
package com.blamejared.primalchests.blockentities;

import com.blamejared.primalchests.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrimalChestBlockEntity extends BlockEntity {

    private final ChestTier tier;
    private final ItemStackHandler itemStackHandler;
    private LazyOptional<IItemHandler> itemHandlerCap;

    public PrimalChestBlockEntity(ChestTier tier, BlockPos pos, BlockState state) {
        super(tier.getBlockEntityType(), pos, state);
        this.tier = tier;
        this.itemStackHandler = new ItemStackHandler(tier.getSize()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                // ItemStackHandler would resize itself to the saved "Size"; the chest capacity is fixed
                for(int i = 0; i < stacks.size(); i++) {
                    stacks.set(i, ItemStack.EMPTY);
                }
                ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
                for(int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getInt("Slot");
                    if(slot >= 0 && slot < stacks.size()) {
                        stacks.set(slot, ItemStack.of(itemTag));
                    }
                }
                onLoad();
            }
        };
        this.itemHandlerCap = LazyOptional.of(() -> itemStackHandler);
    }

    public ChestTier getTier() {
        return tier;
    }

    public ItemStackHandler getItemStackHandler() {
        return itemStackHandler;
    }

    /**
     * Spawns every stack in the world and empties the inventory, so a second call (or anything still holding the
     * handler, like an open menu) cannot get the items again.
     */
    public void dropContents() {
        if(level == null || level.isClientSide) {
            return;
        }
        for(int i = 0; i < itemStackHandler.getSlots(); i++) {
            ItemStack stack = itemStackHandler.getStackInSlot(i);
            if(!stack.isEmpty()) {
                itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        // Same key as 1.12
        compound.put("items", itemStackHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        itemStackHandler.deserializeNBT(compound.getCompound("items"));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Exposed on every side (and without a side), as in 1.12
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandlerCap = LazyOptional.of(() -> itemStackHandler);
    }
}
