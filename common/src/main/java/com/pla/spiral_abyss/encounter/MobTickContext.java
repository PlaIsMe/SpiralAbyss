package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Identifies synchronous scripted rewards without guessing ownership from nearby players/items. */
public final class MobTickContext {
    private static final TagKey<EntityType<?>> SCRIPTED_LOOT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "suppress_tick_loot"));
    private static final TagKey<EntityType<?>> PROTECT_INVENTORY = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "protect_player_inventory"));
    private static final TagKey<EntityType<?>> NO_BLOCK_CHANGES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "no_arena_block_changes"));
    private static final TagKey<EntityType<?>> SUPPRESS_DEATH_EFFECTS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "suppress_death_effects"));
    private static final TagKey<EntityType<?>> REMOVAL_LOOT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "suppress_removal_loot"));
    private static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<LivingEntity> BLOCK_SOURCE = new ThreadLocal<>();
    private MobTickContext() {}

    public static void tick(Entity entity, Runnable action) {
        QliphothEncounters.tick(entity, () -> guardedTick(entity, action));
    }

    private static void guardedTick(Entity entity, Runnable action) {
        LivingEntity current = null;
        LivingEntity blockSource = QliphothEncounters.blockSource();
        if (entity.level().dimension().equals(AbyssDimension.LEVEL)) {
            Entity owner = entity instanceof Projectile projectile ? projectile.getOwner() : entity;
            if (owner instanceof LivingEntity mob && owner.level() == entity.level()
                    && owner.getType().is(NO_BLOCK_CHANGES) && AbyssPlatform.data(owner).hasUUID(WaveSpawner.RUN_TAG))
                blockSource = mob;
            if (entity instanceof LivingEntity mob && (entity.getType().is(REMOVAL_LOOT)
                    || (entity.getType().is(SCRIPTED_LOOT) || entity.getType().is(PROTECT_INVENTORY)
                    || entity.getType().is(SUPPRESS_DEATH_EFFECTS))
                    && AbyssPlatform.data(entity).hasUUID(WaveSpawner.RUN_TAG)))
                current = mob;
        }
        LivingEntity previous = CURRENT.get();
        LivingEntity previousBlockSource = BLOCK_SOURCE.get();
        // Most Abyss entities need neither guard. Do not remove and recreate their
        // thread-local entries on every tick; nested calls still clear an outer source.
        boolean changeCurrent = current != previous;
        boolean changeBlockSource = blockSource != previousBlockSource;
        if (changeCurrent) set(CURRENT, current);
        if (changeBlockSource) set(BLOCK_SOURCE, blockSource);
        try { action.run(); }
        finally {
            if (changeCurrent) set(CURRENT, previous);
            if (changeBlockSource) set(BLOCK_SOURCE, previousBlockSource);
        }
    }

    private static void set(ThreadLocal<LivingEntity> context, LivingEntity source) {
        if (source == null) context.remove();
        else context.set(source);
    }

    public static LivingEntity scriptedRewardSource() {
        LivingEntity source = CURRENT.get();
        return source != null && (source.getType().is(SCRIPTED_LOOT) || source.getType().is(REMOVAL_LOOT))
                ? source : null;
    }
    public static boolean protectsInventory() {
        LivingEntity source = CURRENT.get();
        return source != null && source.getType().is(PROTECT_INVENTORY);
    }

    /** Also covers successor finalizeSpawn side effects invoked inside a native die override. */
    public static LivingEntity suppressedDeathSource() {
        LivingEntity source = CURRENT.get();
        return source != null && source.getHealth() <= 0 && source.getType().is(SUPPRESS_DEATH_EFFECTS)
                ? source : null;
    }

    public static LivingEntity blockChangeSource() {
        LivingEntity source = BLOCK_SOURCE.get();
        return source == null ? suppressedDeathSource() : source;
    }
}
