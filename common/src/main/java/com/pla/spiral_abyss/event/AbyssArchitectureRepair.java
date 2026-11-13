package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Repairs actual architecture removals, including mods that bypass Forge's griefing events. */
public final class AbyssArchitectureRepair {
    private static final class Pending {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final Set<BlockPos> creativeBreaks = new HashSet<>();
        int editDepth;
    }
    private static final Map<ServerLevel, Pending> LEVELS = new WeakHashMap<>();
    private AbyssArchitectureRepair() {}

    public static void removed(Level level, BlockPos pos, BlockState previous, BlockState replacement) {
        if (!(level instanceof ServerLevel server) || !level.dimension().equals(AbyssDimension.LEVEL)) return;
        Pending existing = LEVELS.get(server);
        if (existing != null && existing.editDepth > 0) {
            existing.blocks.remove(pos);
            return;
        }
        if (previous == null) return;
        // Palette blocks are not native architecture. Resolve their desired state from
        // the live terrain ledger instead of saving a stale state across room cleanup.
        if (pos.getY() == ArenaBounds.FLOOR_Y - 1 && AbyssParties.queueBossFloorRepair(server, pos)) return;
        if (previous.is(replacement.getBlock()) || !BlockInit.isAbyssArchitecture(previous.getBlock())) return;
        Pending pending = LEVELS.computeIfAbsent(server, ignored -> new Pending());
        if (pending.creativeBreaks.remove(pos)) return;
        // Keep the first state if another mod changes the same cell again before repair.
        pending.blocks.putIfAbsent(pos.immutable(), previous);
    }

    /** Structure rebuilds and owned temporary environments intentionally replace architecture. */
    public static void edit(ServerLevel level, Runnable action) {
        Pending pending = LEVELS.computeIfAbsent(level, ignored -> new Pending());
        pending.editDepth++;
        try { BossDeathBlocks.edit(action); }
        finally { pending.editDepth--; }
    }

    public static void forget(ServerLevel level, BlockPos from, BlockPos to) {
        Pending pending = LEVELS.get(level);
        if (pending == null) return;
        var area = BoundingBox.fromCorners(from, to);
        pending.blocks.keySet().removeIf(area::isInside);
        pending.creativeBreaks.removeIf(area::isInside);
    }

    public static void creativeBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer().isCreative() && event.getLevel() instanceof ServerLevel level
                && level.dimension().equals(AbyssDimension.LEVEL)) {
            Pending pending = LEVELS.computeIfAbsent(level, ignored -> new Pending());
            pending.creativeBreaks.add(event.getPos().immutable());
            pending.blocks.remove(event.getPos());
        }
    }

    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) repair(level);
    }

    public static void repair(ServerLevel level) {
        Pending pending = LEVELS.get(level);
        if (pending == null) return;
        pending.creativeBreaks.clear();
        if (pending.blocks.isEmpty()) return;
        Map<BlockPos, BlockState> repairs = new HashMap<>(pending.blocks);
        pending.blocks.clear();
        // Defer until the removal call is finished, avoiding recursive setBlock/onRemove calls.
        edit(level, () -> repairs.forEach((pos, state) -> {
            if (level.hasChunkAt(pos) && level.getBlockState(pos) != state)
                level.setBlock(pos, state, Block.UPDATE_ALL);
        }));
    }

    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) LEVELS.remove(level);
    }
}
