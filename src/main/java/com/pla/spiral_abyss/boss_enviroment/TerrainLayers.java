package com.pla.spiral_abyss.boss_enviroment;

import java.util.*;
import java.util.function.*;

/** Reversible per-owner terrain; removing one boss reveals surviving layers, then the original. */
public final class TerrainLayers<K, V> {
    private record Layer<V>(V value, int priority) {}
    private final class Cell {
        final V original;
        final Map<UUID, Layer<V>> owners = new LinkedHashMap<>();
        Cell(V original) { this.original = original; }
        V visible() {
            V value = original;
            int priority = Integer.MIN_VALUE;
            for (var layer : owners.values()) if (layer.priority() >= priority) { value = layer.value(); priority = layer.priority(); }
            return value;
        }
    }
    private final Map<K, Cell> cells = new LinkedHashMap<>();
    private final Function<K, V> read;
    private final BiConsumer<K, V> write;
    public TerrainLayers(Function<K, V> read, BiConsumer<K, V> write) { this.read = read; this.write = write; }
    public boolean contains(K position) { return cells.containsKey(position); }
    public int size() { return cells.size(); }
    public void put(UUID owner, K position, V value, int priority) {
        Cell cell = cells.computeIfAbsent(position, key -> new Cell(read.apply(key)));
        cell.owners.put(owner, new Layer<>(value, priority));
        write.accept(position, cell.visible());
    }
    public void remove(UUID owner) {
        for (var iterator = cells.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            Cell cell = entry.getValue();
            if (cell.owners.remove(owner) == null) continue;
            V visible = cell.visible();
            if (cell.owners.isEmpty()) iterator.remove();
            write.accept(entry.getKey(), visible);
        }
    }
    public void remove(UUID owner, K position) {
        Cell cell = cells.get(position);
        if (cell == null || cell.owners.remove(owner) == null) return;
        V visible = cell.visible();
        if (cell.owners.isEmpty()) cells.remove(position);
        write.accept(position, visible);
    }
    public void clear() {
        Map<K, V> originals = new LinkedHashMap<>();
        cells.forEach((key, cell) -> originals.put(key, cell.original));
        cells.clear();
        originals.forEach(write);
    }
}
