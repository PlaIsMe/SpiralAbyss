package com.pla.spiral_abyss.encounter.data;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Validated, immutable datapack definitions. Durations are converted to ticks when loaded. */
public record FloorDefinition(int number, int waveDelayTicks, Rewards rewards, List<Wave> waves) {
    public FloorDefinition {
        waves = List.copyOf(waves);
    }

    public record Wave(int timeTicks, double healthScale, double damageScale, List<Enemy> bosses) {
        public Wave { bosses = List.copyOf(bosses); }
    }

    public record Enemy(ResourceLocation id, int count) {}

    public record RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount,
                              int weight, double chance) {}

    public record Rewards(List<RewardEntry> entries, int rolls, int experience) {
        public Rewards { entries = List.copyOf(entries); }
    }
}
