package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.encounter.data.DeathBlockRule;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import com.pla.spiral_abyss.encounter.MobTickContext;

/** Suppresses configured delayed death structures without linking the boss's implementation. */
public final class BossDeathBlocks {
    public static final String TAG = "spiral_abyss_suppress_death_blocks";
    private record Guard(BlockPos origin, DeathBlockRule rule, long until) {
        boolean contains(BlockPos pos) {
            return Math.abs((long) pos.getX() - origin.getX()) <= rule.radius()
                    && Math.abs((long) pos.getZ() - origin.getZ()) <= rule.radius()
                    && pos.getY() >= origin.getY() && (long) pos.getY() - origin.getY() <= rule.height();
        }
    }
    private static final Map<ServerLevel, Map<UUID, Guard>> GUARDS = new WeakHashMap<>();
    private static final ThreadLocal<Integer> EDIT_DEPTH = ThreadLocal.withInitial(() -> 0);
    private BossDeathBlocks() {}

    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getLevel() instanceof ServerLevel)
                || !event.getLevel().dimension().equals(AbyssDimension.LEVEL)
                || !AbyssPlatform.data(mob).hasUUID(WaveSpawner.RUN_TAG)) return;
        var data = AbyssPlatform.data(mob);
        if (!data.contains(TAG) && MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(mob.getType())).deathBlocks().configured())
            data.putBoolean(TAG, true);
    }

    public static void died(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !level.dimension().equals(AbyssDimension.LEVEL)
                || !(AbyssPlatform.data(mob).contains(TAG) ? AbyssPlatform.data(mob).getBoolean(TAG)
                    : AbyssPlatform.data(mob).hasUUID(WaveSpawner.RUN_TAG))) return;
        var rule = MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(mob.getType())).deathBlocks();
        if (!rule.configured()) return;
        // Some bosses schedule their reward before super.die posts this event, even if
        // another listener subsequently cancels death. Keep those queued rewards blocked.
        GUARDS.computeIfAbsent(level, unused -> new HashMap<>()).put(mob.getUUID(),
                new Guard(mob.blockPosition().immutable(), rule, level.getGameTime() + rule.ticks()));
    }

    public static boolean suppress(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server) || !level.dimension().equals(AbyssDimension.LEVEL)
                || EDIT_DEPTH.get() > 0) return false;
        if (suppressMobChange(level)) return true;
        var scripted = scriptedGuard(level);
        if (scripted != null && scripted.contains(pos)
                && scripted.rule().blocks().contains(AbyssRegistries.BLOCKS.getKey(state.getBlock()))) return true;
        var guards = GUARDS.get(server);
        if (guards == null || guards.isEmpty()) return false;
        var id = AbyssRegistries.BLOCKS.getKey(state.getBlock());
        long now = level.getGameTime();
        return guards.values().stream().anyMatch(guard -> now <= guard.until()
                && guard.rule().blocks().contains(id) && guard.contains(pos));
    }

    /** Direct optional-mod block writes, including dragon breath; terrain restoration stays authorized. */
    public static boolean suppressMobChange(Level level) {
        var source = MobTickContext.blockChangeSource();
        return level instanceof ServerLevel && level.dimension().equals(AbyssDimension.LEVEL)
                && EDIT_DEPTH.get() == 0 && source != null && source.level() == level;
    }

    /** A rejected reward block must not redirect its loot table into a pre-existing container. */
    public static boolean suppressLoot(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !level.dimension().equals(AbyssDimension.LEVEL)
                || EDIT_DEPTH.get() > 0) return false;
        var scripted = scriptedGuard(level);
        if (scripted != null && scripted.contains(pos)) return true;
        var guards = GUARDS.get(server);
        return guards != null && guards.values().stream()
                .anyMatch(guard -> level.getGameTime() <= guard.until() && guard.contains(pos));
    }

    private static Guard scriptedGuard(Level level) {
        var source = MobTickContext.scriptedRewardSource();
        if (source == null || source.level() != level) return null;
        var rule = MobCatalog.bossOptions(AbyssRegistries.ENTITY_TYPES.getKey(source.getType())).deathBlocks();
        return rule.configured() ? new Guard(source.blockPosition(), rule, level.getGameTime()) : null;
    }

    /** Arena growth/restoration must remain authoritative even while a death reward is pending. */
    public static void edit(Runnable action) {
        int depth = EDIT_DEPTH.get();
        EDIT_DEPTH.set(depth + 1);
        try { action.run(); }
        finally {
            if (depth == 0) EDIT_DEPTH.remove();
            else EDIT_DEPTH.set(depth);
        }
    }

    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var guards = GUARDS.get(level);
        if (guards == null) return;
        guards.values().removeIf(guard -> level.getGameTime() > guard.until());
        if (guards.isEmpty()) GUARDS.remove(level);
    }

    public static void unload(LevelEvent.Unload event) { GUARDS.remove(event.getLevel()); }
}
