package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Resolves counts to entity IDs before spawning anything. */
public final class WaveComposition {
    private WaveComposition() {}
    public static List<FloorDefinition.Enemy> resolve(FloorDefinition.Wave wave, MobCatalog.Pools config, RandomSource random) {
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
        return List.copyOf(result);
    }
}
