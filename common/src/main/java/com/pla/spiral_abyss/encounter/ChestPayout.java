package com.pla.spiral_abyss.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/** One opened chest's immutable reward roll and independently persisted release cursor. */
public final class ChestPayout {
    private FloorRewards.Bundle pending;
    private int chestTicks = -1;
    private int releasedItems;
    private boolean container;
    private final Set<UUID> recipients = new HashSet<>();
    private final Runnable dirty;

    public boolean container() { return container; }
    public boolean hasRecipients() { return !recipients.isEmpty(); }
    public boolean allows(UUID player) { return recipients.contains(player); }
    public void addRecipients(Collection<UUID> players) { recipients.addAll(players); dirty.run(); }
    public void useContainer(Collection<UUID> players) {
        container = true; addRecipients(players);
    }
    public boolean empty() {
        return pending == null || pending.items().stream().skip(releasedItems).allMatch(ItemStack::isEmpty);
    }
    public boolean hasRemainingRare() {
        return pending != null && pending.rareItems().stream()
                .anyMatch(index -> index >= releasedItems && !pending.items().get(index).isEmpty());
    }
    public ItemStack item(int index) {
        return pending == null || index < releasedItems || index >= pending.items().size()
                ? ItemStack.EMPTY : pending.items().get(index).copy();
    }
    public void remaining(int index, ItemStack stack) {
        if (pending == null || index < releasedItems || index >= pending.items().size()) return;
        var items = new ArrayList<>(pending.items());
        items.set(index, stack.copy());
        pending = new FloorRewards.Bundle(items, pending.experience(), pending.rareItems());
        dirty.run();
    }
    /** Consume saved XP independently of the items, so reopening a container cannot pay it again. */
    public int takeExperience() {
        if (pending == null || pending.experience() == 0) return 0;
        int experience = pending.experience();
        pending = new FloorRewards.Bundle(pending.items(), 0, pending.rareItems());
        dirty.run();
        return experience;
    }

    public ChestPayout(Runnable dirty) { this.dirty = Objects.requireNonNull(dirty); }
    public FloorRewards.Bundle pending() { return pending; }
    public int chestTicks() { return chestTicks; }
    public int releasedItems() { return releasedItems; }
    public boolean active() { return pending != null; }
    public boolean begin(FloorRewards.Bundle bundle) {
        Objects.requireNonNull(bundle);
        if (active()) return false;
        container = false; recipients.clear();
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
    /** Clear a fully delivered payout; container XP may already have been consumed on opening. */
    public void complete() {
        container = false; recipients.clear();
        pending = null; chestTicks = -1; releasedItems = 0; dirty.run();
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (pending != null) {
            tag.put("Pending", pending.save());
            tag.putBoolean("Container", container);
            ListTag people = new ListTag();
            recipients.forEach(id -> people.add(StringTag.valueOf(id.toString())));
            tag.put("Recipients", people);
            tag.putInt("ChestTicks", chestTicks); tag.putInt("ReleasedItems", releasedItems);
        }
        return tag;
    }
    public static ChestPayout load(CompoundTag tag, Runnable dirty) {
        ChestPayout payout = new ChestPayout(dirty);
        if (tag.contains("Pending")) {
            payout.container = tag.getBoolean("Container");
            for (var value : tag.getList("Recipients", Tag.TAG_STRING)) {
                try { payout.recipients.add(UUID.fromString(value.getAsString())); } catch (IllegalArgumentException ignored) {}
            }
            payout.pending = FloorRewards.Bundle.load(tag.getCompound("Pending"));
            payout.chestTicks = Math.max(0, tag.getInt("ChestTicks"));
            payout.releasedItems = Math.max(0, Math.min(tag.getInt("ReleasedItems"), payout.pending.items().size()));
        }
        return payout;
    }
}
