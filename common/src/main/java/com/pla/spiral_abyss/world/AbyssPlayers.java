package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.FloorProgress;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import com.pla.spiral_abyss.encounter.ChestPayout;

/** Offline progression and a fixed pool of reusable arena locations. */
public final class AbyssPlayers extends SavedData {
    public static final int SPACING = 512;
    public static final int ARENA_SLOTS = 16;
    private static final int ARENA_LAYOUT_VERSION = 1;
    private final Set<Integer> placedSlots = new HashSet<>();
    private final Set<Integer> legacySlots = new HashSet<>();
    private final Map<UUID, Profile> profiles = new HashMap<>();
    private AbyssCycle cycle = new AbyssCycle();
    public AbyssCycle cycle() { return cycle; }
    public void resetAllProgress() {
        profiles.values().forEach(profile -> {
            profile.progress.reset(); profile.followingHost = null; profile.personalRun = false;
            profile.endlessMode = false; profile.endlessWave = 1; profile.endlessCheckpoint = 0; profile.endlessRewardedWave = 0;
            profile.endlessPayout = new ChestPayout(this::setDirty);
            profile.portal = null; profile.cycleReturnRequired = true; profile.requestArenaReset();
        });
        setDirty();
    }
    public final class Profile {
        public final UUID owner;
        public int slot;
        public final FloorProgress progress;
        public UUID portal;
        public UUID followingHost;
        /** Personal play remains separate until the player leaves and reenters a physical portal. */
        public boolean personalRun;
        public boolean cycleReturnRequired;
        /** Endless progression belongs only to this original arena owner, independently of normal floor claims. */
        public boolean endlessMode;
        public int endlessWave = 1;
        public int endlessCheckpoint;
        public int endlessRewardedWave;
        public ChestPayout endlessPayout = new ChestPayout(AbyssPlayers.this::setDirty);
        public void setEndlessMode(boolean enabled) { endlessMode = enabled; setDirty(); }
        public void saveEndlessWave(int wave) { endlessWave = Math.max(1, wave); setDirty(); }
        public void saveEndlessCheckpoint(int wave) { endlessCheckpoint = Math.max(0, wave); setDirty(); }
        public void markEndlessRewarded(int wave) { endlessRewardedWave = Math.max(endlessRewardedWave, wave); setDirty(); }
        private boolean arenaResetRequested;
        private Profile(UUID owner, int slot, FloorProgress progress) {
            this.owner = owner; this.slot = slot; this.progress = progress;
        }
        public ArenaBounds bounds() {
            if (slot < 0 || slot >= ARENA_SLOTS) throw new IllegalStateException("Profile has no active arena lease");
            return new ArenaBounds(new Vec3((slot % 4 - 1.5) * SPACING + 0.5, ArenaBounds.FLOOR_Y,
                    (slot / 4 - 1.5) * SPACING + 0.5));
        }
        public boolean placed() { return placedSlots.contains(slot) && !arenaResetRequested; }
        public boolean needsArenaReset() { return arenaResetRequested; }
        public void requestArenaReset() { arenaResetRequested = true; setDirty(); }
        public boolean needsLegacyCleanup() { return legacySlots.contains(slot); }
        public void markPlaced() {
            placedSlots.add(slot); legacySlots.remove(slot); arenaResetRequested = false; setDirty();
        }
    }
    public static AbyssPlayers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AbyssPlayers::load, AbyssPlayers::new, "spiral_abyss_players");
    }
    public Profile profile(UUID owner) {
        return profiles.computeIfAbsent(owner, id -> {
            setDirty(); return new Profile(id, -1, new FloorProgress(this::setDirty));
        });
    }
    public Collection<Profile> profiles() { return List.copyOf(profiles.values()); }
    public static AbyssPlayers load(CompoundTag tag) {
        AbyssPlayers data = new AbyssPlayers();
        data.cycle = AbyssCycle.load(tag.getCompound("Cycle"));
        // Old placement flags refer to the retired arena. Upgrade each slot once when it is next used.
        Set<Integer> destination = tag.getInt("ArenaLayoutVersion") == ARENA_LAYOUT_VERSION ? data.placedSlots : data.legacySlots;
        for (int slot : tag.getIntArray("PlacedSlots")) if (slot >= 0 && slot < ARENA_SLOTS) destination.add(slot);
        for (int slot : tag.getIntArray("LegacySlots")) if (slot >= 0 && slot < ARENA_SLOTS) data.legacySlots.add(slot);
        for (Tag value : tag.getList("Profiles", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            UUID owner = entry.getUUID("Owner");
            Profile profile = data.new Profile(owner, -1,
                    FloorProgress.load(entry.getCompound("Progress"), data::setDirty));
            data.profiles.put(owner, profile);
            if (entry.hasUUID("Portal")) profile.portal = entry.getUUID("Portal");
            if (entry.hasUUID("FollowingHost")) profile.followingHost = entry.getUUID("FollowingHost");
            profile.personalRun = entry.contains("PersonalRun") ? entry.getBoolean("PersonalRun") : profile.followingHost != null;
            profile.arenaResetRequested = entry.getBoolean("ResetArena");
            profile.cycleReturnRequired = entry.getBoolean("CycleReturnRequired");
            profile.endlessMode = entry.getBoolean("EndlessMode");
            profile.endlessWave = Math.max(1, entry.getInt("EndlessWave"));
            profile.endlessCheckpoint = Math.max(0, entry.getInt("EndlessCheckpoint"));
            profile.endlessRewardedWave = Math.max(0, entry.getInt("EndlessRewardedWave"));
            profile.endlessPayout = ChestPayout.load(entry.getCompound("EndlessPayout"), data::setDirty);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.put("Cycle", cycle.save());
        tag.putInt("ArenaLayoutVersion", ARENA_LAYOUT_VERSION);
        tag.putIntArray("PlacedSlots", placedSlots.stream().mapToInt(Integer::intValue).toArray());
        tag.putIntArray("LegacySlots", legacySlots.stream().mapToInt(Integer::intValue).toArray());
        ListTag list = new ListTag();
        profiles.values().forEach(profile -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", profile.owner);
            entry.putBoolean("ResetArena", profile.arenaResetRequested);
            entry.putBoolean("CycleReturnRequired", profile.cycleReturnRequired);
            entry.putBoolean("EndlessMode", profile.endlessMode);
            entry.putInt("EndlessWave", profile.endlessWave);
            entry.putInt("EndlessCheckpoint", profile.endlessCheckpoint);
            entry.putInt("EndlessRewardedWave", profile.endlessRewardedWave);
            entry.put("EndlessPayout", profile.endlessPayout.save());
            if (profile.portal != null) entry.putUUID("Portal", profile.portal);
            if (profile.followingHost != null) entry.putUUID("FollowingHost", profile.followingHost);
            entry.putBoolean("PersonalRun", profile.personalRun);
            entry.put("Progress", profile.progress.save()); list.add(entry);
        });
        tag.put("Profiles", list); return tag;
    }
}
