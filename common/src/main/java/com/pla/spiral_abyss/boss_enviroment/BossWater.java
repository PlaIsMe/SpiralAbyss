package com.pla.spiral_abyss.boss_enviroment;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.pla.spiral_abyss.event.BossDeathBlocks;

/** Compact ownership for at most 65*65*120 water cells; preserves all existing architecture. */
final class BossWater {
    private static final int WIDTH = 65, LAYER = WIDTH * WIDTH;
    private final ServerLevel level;
    private final BlockPos origin;
    private final int size;
    private final BitSet placed = new BitSet();
    private final Map<Integer, BlockState> unusualAir = new HashMap<>();
    private int cursor, cleanupCursor;
    BossWater(ServerLevel level, BlockPos center, int depth) {
        this.level = level; this.origin = center.offset(-32, 0, -32); this.size = LAYER * depth;
    }
    private BlockPos position(int index) { return origin.offset(index % WIDTH, index / LAYER, index % LAYER / WIDTH); }
    private int index(BlockPos pos) {
        int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
        return x < 0 || x >= WIDTH || z < 0 || z >= WIDTH || y < 0 || y >= size / LAYER ? -1 : y * LAYER + z * WIDTH + x;
    }
    boolean contains(BlockPos pos) { int index = index(pos); return index >= 0 && placed.get(index); }
    int count() { return placed.cardinality(); }
    void grow(int tick, int duration) {
        int target = (int) Math.ceil(size * Math.min(tick, duration) / (double) duration);
        while (cursor < target) {
            int index = cursor++;
            BlockPos pos = position(index);
            BlockState before = level.getBlockState(pos);
            if (!before.isAir() || level.getBlockEntity(pos) != null) continue;
            placed.set(index);
            if (!before.is(Blocks.AIR)) unusualAir.put(index, before);
            BossDeathBlocks.edit(() ->
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        }
    }
    void retract(int tick, int duration) {
        int target = (int) Math.ceil(cursor * Math.min(tick, duration) / (double) duration);
        while (cleanupCursor < target) restore(cursor - 1 - cleanupCursor++);
    }
    void restore(BlockPos pos) { int index = index(pos); if (index >= 0) restore(index); }
    private void restore(int index) {
        if (placed.get(index)) BossDeathBlocks.edit(() ->
                level.setBlock(position(index), unusualAir.getOrDefault(index, Blocks.AIR.defaultBlockState()),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
    }
    void clear() {
        for (int index = placed.nextSetBit(0); index >= 0; index = placed.nextSetBit(index + 1)) restore(index);
        placed.clear(); unusualAir.clear();
    }
}
