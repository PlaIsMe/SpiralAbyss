package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.WaveComposition;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** World-wide wall clock and mob pool snapshot, saved atomically with player progress. */
public final class AbyssCycle {
    public static final long DAY_MILLIS = 86_400_000L;
    private long startedAt;
    private long seed;
    private AbyssConfig.Pools pools;
    private final Map<String, List<FloorDefinition.Enemy>> lineups = new TreeMap<>();
    public boolean initialized() { return pools != null; }
    public AbyssConfig.Pools pools() { return pools; }
    public long deadline(int days) { return days <= 0 ? -1 : startedAt + days * DAY_MILLIS; }
    public long remainingMillis(long now, int days) { return days <= 0 ? -1 : Math.max(0, deadline(days) - now); }
    public boolean due(long now, int days) { return initialized() && days > 0 && now >= deadline(days); }
    public long nextStart(long now, int days) {
        long interval = days * DAY_MILLIS;
        return startedAt + Math.max(1, (now - startedAt) / interval) * interval;
    }
    public void start(long now, long newSeed, AbyssConfig.Pools snapshot) {
        startedAt = now; seed = newSeed; pools = snapshot; lineups.clear();
    }
    public List<FloorDefinition.Enemy> lineup(int floor, int waveNumber, FloorDefinition.Wave wave,
                                            java.util.function.Predicate<ResourceLocation> available,
                                            AbyssConfig.Pools fallback) {
        // Intentional datapack composition edits get their own cache entry; attribute edits retain selections.
        String key = floor + ":" + waveNumber + ":" + wave.bosses();
        for (MobCategory category : MobCategory.values()) key += ":" + wave.categories().getOrDefault(category, 0);
        List<FloorDefinition.Enemy> existing = lineups.get(key);
        if (existing == null) {
            var usable = usablePools(available, fallback);
            existing = WaveComposition.resolve(wave, usable, RandomSource.create(waveSeed(floor, waveNumber)));
            lineups.put(key, existing);
        }
        repair(available, fallback);
        return lineups.get(key);
    }
    private AbyssConfig.Pools usablePools(java.util.function.Predicate<ResourceLocation> available, AbyssConfig.Pools fallback) {
        var valid = pools.available(available);
        Map<MobCategory, List<ResourceLocation>> entries = new EnumMap<>(MobCategory.class);
        for (MobCategory category : MobCategory.values()) {
            var choices = valid.entries().getOrDefault(category, List.of());
            if (choices.isEmpty()) choices = fallback.entries().getOrDefault(category, List.of()).stream().filter(available).toList();
            entries.put(category, choices);
        }
        return new AbyssConfig.Pools(pools.mixMobs(), entries);
    }
    /** Repairs only missing category entries, retaining counts and every still-available selection. */
    public boolean repair(java.util.function.Predicate<ResourceLocation> available, AbyssConfig.Pools fallback) {
        var usable = usablePools(available, fallback);
        boolean changed = false;
        for (var entry : lineups.entrySet()) {
            List<FloorDefinition.Enemy> replacements = new ArrayList<>(entry.getValue());
            for (int index = 0; index < replacements.size(); index++) {
                var enemy = replacements.get(index);
                if (available.test(enemy.id()) || enemy.category() == null) continue;
                var choices = usable.entries().getOrDefault(enemy.category(), List.of());
                // Keep unresolved slots so a later boot/config repair can recover them without losing counts.
                if (choices.isEmpty()) continue;
                var random = RandomSource.create(seed ^ entry.getKey().hashCode() ^ enemy.id().hashCode() ^ index);
                replacements.set(index, new FloorDefinition.Enemy(choices.get(random.nextInt(choices.size())), enemy.count(), enemy.category()));
                changed = true;
            }
            entry.setValue(List.copyOf(replacements));
        }
        return changed;
    }
    public long waveSeed(int floor, int wave) {
        long mixed = seed ^ (0x9E3779B97F4A7C15L * floor) ^ (0xC2B2AE3D27D4EB4FL * wave);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (!initialized()) return tag;
        tag.putLong("StartedAt", startedAt); tag.putLong("Seed", seed); tag.putBoolean("MixMobs", pools.mixMobs());
        CompoundTag entries = new CompoundTag();
        pools.entries().forEach((category, ids) -> {
            ListTag list = new ListTag(); ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
            entries.put(category.key(), list);
        });
        tag.put("Pools", entries);
        ListTag rolls = new ListTag();
        lineups.forEach((key, enemies) -> {
            CompoundTag roll = new CompoundTag(); roll.putString("Key", key);
            ListTag mobs = new ListTag();
            enemies.forEach(enemy -> {
                CompoundTag mob = new CompoundTag(); mob.putString("Id", enemy.id().toString()); mob.putInt("Count", enemy.count());
                if (enemy.category() != null) mob.putString("Category", enemy.category().name());
                mobs.add(mob);
            });
            roll.put("Mobs", mobs); rolls.add(roll);
        });
        tag.put("Lineups", rolls); return tag;
    }
    public static AbyssCycle load(CompoundTag tag) {
        AbyssCycle cycle = new AbyssCycle();
        if (!tag.contains("Pools", Tag.TAG_COMPOUND)) return cycle;
        Map<MobCategory, List<ResourceLocation>> entries = new EnumMap<>(MobCategory.class);
        CompoundTag saved = tag.getCompound("Pools");
        for (MobCategory category : MobCategory.values()) {
            if (!saved.contains(category.key())) continue;
            List<ResourceLocation> ids = new ArrayList<>();
            for (Tag value : saved.getList(category.key(), Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id != null) ids.add(id);
            }
            entries.put(category, ids);
        }
        cycle.start(tag.getLong("StartedAt"), tag.getLong("Seed"), new AbyssConfig.Pools(tag.getBoolean("MixMobs"), entries));
        for (Tag value : tag.getList("Lineups", Tag.TAG_COMPOUND)) {
            CompoundTag roll = (CompoundTag) value;
            List<FloorDefinition.Enemy> enemies = new ArrayList<>();
            for (Tag mobValue : roll.getList("Mobs", Tag.TAG_COMPOUND)) {
                CompoundTag mob = (CompoundTag) mobValue;
                ResourceLocation id = ResourceLocation.tryParse(mob.getString("Id"));
                if (id == null || mob.getInt("Count") <= 0) continue;
                MobCategory category = mob.contains("Category") ? MobCategory.valueOf(mob.getString("Category")) : null;
                enemies.add(new FloorDefinition.Enemy(id, mob.getInt("Count"), category));
            }
            cycle.lineups.put(roll.getString("Key"), List.copyOf(enemies));
        }
        return cycle;
    }
}
