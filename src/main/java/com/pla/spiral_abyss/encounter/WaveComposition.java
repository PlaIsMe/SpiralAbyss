package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.*;
import com.mojang.logging.LogUtils;

/** Resolves counts to entity IDs before spawning anything. */
public final class WaveComposition {
    private WaveComposition() {}
    public record Selection(ResourceLocation type, List<FloorDefinition.Enemy> enemies) {
        public Selection { enemies = List.copyOf(enemies); }
    }
    public static List<FloorDefinition.Enemy> resolve(FloorDefinition.Wave wave, MobCatalog.Pools config, RandomSource random) {
        return select(wave, config, random).enemies();
    }
    public static Selection select(FloorDefinition.Wave wave, MobCatalog.Pools config, RandomSource random) {
        return select(wave, config, MobCatalog.equipment(), random, Set.of());
    }
    public static Selection select(FloorDefinition.Wave wave, MobCatalog.Pools config, MobCatalog.Equipment equipment, RandomSource random, Set<ResourceLocation> usedTypes) {
        if (!wave.waveTypes().isEmpty()) {
            var usable = wave.waveTypes().stream().filter(type -> usable(type, config)).toList();
            if (usable.isEmpty()) throw new IllegalArgumentException("No usable wave type: empty category pool in " + wave.waveTypes().stream().map(FloorDefinition.WaveType::id).toList());
            var candidates = usable.stream().filter(type -> !usedTypes.contains(type.id())).toList();
            if (candidates.isEmpty()) {
                LogUtils.getLogger().warn("All usable configured wave types already used this floor: {}; reusing a configured difficulty-compatible candidate", usedTypes);
                candidates = usable;
            }
            var selected = candidates.get(random.nextInt(candidates.size()));
            LogUtils.getLogger().debug("Wave candidates {}, excluded {}, selected {}", wave.waveTypes().stream().map(FloorDefinition.WaveType::id).toList(), usedTypes, selected.id());
            List<FloorDefinition.Enemy> groups = new ArrayList<>();
            for (int stage = 0; stage < selected.sequence().size(); stage++) {
                Map<MobCategory, Set<ResourceLocation>> chosen = new EnumMap<>(MobCategory.class);
                for (var group : selected.sequence().get(stage).groups()) {
                    var used = chosen.computeIfAbsent(group.category(), unused -> new HashSet<>());
                    var pool = candidates(config.entries().getOrDefault(group.category(), List.of()), group.role(), equipment, selected.id() + " stage " + (stage + 1) + " category " + group.category().key());
                    var fresh = pool.stream().filter(id -> !used.contains(id)).toList();
                    // Role correctness takes priority over variety; singleton boss pools remain usable.
                    if (!fresh.isEmpty()) pool = fresh;
                    ResourceLocation id = pool.get(random.nextInt(pool.size()));
                    used.add(id);
                    groups.add(new FloorDefinition.Enemy(id, group.count(), group.category(), stage, group.role()));
                }
            }
            return new Selection(selected.id(), groups);
        }
        List<FloorDefinition.Enemy> result = new ArrayList<>(wave.bosses());
        for (MobCategory category : MobCategory.values()) {
            int count = wave.categories().getOrDefault(category, 0);
            if (count == 0) continue;
            var pool = config.entries().getOrDefault(category, List.of());
            if (pool.isEmpty()) throw new IllegalArgumentException("No usable mob IDs configured for " + category.key());
            if (!config.mixMobs()) {
                result.add(new FloorDefinition.Enemy(pool.get(random.nextInt(pool.size())), count, category));
            } else {
                Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
                for (int i = 0; i < count; i++) counts.merge(pool.get(random.nextInt(pool.size())), 1, Integer::sum);
                counts.forEach((id, amount) -> result.add(new FloorDefinition.Enemy(id, amount, category)));
            }
        }
        return new Selection(null, result);
    }
    public static List<ResourceLocation> candidates(List<ResourceLocation> pool, CombatRole role, MobCatalog.Equipment equipment, String context) {
        var distinct = pool.stream().distinct().toList();
        if (role == CombatRole.ANY) return distinct;
        var matching = distinct.stream().filter(id -> equipment.role(id) == role).toList();
        if (!matching.isEmpty() || distinct.isEmpty()) return matching;
        LogUtils.getLogger().warn("No {} candidates found for {}; falling back to unfiltered category pool", role.key(), context);
        return distinct;
    }
    private static boolean usable(FloorDefinition.WaveType type, MobCatalog.Pools config) {
        return type.groups().stream().allMatch(group -> !config.entries().getOrDefault(group.category(), List.of()).isEmpty());
    }
}
