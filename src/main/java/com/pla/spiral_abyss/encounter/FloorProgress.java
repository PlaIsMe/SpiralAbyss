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
    private final Runnable dirty;
    public FloorProgress(Runnable dirty) { this.dirty = dirty; }
    public int floor() { return floor; }
    public boolean completed() { return (completed & (1 << floor)) != 0; }
    public boolean claimed() { return (claimed & (1 << floor)) != 0; }
    public int claimedMask() { return claimed; }
    public FloorRewards.Bundle pending() { return pending; }
    public Set<UUID> beneficiaries() { return Set.copyOf(beneficiaries); }
    public int chestTicks() { return chestTicks; }
    public boolean canSelectFloor(int number) {
        return number >= 1 && number <= FloorParser.MAX_FLOORS && (number == floor || pending == null);
    }
    public void selectFloor(int number) {
        if (!canSelectFloor(number)) throw new IllegalStateException("Claim pending rewards before changing floors");
        if (number == floor) return;
        floor = number; beneficiaries.clear(); chestTicks = -1; dirty.run();
    }
    public void claimOpenedChest() {
        int elapsed = chestTicks;
        syncClaim(floor);
        chestTicks = elapsed;
        dirty.run();
    }
    public void finishOpening() { chestTicks = -1; dirty.run(); }
    public void victory(FloorRewards.Bundle bundle, Collection<UUID> party) {
        completed |= 1 << floor; pending = bundle;
        beneficiaries.clear(); beneficiaries.addAll(party);
        chestTicks = -1; dirty.run();
    }
    public void syncClaim(int beatenFloor) {
        completed |= 1 << beatenFloor; claimed |= 1 << beatenFloor;
        if (beatenFloor >= floor) {
            floor = beatenFloor; pending = null; beneficiaries.clear(); chestTicks = -1;
        }
        dirty.run();
    }
    public boolean beginOpening() {
        if (!completed() || claimed() || pending == null || chestTicks >= 0) return false;
        chestTicks = 0; dirty.run(); return true;
    }
    public void tickOpening() { if (chestTicks >= 0) { chestTicks++; dirty.run(); } }
    public void advance() {
        if (!claimed() || floor >= FloorParser.MAX_FLOORS) throw new IllegalStateException("Claim the current floor first");
        floor++; pending = null; beneficiaries.clear(); chestTicks = -1; dirty.run();
    }
    public void reset() {
        floor = 1; completed = claimed = 0; pending = null; beneficiaries.clear(); chestTicks = -1; dirty.run();
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Floor", floor); tag.putInt("Completed", completed); tag.putInt("Claimed", claimed);
        tag.putInt("ChestTicks", chestTicks);
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
        for (Tag id : tag.getList("Beneficiaries", Tag.TAG_STRING)) data.beneficiaries.add(UUID.fromString(id.getAsString()));
        return data;
    }
}
