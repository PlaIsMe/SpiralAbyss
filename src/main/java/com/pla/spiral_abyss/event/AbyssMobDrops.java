package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shared policy for death loot, custom drops and live-mob item drops. Players are excluded. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssMobDrops {
    private AbyssMobDrops() {}

    public static boolean suppress(Entity entity) {
        return entity instanceof Mob && !entity.level().isClientSide
                && (entity.level().dimension().equals(AbyssDimension.LEVEL)
                || entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG));
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
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof ItemEntity item) {
            Entity thrower = item.getOwner();
            if (thrower != null && suppress(thrower)) event.setCanceled(true);
        }
    }
}
