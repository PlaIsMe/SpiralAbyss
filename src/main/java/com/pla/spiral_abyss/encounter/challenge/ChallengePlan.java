package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.encounter.challenge.Challenge.*;
import java.util.*;
import java.util.function.Predicate;

/** Selects one feasible objective per datapack tier, independently for each floor. */
public final class ChallengePlan {
    public static final int VERSION = 6;

    public record Context(List<Integer> populations, Predicate<Challenge> eligible) {
        public Context {
            populations = List.copyOf(populations);
            if (populations.isEmpty() || populations.stream().anyMatch(count -> count < 1))
                throw new IllegalArgumentException("Missing wave populations");
        }
        boolean accepts(List<Challenge> selected) {
            return selected.stream().allMatch(eligible)
                    && populations.stream().allMatch(count -> ChallengeCatalog.feasible(selected, count));
        }
    }

    private ChallengePlan() {}

    public static List<Challenge> select(long seed, ChallengeCatalog.Snapshot catalog,
            Map<Difficulty, List<Type>> allowed, List<Challenge> saved, Context context) {
        Map<Difficulty, Challenge> retained = new EnumMap<>(Difficulty.class);
        for (Challenge old : saved) {
            if (allowed.getOrDefault(old.difficulty(), List.of()).contains(old.type())
                    && catalog.tier(old.type(), old.difficulty()) != null
                    && ChallengeCatalog.available(old) && context.accepts(List.of(old)))
                retained.put(old.difficulty(), old);
        }
        if (retained.size() == Difficulty.values().length) {
            var ordered = Arrays.stream(Difficulty.values()).map(retained::get).toList();
            if (context.accepts(ordered)) return ordered;
        }
        List<List<Challenge>> options = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            List<Type> types = new ArrayList<>(allowed.getOrDefault(difficulty, List.of()));
            Collections.shuffle(types, new Random(seed ^ difficulty.ordinal()));
            Challenge old = saved.stream().filter(c -> c.difficulty() == difficulty && types.contains(c.type()))
                    .findFirst().orElse(null);
            if (old != null) { types.remove(old.type()); types.add(0, old.type()); }
            List<Challenge> choices = new ArrayList<>();
            for (Type type : types) {
                if (catalog.tier(type, difficulty) == null) continue;
                for (Challenge candidate : catalog.candidates(type, difficulty,
                        seed ^ ((long) type.ordinal() << 32) ^ difficulty.ordinal(), old))
                    if (context.accepts(List.of(candidate))) choices.add(candidate);
            }
            if (choices.isEmpty()) throw new IllegalArgumentException("No available challenge." + difficulty.key() + " choice for this floor");
            options.add(choices);
        }
        List<Challenge> result = new ArrayList<>();
        // First repair only invalid slots; relax retained slots only if the combination cannot work.
        if (!search(result, options, retained, context, new int[]{100000})
                && !search(result, options, Map.of(), context, new int[]{100000}))
            throw new IllegalArgumentException("No feasible easy, medium and hard challenge set for this floor");
        return List.copyOf(result);
    }

    private static boolean search(List<Challenge> result, List<List<Challenge>> options,
            Map<Difficulty, Challenge> retained, Context context, int[] budget) {
        if (result.size() == Difficulty.values().length) return true;
        Challenge fixed = retained.get(Difficulty.values()[result.size()]);
        for (Challenge candidate : fixed == null ? options.get(result.size()) : List.of(fixed)) {
            if (--budget[0] < 0) return false;
            result.add(candidate);
            if (context.accepts(result) && search(result, options, retained, context, budget)) return true;
            result.remove(result.size() - 1);
        }
        return false;
    }
}
