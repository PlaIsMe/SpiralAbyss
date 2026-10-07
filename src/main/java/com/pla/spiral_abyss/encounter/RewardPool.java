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
    /** Weighted selection without revisiting a failed table ID during the same chest roll. */
    public RewardEntry table(RandomSource random, Set<net.minecraft.resources.ResourceLocation> attempted) {
        int remainingWeight = tables.stream().filter(entry -> !attempted.contains(entry.id())).mapToInt(RewardEntry::weight).sum();
        if (remainingWeight == 0) return null;
        int choice = random.nextInt(remainingWeight);
        for (RewardEntry entry : tables) {
            if (attempted.contains(entry.id())) continue;
            choice -= entry.weight();
            if (choice < 0) return entry;
        }
        throw new IllegalStateException("Invalid remaining reward weights");
    }
    public RewardEntry bonus(RewardEntry requested, RandomSource random) {
        if (items.contains(requested)) return requested;
        return items.isEmpty() ? null : items.get(random.nextInt(items.size()));
    }
    /** Keep every participant's bonus chance, but select only one of the successful rolls. */
    public RewardEntry singleBonus(List<RewardEntry> requested, RandomSource random, int eligiblePlayers) {
        RewardEntry selected = null;
        int successes = 0;
        for (int player = 0; player < eligiblePlayers; player++) {
            for (RewardEntry request : requested) {
                RewardEntry entry = bonus(request, random);
                if (entry == null || random.nextDouble() >= entry.chance()) continue;
                // Reservoir selection avoids favoring the first boss or the first participant.
                if (random.nextInt(++successes) == 0) selected = entry;
            }
        }
        return selected;
    }
}
