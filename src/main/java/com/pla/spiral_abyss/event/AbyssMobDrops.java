package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;

/** Shared policy for death loot, custom drops and live-mob item drops. Players are excluded. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssMobDrops {
    public static final String NO_PICKUP = "spiral_abyss_no_pickup";
    private AbyssMobDrops() {}

    public static boolean suppress(Entity entity) {
        return entity instanceof Mob && !entity.level().isClientSide
                && (entity.level().dimension().equals(AbyssDimension.LEVEL)
                || entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG));
    }

    /** Explicit custom loot carriers only. Never suppress players, living furniture or item drops. */
    public static boolean suppressAuxiliary(Entity entity) {
        return !entity.level().isClientSide && entity.level().dimension().equals(AbyssDimension.LEVEL)
                && !(entity instanceof LivingEntity) && !(entity instanceof ItemEntity)
                && MobCatalog.suppressesSpawn(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void auxiliarySpawn(EntityJoinLevelEvent event) {
        // Runs for fresh spawns and entities loaded from disk. No optional mod classes are linked.
        if (suppressAuxiliary(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void auxiliaryInteract(PlayerInteractEvent.EntityInteract event) {
        if (suppressAuxiliary(event.getTarget())) {
            // A datapack reload can enable suppression while old body parts are already loaded.
            event.getTarget().discard();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void auxiliaryInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (suppressAuxiliary(event.getTarget())) {
            event.getTarget().discard();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    // Also catch drops supplied by other mods through Forge, after ordinary event handlers.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void drops(LivingDropsEvent event) {
        if (suppress(event.getEntity())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void experience(LivingExperienceDropEvent event) {
        if (suppress(event.getEntity())) event.setDroppedExperience(0);
    }

    // BehaviorUtils.throwItem and fox spitting bypass spawnAtLocation, but record the source mob.
    // The same safeguard covers modded direct item creation when its thrower is identified.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void thrownItem(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                && (arrow.getPersistentData().getBoolean(NO_PICKUP) || (arrow.getOwner() != null && suppress(arrow.getOwner())))) {
            // Persist both the vanilla pickup mode and our marker across chunk/server reloads.
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putBoolean(NO_PICKUP, true);
        }
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof ItemEntity item) {
            Entity thrower = item.getOwner();
            if (thrower != null && suppress(thrower)) event.setCanceled(true);
        }
    }
}
