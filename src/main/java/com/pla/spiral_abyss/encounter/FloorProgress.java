package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorParser;
import net.minecraft.nbt.*;
import java.util.*;

/** Per-player progression stored in AbyssPlayers, including an unclaimed chest's exact contents. */
public final class FloorProgress {
    private int floor = 1;
    private int completed;
    private int claimed;
    private FloorRewards.Bundle pending;
    private final Set<UUID> beneficiaries = new HashSet<>();
    private int chestTicks = -1;
    private int releasedItems;
    private final Runnable dirty;
    public FloorProgress(Runnable dirty) { this.dirty = dirty; }
    public int floor() { return floor; }
    public boolean completed() { return (completed & (1 << floor)) != 0; }
    public boolean claimed() { return (claimed & (1 << floor)) != 0; }
    public boolean claimed(int number) { return number >= 1 && number <= FloorParser.MAX_FLOORS && (claimed & (1 << number)) != 0; }
    public boolean unlocked(int number) {
        if (number < 1 || number > FloorParser.MAX_FLOORS) return false;
        if (claimed(number)) return true;
        for (int previous = 1; previous < number; previous++) if (!claimed(previous)) return false;
        return true;
    }
    public int claimedMask() { return claimed; }
    public FloorRewards.Bundle pending() { return pending; }
    public Set<UUID> beneficiaries() { return Set.copyOf(beneficiaries); }
    public int chestTicks() { return chestTicks; }
    public int releasedItems() { return releasedItems; }
    public void markBeneficiariesClaimed() { beneficiaries.clear(); dirty.run(); }
    public net.minecraft.world.item.ItemStack takeNextReward() {
        if (pending == null || releasedItems >= pending.items().size()) return null;
        var stack = pending.items().get(releasedItems++).copy();
        dirty.run();
        return stack;
    }
    public boolean canSelectFloor(int number) {
        return unlocked(number) && (number == floor || pending == null);
    }
    public void selectFloor(int number) {
        if (!canSelectFloor(number)) throw new IllegalStateException("Floor is locked or another chest is pending");
        if (number == floor) return;
        floor = number; beneficiaries.clear(); chestTicks = -1; dirty.run();
    }
    public void claimOpenedChest() {
        int elapsed = chestTicks;
        if (pending != null) { completed |= 1 << floor; }
        pending = null; beneficiaries.clear(); releasedItems = 0;
        chestTicks = elapsed;
        dirty.run();
    }
    public void finishOpening() { chestTicks = -1; dirty.run(); }
    public void victory(FloorRewards.Bundle bundle, Collection<UUID> party) {
        completed |= 1 << floor; pending = bundle; releasedItems = 0;
        beneficiaries.clear(); beneficiaries.addAll(party);
        chestTicks = -1; dirty.run();
    }
    public void syncClaim(int beatenFloor) {
        completed |= 1 << beatenFloor; claimed |= 1 << beatenFloor;
        // A guest's claim must not move their own arena's selected floor or overwrite another chest.
        dirty.run();
    }
    public boolean beginOpening() {
        if (!completed() || pending == null || chestTicks >= 0) return false;
        chestTicks = 0; dirty.run(); return true;
    }
    public void tickOpening() { if (chestTicks >= 0) { chestTicks++; dirty.run(); } }
    public void advance() {
        if (!claimed() || pending != null || !unlocked(floor + 1)) throw new IllegalStateException("Claim the current floor first");
        floor++; pending = null; beneficiaries.clear(); chestTicks = -1; dirty.run();
    }
    public void reset() {
        floor = 1; completed = claimed = releasedItems = 0; pending = null; beneficiaries.clear(); chestTicks = -1; dirty.run();
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Floor", floor); tag.putInt("Completed", completed); tag.putInt("Claimed", claimed);
        tag.putInt("ChestTicks", chestTicks);
        tag.putInt("ReleasedItems", releasedItems);
        if (pending != null) tag.put("Pending", pending.save());
        ListTag members = new ListTag();
        beneficiaries.forEach(id -> members.add(StringTag.valueOf(id.toString())));
        tag.put("Beneficiaries", members); return tag;
    }
    public static FloorProgress load(CompoundTag tag, Runnable dirty) {
        FloorProgress data = new FloorProgress(dirty);
        data.floor = Math.max(1, Math.min(FloorParser.MAX_FLOORS, tag.getInt("Floor")));
        data.completed = tag.getInt("Completed"); data.claimed = tag.getInt("Claimed");
        data.chestTicks = tag.contains("ChestTicks") ? tag.getInt("ChestTicks") : -1;
        if (tag.contains("Pending")) data.pending = FloorRewards.Bundle.load(tag.getCompound("Pending"));
        data.releasedItems = data.pending == null ? 0 : Math.max(0, Math.min(tag.getInt("ReleasedItems"), data.pending.items().size()));
        for (Tag id : tag.getList("Beneficiaries", Tag.TAG_STRING)) data.beneficiaries.add(UUID.fromString(id.getAsString()));
        return data;
    }
}
