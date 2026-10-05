package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.WaveComposition;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** World-wide wall clock and mob pool snapshot, saved atomically with player progress. */
public final class AbyssCycle {
    public static final long DAY_MILLIS = 86_400_000L;
    private long startedAt;
    private long seed;
    private MobCatalog.Pools pools;
    private final Map<String, List<FloorDefinition.Enemy>> lineups = new TreeMap<>();
    private final Map<Integer, List<Challenge>> challenges = new TreeMap<>();
    public boolean initialized() { return pools != null; }
    public MobCatalog.Pools pools() { return pools; }
    public long deadline(int days) { return days <= 0 ? -1 : startedAt + days * DAY_MILLIS; }
    public long remainingMillis(long now, int days) { return days <= 0 ? -1 : Math.max(0, deadline(days) - now); }
    public boolean due(long now, int days) { return initialized() && days > 0 && now >= deadline(days); }
    public long nextStart(long now, int days) {
        long interval = days * DAY_MILLIS;
        return startedAt + Math.max(1, (now - startedAt) / interval) * interval;
    }
    public void start(long now, long newSeed, MobCatalog.Pools snapshot) {
        startedAt = now; seed = newSeed; pools = snapshot; lineups.clear(); challenges.clear();
    }
    public List<Challenge> challenges(int floor) {
        return challenges(floor, challenge -> true);
    }
    public List<Challenge> challenges(int floor, java.util.function.Predicate<Challenge> eligible) {
        Map<Integer,com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context> contexts = new TreeMap<>();
        for (var definition : FloorCatalog.snapshot().floors().values()) {
            var populations = definition.waves().stream().map(wave -> wave.categories().values().stream().mapToInt(Integer::intValue).sum()
                    + wave.bosses().stream().mapToInt(FloorDefinition.Enemy::count).sum()).toList();
            contexts.put(definition.number(),new com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context(populations,
                    definition.number()==floor?eligible:challenge->true));
        }
        return challenges(floor,contexts);
    }
    /** Injectable snapshot/count also supports focused persistence and optional-mod checks. */
    public List<Challenge> challenges(int floor, int minimumEnemies, ChallengeCatalog.Snapshot catalog) {
        return challenges(floor, List.of(minimumEnemies), catalog);
    }
    public List<Challenge> challenges(int floor, List<Integer> populations, ChallengeCatalog.Snapshot catalog) {
        return challenges(floor, populations, catalog, challenge -> true);
    }
    public List<Challenge> challenges(int floor, List<Integer> populations, ChallengeCatalog.Snapshot catalog,
                                      java.util.function.Predicate<Challenge> eligible) {
        Map<Integer,com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context> contexts = new TreeMap<>();
        for(int number=1;number<=6;number++) contexts.put(number,new com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context(populations,eligible));
        return challenges(floor,contexts,catalog);
    }
    public List<Challenge> challenges(int floor, Map<Integer,com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context> contexts) {
        return challenges(floor,contexts,ChallengeCatalog.snapshot());
    }
    public List<Challenge> challenges(int floor, Map<Integer,com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context> contexts, ChallengeCatalog.Snapshot catalog) {
        if (floor < 1 || floor > 6) throw new IllegalArgumentException("Invalid challenge floor");
        if(floor<=2) {
            var selected=com.pla.spiral_abyss.encounter.challenge.ChallengePlan.timeOnly(floor,seed,catalog,challenges);
            challenges.put(floor,selected);return selected;
        }
        var plan = com.pla.spiral_abyss.encounter.challenge.ChallengePlan.select(seed ^ 0x4348414C4C454E47L,catalog,challenges,contexts);
        if(plan!=challenges){challenges.clear(); challenges.putAll(plan);}
        return challenges.get(floor);
    }
    public List<FloorDefinition.Enemy> lineup(int floor, int waveNumber, FloorDefinition.Wave wave,
                                            java.util.function.Predicate<ResourceLocation> available,
                                            MobCatalog.Pools fallback) {
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
    private MobCatalog.Pools usablePools(java.util.function.Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
        var valid = pools.available(available);
        Map<MobCategory, List<ResourceLocation>> entries = new EnumMap<>(MobCategory.class);
        for (MobCategory category : MobCategory.values()) {
            var choices = valid.entries().getOrDefault(category, List.of());
            if (choices.isEmpty()) choices = fallback.entries().getOrDefault(category, List.of()).stream().filter(available).toList();
            entries.put(category, choices);
        }
        return new MobCatalog.Pools(pools.mixMobs(), entries);
    }
    /** Repairs only missing category entries, retaining counts and every still-available selection. */
    public boolean repair(java.util.function.Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
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
        tag.put("Lineups", rolls);
        ListTag challengeFloors = new ListTag();
        challenges.forEach((floor, selected) -> {
            CompoundTag entry = new CompoundTag(); entry.putInt("Floor", floor);
            ListTag list = new ListTag(); selected.forEach(challenge -> list.add(challenge.save())); entry.put("Challenges", list); challengeFloors.add(entry);
        });
        tag.put("ChallengeFloors", challengeFloors);
        tag.putInt("ChallengePolicy",com.pla.spiral_abyss.encounter.challenge.ChallengePlan.VERSION); return tag;
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
        cycle.start(tag.getLong("StartedAt"), tag.getLong("Seed"), new MobCatalog.Pools(tag.getBoolean("MixMobs"), entries));
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
        if(tag.getInt("ChallengePolicy")==com.pla.spiral_abyss.encounter.challenge.ChallengePlan.VERSION)
        for (Tag value : tag.getList("ChallengeFloors", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            int floor = entry.getInt("Floor");
            if (floor < 1 || floor > 6) continue;
            List<Challenge> selected = new ArrayList<>();
            for (Tag challenge : entry.getList("Challenges", Tag.TAG_COMPOUND)) selected.add(Challenge.load((CompoundTag) challenge));
            if (selected.size() == 3) cycle.challenges.put(floor, List.copyOf(selected));
        }
        return cycle;
    }
}
