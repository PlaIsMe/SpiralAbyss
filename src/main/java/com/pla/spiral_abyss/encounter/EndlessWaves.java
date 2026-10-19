package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Endless mode reuses the loaded datapack archetypes without normal-floor boss reservations. */
public final class EndlessWaves {
    public static final int FLOOR = 7;
    private static final Set<String> NINTH = Set.of("boss_minions", "boss_vanguard", "solo_boss");
    private static final Set<String> TENTH = Set.of("dual_boss", "final_wave_boss", "final_escalation");
    private EndlessWaves() {}

    public static int position(int wave) { return (Math.max(1, wave) - 1) % 10 + 1; }
    public static int block(int wave) { return (Math.max(1, wave) - 1) / 10; }
    public static double bossHealth(int wave) { return 1000.0 + 500.0 * block(wave); }
    // Keep finite values even for operator-edited profiles far beyond playable progression.
    public static double mobScale(int wave) { return Math.pow(1.2, Math.min(100, block(wave))); }
    public static boolean eligible(FloorDefinition.WaveType type, int wave) {
        int position = position(wave);
        String name = type.id().getPath();
        if (position == 9) return NINTH.contains(name);
        if (position == 10) return TENTH.contains(name);
        if (NINTH.contains(name) || TENTH.contains(name)) return false;
        boolean boss = type.groups().stream().anyMatch(group -> group.category() == MobCategory.BOSS);
        boolean mini = type.groups().stream().anyMatch(group -> group.category() == MobCategory.MINI_BOSS);
        boolean champion = type.groups().stream().anyMatch(group -> group.category() == MobCategory.CHAMPION_MOB);
        if (boss) return position == 8;
        if (mini) return position >= 7;
        if (champion) return position >= 4;
        return true;
    }
    public static int weight(FloorDefinition.WaveType type, int wave) {
        int target = position(wave) <= 3 ? 1 + position(wave) / 2 : position(wave) <= 6 ? 3 : 5;
        return Math.max(1, 8 - Math.abs(type.tier() - target) * 3);
    }
    public static WaveComposition.Selection select(int wave, long seed,
            Collection<FloorDefinition.WaveType> templates, MobCatalog.Pools pools,
            MobCatalog.Equipment equipment) {
        var choices = templates.stream().filter(type -> eligible(type, wave))
                .filter(type -> type.groups().stream().allMatch(group -> !pools.entries().getOrDefault(group.category(), List.of()).isEmpty()))
                .sorted(Comparator.comparing(type -> type.id().toString())).toList();
        if (choices.isEmpty()) throw new IllegalArgumentException("No available endless archetype for wave " + wave);
        RandomSource random = RandomSource.create(seed);
        int pick = random.nextInt(choices.stream().mapToInt(type -> weight(type, wave)).sum());
        FloorDefinition.WaveType selected = choices.get(choices.size() - 1);
        for (var type : choices) { pick -= weight(type, wave); if (pick < 0) { selected = type; break; } }
        var composition = WaveComposition.select(new FloorDefinition.Wave(Map.of(), List.of(), List.of(selected)), pools, equipment, random, Set.of());
        return new WaveComposition.Selection(composition.type(), BossWavePolicy.sequential(composition.enemies()));
    }
    public static int experience(int wave, RandomSource random) {
        long milestone = Math.max(1, wave / 10);
        long base = 100 * milestone * milestone;
        return (int) Math.min(1_000_000, base + random.nextInt((int) Math.min(100_000, 50 * milestone) + 1));
    }
}
