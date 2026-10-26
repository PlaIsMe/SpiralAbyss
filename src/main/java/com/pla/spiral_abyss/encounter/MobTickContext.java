package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Identifies synchronous scripted rewards without guessing ownership from nearby players/items. */
public final class MobTickContext {
    private static final TagKey<EntityType<?>> SCRIPTED_LOOT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "suppress_tick_loot"));
    private static final TagKey<EntityType<?>> PROTECT_INVENTORY = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "protect_player_inventory"));
    private static final ThreadLocal<Mob> CURRENT = new ThreadLocal<>();
    private MobTickContext() {}

    public static void tick(Entity entity, Runnable action) {
        Mob previous = CURRENT.get();
        if (entity instanceof Mob mob && entity.level().dimension().equals(AbyssDimension.LEVEL)
                && entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)
                && (entity.getType().is(SCRIPTED_LOOT) || entity.getType().is(PROTECT_INVENTORY)))
            CURRENT.set(mob);
        else CURRENT.remove();
        try { action.run(); }
        finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static Mob scriptedRewardSource() {
        Mob source = CURRENT.get();
        return source != null && source.getType().is(SCRIPTED_LOOT) ? source : null;
    }
    public static boolean protectsInventory() {
        Mob source = CURRENT.get();
        return source != null && source.getType().is(PROTECT_INVENTORY);
    }
}
