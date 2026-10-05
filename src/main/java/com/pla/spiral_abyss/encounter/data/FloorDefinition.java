package com.pla.spiral_abyss.encounter.data;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;

/** Validated, immutable datapack definitions. Durations are converted to ticks when loaded. */
public record FloorDefinition(int number, int timeTicks, int waveDelayTicks, Rewards rewards, List<Wave> waves) {
    public FloorDefinition {
        waves = List.copyOf(waves);
    }

    public record Wave(double healthScale, double damageScale,
                       Map<MobCategory, Integer> categories, List<Enemy> bosses) {
        public Wave { categories = Map.copyOf(categories); bosses = List.copyOf(bosses); }
    }

    public record Enemy(ResourceLocation id, int count, MobCategory category) {
        public Enemy(ResourceLocation id, int count) { this(id, count, null); }
    }

    public record RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount,
                              int weight, double chance, boolean rare) {
        public RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount,
                           int weight, double chance) {
            this(id, item, minCount, maxCount, weight, chance, false);
        }
    }

    public record Rewards(List<RewardEntry> entries, int rolls, int experience) {
        public Rewards { entries = List.copyOf(entries); }
    }
}
