package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Explicit noncombat reward replacements and persistent harvestable corpses, only in the Abyss. */
@EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssRewardCarriers {
    private static final TagKey<EntityType<?>> CARRIERS = tag("suppressed_reward_carriers");
    private static final TagKey<EntityType<?>> CORPSES = tag("remove_corpse_on_death");
    private static final TagKey<EntityType<?>> DEATH_SPAWNS = tag("suppressed_death_spawns");
    private AbyssRewardCarriers() {}

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, name));
    }

    public static boolean suppress(Entity entity) {
        return entity instanceof Mob mob && !entity.level().isClientSide
                && entity.level().dimension().equals(AbyssDimension.LEVEL)
                && (entity.getType().is(CARRIERS) || entity.getType().is(CORPSES)
                && entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG) && mob.getHealth() <= 0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void join(EntityJoinLevelEvent event) {
        var source = MobTickContext.suppressedDeathSource();
        if (suppress(event.getEntity()) || source != null && event.getLevel() == source.level()
                && event.getEntity().getType().is(DEATH_SPAWNS)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void deathExplosion(ExplosionEvent.Start event) {
        var source = MobTickContext.suppressedDeathSource();
        if (source != null && event.getLevel() == source.level()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(EntityTickEvent.Pre event) {
        if (suppress(event.getEntity())) {
            event.getEntity().discard();
            event.setCanceled(true);
        }
    }
}
