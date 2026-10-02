package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.FloorProgress;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Offline progression and a fixed pool of reusable arena locations. */
public final class AbyssPlayers extends SavedData {
    public static final int SPACING = 512;
    public static final int ARENA_SLOTS = 16;
    private final Set<Integer> placedSlots = new HashSet<>();
    private final Map<UUID, Profile> profiles = new HashMap<>();
    public final class Profile {
        public final UUID owner;
        public int slot;
        public final FloorProgress progress;
        private Profile(UUID owner, int slot, FloorProgress progress) {
            this.owner = owner; this.slot = slot; this.progress = progress;
        }
        public ArenaBounds bounds() {
            if (slot < 0 || slot >= ARENA_SLOTS) throw new IllegalStateException("Profile has no active arena lease");
            return new ArenaBounds(new Vec3((slot % 4 - 1.5) * SPACING + 0.5, 65,
                    (slot / 4 - 1.5) * SPACING + 0.5));
        }
        public boolean placed() { return placedSlots.contains(slot); }
        public void markPlaced() { placedSlots.add(slot); setDirty(); }
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
        for (int slot : tag.getIntArray("PlacedSlots")) data.placedSlots.add(slot);
        for (Tag value : tag.getList("Profiles", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            UUID owner = entry.getUUID("Owner");
            Profile profile = data.new Profile(owner, -1,
                    FloorProgress.load(entry.getCompound("Progress"), data::setDirty));
            data.profiles.put(owner, profile);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putIntArray("PlacedSlots", placedSlots.stream().mapToInt(Integer::intValue).toArray());
        ListTag list = new ListTag();
        profiles.values().forEach(profile -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", profile.owner);
            entry.put("Progress", profile.progress.save()); list.add(entry);
        });
        tag.put("Profiles", list); return tag;
    }
}
