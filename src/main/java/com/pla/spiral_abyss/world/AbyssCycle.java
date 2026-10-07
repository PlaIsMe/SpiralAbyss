package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.WaveComposition;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.challenge.ChallengePlan;
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
    private ResourceLocation floorFiveBoss, floorSixBoss;
    private final Map<String, List<FloorDefinition.Enemy>> lineups = new TreeMap<>();
    private final Map<String, ResourceLocation> selectedTypes = new TreeMap<>();
    private final Map<String, String> activeLineups = new TreeMap<>();
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
        startedAt = now; seed = newSeed; pools = snapshot; lineups.clear(); selectedTypes.clear(); activeLineups.clear(); challenges.clear();
        floorFiveBoss = null; floorSixBoss = null;
    }
    public List<Challenge> challenges(FloorDefinition floor, ChallengePlan.Context context, ChallengeCatalog.Snapshot catalog) {
        var selected = ChallengePlan.select(seed ^ 0x4348414C4C454E47L ^ floor.number(), catalog,
                floor.challenge(), challenges.getOrDefault(floor.number(), List.of()), context);
        challenges.put(floor.number(), selected);
        return selected;
    }
    public List<FloorDefinition.Enemy> lineup(int floor, int waveNumber, FloorDefinition.Wave wave,
                                            java.util.function.Predicate<ResourceLocation> available,
                                            MobCatalog.Pools fallback) {
        com.pla.spiral_abyss.encounter.BossWavePolicy.validate(floor, waveNumber, wave);
        selectBossPair(available, fallback);
        // Intentional datapack composition edits get their own cache entry; attribute edits retain selections.
        String key = lineupKey(floor, waveNumber, wave);
        String previousKey = activeLineups.put(floor + ":" + waveNumber, key);
        // The dual-boss schema now uses explicit stages; retain compatible saved species.
        if (floor == 6 && waveNumber == 3 && previousKey != null && !lineups.containsKey(key)) {
            var prior = lineups.get(previousKey);
            var selected = selectedTypes.get(previousKey);
            var definition = wave.waveTypes().stream().filter(type -> type.id().equals(selected)).findFirst().orElse(null);
            if (prior != null && definition != null && definition.groups().size() == prior.size()) {
                boolean compatible = true;
                for (int index = 0; index < prior.size(); index++) {
                    var group = definition.groups().get(index); var enemy = prior.get(index);
                    compatible &= group.category() == enemy.category() && group.count() == enemy.count() && group.role() == enemy.role();
                }
                if (compatible) { lineups.put(key, com.pla.spiral_abyss.encounter.BossWavePolicy.sequential(prior)); selectedTypes.put(key, selected); }
            }
        }
        Set<ResourceLocation> used = new HashSet<>();
        for (int other = 1; other <= 3; other++) {
            if (other == waveNumber) continue;
            String otherKey = activeLineups.get(floor + ":" + other);
            if (otherKey != null && selectedTypes.containsKey(otherKey)) used.add(selectedTypes.get(otherKey));
        }
        migrateLegacyTemplate(floor, waveNumber, wave, key);
        List<FloorDefinition.Enemy> existing = lineups.get(key);
        // Reactivating an edited definition must not duplicate an unchanged sibling wave.
        if (existing != null && !key.equals(previousKey) && used.contains(selectedTypes.get(key))) {
            com.mojang.logging.LogUtils.getLogger().warn("Reactivated floor {} wave {} template {} conflicts with another wave; selecting an unused configured candidate", floor, waveNumber, selectedTypes.get(key));
            lineups.remove(key);
            selectedTypes.remove(key);
            existing = null;
        }
        if (existing == null) {
            var usable = usablePools(available, fallback);
            var selected = WaveComposition.select(wave, usable, MobCatalog.equipment(), RandomSource.create(waveSeed(floor, waveNumber)), used);
            com.mojang.logging.LogUtils.getLogger().debug("Abyss floor {} wave {} selected {} (cycle-stable)", floor, waveNumber, selected.type());
            lineups.put(key, selected.enemies());
            if (selected.type() != null) selectedTypes.put(key, selected.type());
        }
        repair(available, fallback);
        if (floor == 5 && waveNumber == 3 || floor == 6 && waveNumber == 2) {
            selectBossPair(available, fallback);
            ResourceLocation boss = floor == 5 ? floorFiveBoss : floorSixBoss;
            if (floorFiveBoss == null || floorSixBoss == null) throw new IllegalArgumentException("Floors 5/6 require two distinct available boss species in mob_pools/boss.json");
            lineups.put(key, lineups.get(key).stream().map(enemy -> enemy.category() == MobCategory.BOSS || enemy.category() == null ? new FloorDefinition.Enemy(boss, enemy.count(), MobCategory.BOSS, enemy.stage(), enemy.role()) : enemy).toList());
        }
        lineups.put(key, com.pla.spiral_abyss.encounter.BossWavePolicy.sequential(lineups.get(key)));
        return lineups.get(key);
    }
    private ResourceLocation savedBoss(String slot, java.util.function.Predicate<ResourceLocation> available) {
        String key = activeLineups.get(slot);
        return key == null ? null : lineups.getOrDefault(key, List.of()).stream().filter(enemy -> enemy.category() == MobCategory.BOSS && available.test(enemy.id())).map(FloorDefinition.Enemy::id).findFirst().orElse(null);
    }
    private void selectBossPair(java.util.function.Predicate<ResourceLocation> available, MobCatalog.Pools fallback) {
        List<ResourceLocation> choices = new ArrayList<>(usablePools(available, fallback).entries().getOrDefault(MobCategory.BOSS, List.of()));
        if (choices.size() < 2) for (var id : fallback.entries().getOrDefault(MobCategory.BOSS, List.of())) if (available.test(id) && !choices.contains(id)) choices.add(id);
        if (floorFiveBoss == null) floorFiveBoss = savedBoss("5:3", available);
        if (floorSixBoss == null) floorSixBoss = savedBoss("6:2", available);
        if (floorFiveBoss != null && !available.test(floorFiveBoss)) floorFiveBoss = null;
        if (floorSixBoss != null && (!available.test(floorSixBoss) || floorSixBoss.equals(floorFiveBoss))) floorSixBoss = null;
        if (floorFiveBoss == null) floorFiveBoss = chooseBoss(choices, floorSixBoss, waveSeed(5, 3));
        if (floorSixBoss == null) floorSixBoss = chooseBoss(choices, floorFiveBoss, waveSeed(6, 2));
    }
    private static ResourceLocation chooseBoss(List<ResourceLocation> choices, ResourceLocation excluded, long seed) {
        var allowed = choices.stream().filter(id -> !id.equals(excluded)).sorted().toList();
        return allowed.isEmpty() ? null : allowed.get(RandomSource.create(seed).nextInt(allowed.size()));
    }
    public ResourceLocation selectedType(int floor, int waveNumber, FloorDefinition.Wave wave) {
        return selectedTypes.get(lineupKey(floor, waveNumber, wave));
    }
    private void migrateLegacyTemplate(int floor, int waveNumber, FloorDefinition.Wave wave, String key) {
        if (lineups.containsKey(key) || wave.waveTypes().isEmpty()) return;
        // Unedited legacy top-level groups retain their exact species; only stage metadata changes.
        if (wave.waveTypes().stream().anyMatch(type -> type.tier() != 1 || type.sequence().stream().anyMatch(stage -> stage.groups().size() != 1 || stage.groups().get(0).role() != com.pla.spiral_abyss.encounter.data.CombatRole.ANY))) return;
        String types = wave.waveTypes().stream().map(type -> "WaveType[id=" + type.id() + ", groups=" + type.groups().stream().map(group -> "WaveGroup[category=" + group.category() + ", count=" + group.count() + "]").collect(java.util.stream.Collectors.joining(", ", "[", "]")) + "]").collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        String legacy = floor + ":" + waveNumber + ":[]:0:0:0:0:0:types:" + com.google.common.hash.Hashing.sha256().hashString(types, java.nio.charset.StandardCharsets.UTF_8);
        var previous = lineups.get(legacy);
        ResourceLocation selected = selectedTypes.get(legacy);
        if (previous == null || selected == null) return;
        var definition = wave.waveTypes().stream().filter(type -> type.id().equals(selected)).findFirst().orElse(null);
        if (definition == null || definition.groups().size() != previous.size()) return;
        List<FloorDefinition.Enemy> migrated = new ArrayList<>();
        for (int index = 0; index < previous.size(); index++) {
            var enemy = previous.get(index);
            var group = definition.groups().get(index);
            if (enemy.count() != group.count() || enemy.category() != group.category()) return;
            migrated.add(new FloorDefinition.Enemy(enemy.id(), enemy.count(), enemy.category(), index, enemy.role()));
        }
        lineups.put(key, List.copyOf(migrated));
        selectedTypes.put(key, selected);
    }
    private static String lineupKey(int floor, int waveNumber, FloorDefinition.Wave wave) {
        // Preserve legacy cache keys exactly, avoiding a migration reroll of existing fixed waves.
        String bosses = wave.bosses().stream().map(enemy -> "Enemy[id=" + enemy.id() + ", count=" + enemy.count() + ", category=" + enemy.category() + "]").collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        String key = floor + ":" + waveNumber + ":" + bosses;
        for (MobCategory category : MobCategory.values()) key += ":" + wave.categories().getOrDefault(category, 0);
        return wave.waveTypes().isEmpty() ? key : key + ":types:" + com.google.common.hash.Hashing.sha256()
                .hashString(wave.waveTypes().toString(), java.nio.charset.StandardCharsets.UTF_8);
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
        ResourceLocation previousFive = floorFiveBoss, previousSix = floorSixBoss;
        selectBossPair(available, fallback);
        changed = !Objects.equals(previousFive, floorFiveBoss) || !Objects.equals(previousSix, floorSixBoss);
        for (var entry : lineups.entrySet()) {
            List<FloorDefinition.Enemy> replacements = new ArrayList<>(entry.getValue());
            for (int index = 0; index < replacements.size(); index++) {
                var enemy = replacements.get(index);
                if (available.test(enemy.id()) || enemy.category() == null) continue;
                var choices = usable.entries().getOrDefault(enemy.category(), List.of());
                if (selectedTypes.containsKey(entry.getKey())) {
                    choices = WaveComposition.candidates(choices, enemy.role(), MobCatalog.equipment(), "saved " + selectedTypes.get(entry.getKey()) + " category " + enemy.category().key());
                    Set<ResourceLocation> occupied = new HashSet<>();
                    for (int other = 0; other < replacements.size(); other++) {
                        var group = replacements.get(other);
                        if (other != index && group.stage() == enemy.stage() && group.category() == enemy.category()) occupied.add(group.id());
                    }
                    var distinct = choices.stream().filter(id -> !occupied.contains(id)).toList();
                    if (!distinct.isEmpty()) choices = distinct;
                }
                // Keep unresolved slots so a later boot/config repair can recover them without losing counts.
                if (choices.isEmpty()) continue;
                var random = RandomSource.create(seed ^ entry.getKey().hashCode() ^ enemy.id().hashCode() ^ index);
                replacements.set(index, new FloorDefinition.Enemy(choices.get(random.nextInt(choices.size())), enemy.count(), enemy.category(), enemy.stage(), enemy.role()));
                changed = true;
            }
            entry.setValue(List.copyOf(replacements));
        }
        for (String slot : List.of("5:3", "6:2")) {
            String key = activeLineups.get(slot);
            ResourceLocation boss = slot.equals("5:3") ? floorFiveBoss : floorSixBoss;
            if (key == null || boss == null || !lineups.containsKey(key)) continue;
            var before = lineups.get(key);
            var after = before.stream().map(enemy -> enemy.category() == MobCategory.BOSS ? new FloorDefinition.Enemy(boss, enemy.count(), enemy.category(), enemy.stage(), enemy.role()) : enemy).toList();
            if (!before.equals(after)) { lineups.put(key, after); changed = true; }
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
        if (floorFiveBoss != null) tag.putString("FloorFiveBoss", floorFiveBoss.toString());
        if (floorSixBoss != null) tag.putString("FloorSixBoss", floorSixBoss.toString());
        CompoundTag entries = new CompoundTag();
        pools.entries().forEach((category, ids) -> {
            ListTag list = new ListTag(); ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
            entries.put(category.key(), list);
        });
        tag.put("Pools", entries);
        ListTag rolls = new ListTag();
        lineups.forEach((key, enemies) -> {
            CompoundTag roll = new CompoundTag(); roll.putString("Key", key);
            if (selectedTypes.containsKey(key)) roll.putString("SelectedType", selectedTypes.get(key).toString());
            ListTag mobs = new ListTag();
            enemies.forEach(enemy -> {
                CompoundTag mob = new CompoundTag(); mob.putString("Id", enemy.id().toString()); mob.putInt("Count", enemy.count());
                if (enemy.category() != null) mob.putString("Category", enemy.category().name());
                mob.putInt("Stage", enemy.stage()); mob.putString("Role", enemy.role().name());
                mobs.add(mob);
            });
            roll.put("Mobs", mobs); rolls.add(roll);
        });
        tag.put("Lineups", rolls);
        CompoundTag active = new CompoundTag();
        activeLineups.forEach(active::putString);
        tag.put("ActiveLineups", active);
        ListTag challengeFloors = new ListTag();
        challenges.forEach((floor, selected) -> {
            CompoundTag entry = new CompoundTag(); entry.putInt("Floor", floor);
            ListTag list = new ListTag(); selected.forEach(challenge -> list.add(challenge.save())); entry.put("Challenges", list); challengeFloors.add(entry);
        });
        tag.put("ChallengeFloors", challengeFloors);
        tag.putInt("ChallengePolicy",ChallengePlan.VERSION); return tag;
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
        cycle.floorFiveBoss = tag.contains("FloorFiveBoss", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("FloorFiveBoss")) : null;
        cycle.floorSixBoss = tag.contains("FloorSixBoss", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("FloorSixBoss")) : null;
        for (Tag value : tag.getList("Lineups", Tag.TAG_COMPOUND)) {
            CompoundTag roll = (CompoundTag) value;
            List<FloorDefinition.Enemy> enemies = new ArrayList<>();
            for (Tag mobValue : roll.getList("Mobs", Tag.TAG_COMPOUND)) {
                CompoundTag mob = (CompoundTag) mobValue;
                ResourceLocation id = ResourceLocation.tryParse(mob.getString("Id"));
                if (id == null || mob.getInt("Count") <= 0) continue;
                MobCategory category = mob.contains("Category") ? MobCategory.valueOf(mob.getString("Category")) : null;
                com.pla.spiral_abyss.encounter.data.CombatRole role = com.pla.spiral_abyss.encounter.data.CombatRole.ANY;
                try { if (mob.contains("Role")) role = com.pla.spiral_abyss.encounter.data.CombatRole.valueOf(mob.getString("Role")); }
                catch (IllegalArgumentException ignored) { }
                enemies.add(new FloorDefinition.Enemy(id, mob.getInt("Count"), category, Math.max(0, mob.getInt("Stage")), role));
            }
            cycle.lineups.put(roll.getString("Key"), List.copyOf(enemies));
            ResourceLocation type = roll.contains("SelectedType", Tag.TAG_STRING) ? ResourceLocation.tryParse(roll.getString("SelectedType")) : null;
            if (type != null) cycle.selectedTypes.put(roll.getString("Key"), type);
        }
        CompoundTag active = tag.getCompound("ActiveLineups");
        for (String key : active.getAllKeys()) cycle.activeLineups.put(key, active.getString(key));
        if(tag.getInt("ChallengePolicy")==ChallengePlan.VERSION)
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
