package com.pla.spiral_abyss.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** One opened chest's immutable reward roll and independently persisted release cursor. */
public final class ChestPayout {
    private FloorRewards.Bundle pending;
    private int chestTicks = -1;
    private int releasedItems;
    private final Runnable dirty;

    public ChestPayout(Runnable dirty) { this.dirty = Objects.requireNonNull(dirty); }
    public FloorRewards.Bundle pending() { return pending; }
    public int chestTicks() { return chestTicks; }
    public int releasedItems() { return releasedItems; }
    public boolean active() { return pending != null; }
    public boolean begin(FloorRewards.Bundle bundle) {
        Objects.requireNonNull(bundle);
        if (active()) return false;
        pending = bundle; chestTicks = 0; releasedItems = 0; dirty.run();
        return true;
    }
    public void tickOpening() {
        if (active()) { if (chestTicks < Integer.MAX_VALUE) chestTicks++; dirty.run(); }
    }
    public ItemStack takeNextReward() {
        if (pending == null || releasedItems >= pending.items().size()) return null;
        ItemStack stack = pending.items().get(releasedItems++).copy();
        dirty.run();
        return stack;
    }
    /** Call once when releasing the saved XP after the final stack and dispensing effect. */
    public void complete() {
        pending = null; chestTicks = -1; releasedItems = 0; dirty.run();
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (pending != null) {
            tag.put("Pending", pending.save());
            tag.putInt("ChestTicks", chestTicks); tag.putInt("ReleasedItems", releasedItems);
        }
        return tag;
    }
    public static ChestPayout load(CompoundTag tag, Runnable dirty) {
        ChestPayout payout = new ChestPayout(dirty);
        if (tag.contains("Pending")) {
            payout.pending = FloorRewards.Bundle.load(tag.getCompound("Pending"));
            payout.chestTicks = Math.max(0, tag.getInt("ChestTicks"));
            payout.releasedItems = Math.max(0, Math.min(tag.getInt("ReleasedItems"), payout.pending.items().size()));
        }
        return payout;
    }
}
