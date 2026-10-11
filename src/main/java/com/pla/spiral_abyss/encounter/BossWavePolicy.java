package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.*;
import java.util.*;

/** Server-side late-floor contract, independent of previews and candidate names. */
public final class BossWavePolicy {
    private BossWavePolicy() {}
    public static void validate(int floor, int wave, FloorDefinition.Wave definition) {
        List<Integer> counts = definition.waveTypes().isEmpty()
                ? List.of(definition.categories().getOrDefault(MobCategory.BOSS, 0) + definition.bosses().stream().mapToInt(FloorDefinition.Enemy::count).sum())
                : definition.waveTypes().stream().map(type -> type.groups().stream().filter(group -> group.category() == MobCategory.BOSS).mapToInt(FloorDefinition.WaveGroup::count).sum()).toList();
        boolean forbidden = floor < 5 || floor == 5 && wave < 3 || floor == 6 && wave == 1;
        boolean single = floor == 5 && wave == 3 || floor == 6 && wave == 2;
        if (counts.stream().anyMatch(count -> forbidden ? count != 0 : single && count != 1)) throw new IllegalArgumentException("Floor " + floor + " wave " + wave + (forbidden ? " forbids boss groups" : " requires exactly one boss"));
        if (floor == 6 && wave == 3 && counts.stream().anyMatch(count -> count < 1 || count > 2)) throw new IllegalArgumentException("Floor 6 wave 3 requires one or two bosses");
        if (!forbidden && (!definition.bosses().isEmpty() || definition.waveTypes().stream().flatMap(type -> type.groups().stream()).anyMatch(group -> group.category() == MobCategory.BOSS && group.role() != CombatRole.ANY))) throw new IllegalArgumentException("Cycle boss slots require category boss with role any, not explicit fixed IDs");
    }
    public static List<FloorDefinition.Enemy> sequential(List<FloorDefinition.Enemy> enemies) {
        SortedMap<Integer, List<FloorDefinition.Enemy>> stages = new TreeMap<>();
        enemies.forEach(enemy -> stages.computeIfAbsent(enemy.stage(), unused -> new ArrayList<>()).add(enemy));
        List<FloorDefinition.Enemy> result = new ArrayList<>();
        int stage = 0;
        for (var group : stages.values()) {
            boolean boss = false;
            for (var enemy : group) if (enemy.category() != MobCategory.BOSS && enemy.category() != null) result.add(new FloorDefinition.Enemy(enemy.id(), enemy.count(), enemy.category(), stage, enemy.role()));
            for (var enemy : group) if (enemy.category() == MobCategory.BOSS || enemy.category() == null) for (int count = 0; count < enemy.count(); count++) {
                if (boss) stage++;
                result.add(new FloorDefinition.Enemy(enemy.id(), 1, enemy.category(), stage, enemy.role()));
                boss = true;
            }
            stage++;
        }
        return List.copyOf(result);
    }
}
