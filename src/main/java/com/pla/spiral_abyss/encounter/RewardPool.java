package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition.RewardEntry;
import net.minecraft.util.RandomSource;
import java.util.*;
import java.util.function.Predicate;

/** Optional mods may disappear between datapack authoring and use. Resolve a finite pool first. */
public final class RewardPool {
    private final List<RewardEntry> tables;
    private final List<RewardEntry> items;
    private final int tableWeight;
    public RewardPool(List<RewardEntry> entries, Predicate<RewardEntry> available) {
        tables = entries.stream().filter(e -> !e.item()).filter(available).toList();
        items = entries.stream().filter(RewardEntry::item).filter(available).toList();
        tableWeight = tables.stream().mapToInt(RewardEntry::weight).sum();
    }
    public RewardEntry table(RandomSource random) {
        if (tableWeight == 0) return null;
        int choice = random.nextInt(tableWeight);
        for (RewardEntry entry : tables) {
            choice -= entry.weight();
            if (choice < 0) return entry;
        }
        throw new IllegalStateException("Invalid reward weights");
    }
    public RewardEntry bonus(RewardEntry requested, RandomSource random) {
        if (items.contains(requested)) return requested;
        return items.isEmpty() ? null : items.get(random.nextInt(items.size()));
    }
}
