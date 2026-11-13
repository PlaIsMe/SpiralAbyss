package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import java.util.Set;
import com.pla.spiral_abyss.encounter.MobTickContext;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;

/** Shared policy for death loot, custom drops and live-mob item drops. Players are excluded. */
public final class AbyssMobDrops {
    public static final String NO_PICKUP = "spiral_abyss_no_pickup";
    private AbyssMobDrops() {}

    public static boolean suppress(Entity entity) {
        return !entity.level().isClientSide && !(entity instanceof Player)
                && (AbyssPlatform.data(entity).hasUUID("spiral_abyss_environment_owner")
                || AbyssPlatform.data(entity).hasUUID(WaveSpawner.RUN_TAG)
                || entity instanceof Mob && (entity.level().dimension().equals(AbyssDimension.LEVEL)
                || AbyssPlatform.data(entity).hasUUID(WaveSpawner.RUN_TAG)));
    }

    /** Explicit custom loot carriers only. Never suppress players, living furniture or item drops. */
    public static boolean suppressAuxiliary(Entity entity) {
        return !entity.level().isClientSide && entity.level().dimension().equals(AbyssDimension.LEVEL)
                && !(entity instanceof LivingEntity) && !(entity instanceof ItemEntity)
                && MobCatalog.suppressesSpawn(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /** Block-state filter for custom falling-block death effects, scoped to their dying wave mob. */
    public static boolean suppressFallingBlock(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !level.dimension().equals(AbyssDimension.LEVEL)) return false;
        var targets = MobCatalog.suppressedFallingBlocks();
        if (targets.isEmpty()) return false;
        var block = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (targets.values().stream().noneMatch(blocks -> blocks.contains(block))) return false;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (!(entity instanceof Mob mob) || mob.isAlive() || !mob.blockPosition().equals(pos)
                    || !AbyssPlatform.data(mob).hasUUID(WaveSpawner.RUN_TAG)) continue;
            var owner = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (targets.getOrDefault(owner, Set.of()).contains(block)) return true;
        }
        return false;
    }

    public static void auxiliarySpawn(EntityJoinLevelEvent event) {
        // Runs for fresh spawns and entities loaded from disk. No optional mod classes are linked.
        if (suppressAuxiliary(event.getEntity())) event.setCanceled(true);
    }

    public static void auxiliaryInteract(PlayerInteractEvent.EntityInteract event) {
        if (suppressAuxiliary(event.getTarget()) || AbyssRewardCarriers.suppress(event.getTarget())) {
            // A datapack reload can enable suppression while old loot carriers are already loaded.
            event.getTarget().discard();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void auxiliaryInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (suppressAuxiliary(event.getTarget()) || AbyssRewardCarriers.suppress(event.getTarget())) {
            event.getTarget().discard();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    // Also catch drops supplied by other mods through Forge, after ordinary event handlers.
    public static void drops(LivingDropsEvent event) {
        if (suppress(event.getEntity())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    public static void experience(LivingExperienceDropEvent event) {
        if (suppress(event.getEntity())) event.setDroppedExperience(0);
    }

    // BehaviorUtils.throwItem and fox spitting bypass spawnAtLocation, but record the source mob.
    // The same safeguard covers modded direct item creation when its thrower is identified.
    public static void thrownItem(EntityJoinLevelEvent event) {
        var scriptedSource = MobTickContext.scriptedRewardSource();
        if (scriptedSource != null && event.getLevel() == scriptedSource.level()
                && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)) {
            event.setCanceled(true);
            return;
        }
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof AbstractArrow arrow
                && (AbyssPlatform.data(arrow).getBoolean(NO_PICKUP) || (arrow.getOwner() != null && suppress(arrow.getOwner())))) {
            // Persist both the vanilla pickup mode and our marker across chunk/server reloads.
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            AbyssPlatform.data(arrow).putBoolean(NO_PICKUP, true);
        }
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof ItemEntity item) {
            Entity thrower = item.getOwner();
            if (thrower != null && suppress(thrower)) event.setCanceled(true);
        }
    }
}
