package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorParser;
import net.minecraft.nbt.*;
import java.util.*;

/** Committed personal stars/claims and exact, already-opened arena chest payouts. */
public final class FloorProgress {
    private int floor = 1;
    private final int[][] stars = new int[FloorParser.MAX_FLOORS + 1][3];
    private final int[] tiers = new int[FloorParser.MAX_FLOORS + 1];
    private final ChestPayout[][] payouts = new ChestPayout[FloorParser.MAX_FLOORS + 1][3];
    private final Runnable dirty;

    public FloorProgress(Runnable dirty) {
        this.dirty = Objects.requireNonNull(dirty);
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++)
            for (int index = 0; index < 3; index++) payouts[number][index] = new ChestPayout(dirty);
    }
    public int floor() { return floor; }
    public boolean completed() { return claimed(); }
    public boolean claimed() { return claimed(floor); }
    public boolean claimed(int number) { return validFloor(number) && tiers[number] != 0; }
    public boolean unlocked(int number) {
        if (!validFloor(number)) return false;
        if (claimed(number)) return true;
        for (int previous = 1; previous < number; previous++) if (!claimed(previous)) return false;
        return true;
    }
    public int claimedMask() {
        int mask = 0;
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++) if (claimed(number)) mask |= 1 << number;
        return mask;
    }
    public int masteredMask() {
        int mask = 0;
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++) if (totalStars(number) == 9) mask |= 1 << number;
        return mask;
    }
    public int[] waveStars(int number) { requireFloor(number); return stars[number].clone(); }
    public int totalStars(int number) { return Arrays.stream(waveStars(number)).sum(); }
    public int[] projectedStars(int number, int[] attemptStars) {
        requireStars(attemptStars);
        int[] projected = waveStars(number);
        for (int index = 0; index < 3; index++) projected[index] = Math.max(projected[index], attemptStars[index]);
        return projected;
    }
    public boolean tierClaimed(int number, int tier) {
        requireFloor(number); requireTier(tier);
        return (tiers[number] & (1 << (tier - 1))) != 0;
    }
    /** A caller must determine current, captured party eligibility before committing a payout. */
    public boolean commitClaim(int number, int tier, int[] attemptStars) {
        requireTier(tier);
        int[] projected = projectedStars(number, attemptStars);
        if (tierClaimed(number, tier) || Arrays.stream(projected).sum() < tier * 3) return false;
        stars[number] = projected;
        tiers[number] |= 1 << (tier - 1);
        dirty.run();
        return true;
    }
    /** Explicit acknowledgement of an already-claimed reward can save an improved replay. */
    public void recordStars(int number, int[] attemptStars) {
        int[] projected = projectedStars(number, attemptStars);
        if (!claimed(number)) throw new IllegalStateException("Claim a reward before recording stars");
        if (!Arrays.equals(stars[number], projected)) { stars[number] = projected; dirty.run(); }
    }
    public ChestPayout payout(int tier) { return payout(floor, tier); }
    public ChestPayout payout(int number, int tier) { requireFloor(number); requireTier(tier); return payouts[number][tier - 1]; }
    public boolean hasPayouts() { return hasPayouts(floor); }
    public boolean hasPayouts(int number) { requireFloor(number); return Arrays.stream(payouts[number]).anyMatch(ChestPayout::active); }
    public int pendingFloor() {
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++)
            for (ChestPayout payout : payouts[number]) if (payout.active()) return number;
        return 0;
    }
    /** Defer opened payouts only when every originating floor has a committed personal claim. */
    public int requiredPayoutFloor() {
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++)
            if (hasPayouts(number) && !claimed(number)) return pendingFloor();
        return 0;
    }
    /** A new visit has no live reward roster; saved claims still authorize deferred payouts. */
    public boolean canEnterFloor(int number) {
        int required = requiredPayoutFloor();
        return (required == 0 || number == required) && canSelectFloor(number, true);
    }
    /** Only the selected floor's opened bundle can animate or authorize its chest. */
    public FloorRewards.Bundle pending() {
        for (ChestPayout payout : payouts[floor]) if (payout.pending() != null) return payout.pending();
        return null;
    }
    public int chestTicks() { return Arrays.stream(payouts[floor]).mapToInt(ChestPayout::chestTicks).max().orElse(-1); }
    public boolean canSelectFloor(int number) { return canSelectFloor(number, false); }
    public boolean canSelectFloor(int number, boolean acknowledgedCurrentFloor) {
        return unlocked(number) && (number == floor || hasPayouts(number) || !hasPayouts()
                || acknowledgedCurrentFloor && claimed(floor));
    }
    public void selectFloor(int number) {
        selectFloor(number, false);
    }
    public void selectFloor(int number, boolean acknowledgedCurrentFloor) {
        if (!canSelectFloor(number, acknowledgedCurrentFloor)) throw new IllegalStateException("Floor is locked or another chest is pending");
        if (number != floor) { floor = number; dirty.run(); }
    }
    public void advance() {
        if (!claimed() || hasPayouts() || !unlocked(floor + 1)) throw new IllegalStateException("Claim the current floor first");
        floor++; dirty.run();
    }
    /** Operator test shortcut: commit perfect results and mark every chest through this floor looted. */
    public void skipThrough(int number) {
        requireFloor(number);
        for (int current = 1; current <= number; current++) {
            Arrays.fill(stars[current], 3);
            tiers[current] = 7;
            for (ChestPayout payout : payouts[current]) if (payout.active()) payout.complete();
        }
        floor = Math.max(floor, Math.min(number + 1, FloorParser.MAX_FLOORS));
        dirty.run();
    }
    public void reset() {
        floor = 1;
        for (int[] waveStars : stars) Arrays.fill(waveStars, 0);
        Arrays.fill(tiers, 0);
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++)
            for (ChestPayout payout : payouts[number]) payout.complete();
        dirty.run();
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Floor", floor);
        tag.putInt("StarProgressVersion", 1);
        ListTag floors = new ListTag();
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Floor", number); entry.putIntArray("Stars", stars[number]); entry.putInt("Tiers", tiers[number]);
            floors.add(entry);
        }
        tag.put("StarFloors", floors);
        ListTag chests = new ListTag();
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++)
            for (int tier = 1; tier <= 3; tier++) if (payout(number, tier).active()) {
                CompoundTag entry = payout(number, tier).save();
                entry.putInt("Floor", number); entry.putInt("Tier", tier); chests.add(entry);
            }
        tag.put("Payouts", chests);
        return tag;
    }
    public static FloorProgress load(CompoundTag tag, Runnable dirty) {
        FloorProgress data = new FloorProgress(dirty);
        data.floor = Math.max(1, Math.min(FloorParser.MAX_FLOORS, tag.getInt("Floor")));
        if (tag.contains("StarProgressVersion")) {
            for (Tag value : tag.getList("StarFloors", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) value;
                int number = entry.getInt("Floor");
                if (!validFloor(number)) continue;
                int[] savedStars = entry.getIntArray("Stars");
                for (int index = 0; index < Math.min(3, savedStars.length); index++)
                    data.stars[number][index] = Math.max(0, Math.min(3, savedStars[index]));
                data.tiers[number] = entry.getInt("Tiers") & 7;
            }
            for (Tag value : tag.getList("Payouts", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) value;
                int tier = entry.getInt("Tier");
                int number = entry.contains("Floor") ? entry.getInt("Floor") : data.floor;
                if (validFloor(number) && tier >= 1 && tier <= 3)
                    data.payouts[number][tier - 1] = ChestPayout.load(entry, dirty);
            }
        } else {
            // Legacy completion alone was recorded at victory; only actual claims carry forward.
            int claimed = tag.getInt("Claimed");
            for (int number = 1; number <= FloorParser.MAX_FLOORS; number++) if ((claimed & (1 << number)) != 0) {
                Arrays.fill(data.stars[number], 1); data.tiers[number] = 1;
            }
            // Unclicked legacy offers are deliberately abandoned. Opened payouts retain the exact cursor.
            if (tag.contains("Pending") && tag.contains("ChestTicks") && tag.getInt("ChestTicks") >= 0)
                data.payouts[data.floor][0] = ChestPayout.load(tag, dirty);
        }
        return data;
    }
    private static boolean validFloor(int number) { return number >= 1 && number <= FloorParser.MAX_FLOORS; }
    private static void requireFloor(int number) { if (!validFloor(number)) throw new IllegalArgumentException("Invalid floor: " + number); }
    private static void requireTier(int tier) { if (tier < 1 || tier > 3) throw new IllegalArgumentException("Invalid chest tier: " + tier); }
    private static void requireStars(int[] values) {
        if (values == null || values.length != 3 || Arrays.stream(values).anyMatch(value -> value < 0 || value > 3))
            throw new IllegalArgumentException("Expected three wave star counts between zero and three");
    }
}
