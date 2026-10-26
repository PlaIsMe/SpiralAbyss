package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import java.util.*;
import java.util.function.*;

/** Server-thread stage cursor and the sole collection of current-stage entities. */
public final class WaveStages<T> {
    private List<List<FloorDefinition.Enemy>> sequence = List.of();
    private final List<T> enemies = new ArrayList<>();
    private int index = -1;
    private int removed;
    private int additional;
    private boolean spawning;

    public void prepare(List<FloorDefinition.Enemy> lineup) {
        if (!enemies.isEmpty() || spawning) throw new IllegalStateException("Clear the previous encounter before preparing stages");
        SortedMap<Integer, List<FloorDefinition.Enemy>> batches = new TreeMap<>();
        lineup.forEach(enemy -> batches.computeIfAbsent(enemy.stage(), unused -> new ArrayList<>()).add(enemy));
        if (batches.isEmpty()) throw new IllegalArgumentException("Empty encounter lineup");
        sequence = batches.values().stream().map(List::copyOf).toList();
        index = -1;
        removed = 0;
        additional = 0;
    }
    public boolean advance(Function<List<FloorDefinition.Enemy>, List<T>> spawn) {
        if (spawning || !enemies.isEmpty() || index + 1 >= sequence.size()) return false;
        spawning = true;
        index++;
        try {
            List<T> created = spawn.apply(sequence.get(index));
            int expected = sequence.get(index).stream().mapToInt(FloorDefinition.Enemy::count).sum();
            enemies.addAll(created);
            if (created.size() != expected) throw new IllegalStateException("Stage spawned " + created.size() + " of " + expected + " enemies");
            return true;
        } finally { spawning = false; }
    }
    public void removeInvalid(Predicate<T> invalid) {
        int before = enemies.size();
        enemies.removeIf(invalid);
        removed += before - enemies.size();
    }
    public boolean add(T enemy) {
        if (index < 0 || enemies.contains(enemy)) return false;
        enemies.add(enemy);
        additional++;
        return true;
    }
    public List<T> enemies() { return List.copyOf(enemies); }
    public int index() { return index; }
    public int size() { return sequence.size(); }
    public int removed() { return removed; }
    public int total() { return additional + sequence.stream().flatMap(List::stream).mapToInt(FloorDefinition.Enemy::count).sum(); }
    public boolean complete() { return !spawning && index >= 0 && index + 1 == sequence.size() && enemies.isEmpty(); }
    public void clear(Consumer<T> discard) {
        var previous = List.copyOf(enemies);
        enemies.clear();
        sequence = List.of();
        index = -1;
        removed = 0;
        additional = 0;
        spawning = false;
        previous.forEach(discard);
    }
}
