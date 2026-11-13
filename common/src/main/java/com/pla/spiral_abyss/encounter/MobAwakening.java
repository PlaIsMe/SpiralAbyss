package com.pla.spiral_abyss.encounter;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Opt-in native awakening for mobs with separate awake and sleeping-pose state. */
public final class MobAwakening {
    private static final TagKey<EntityType<?>> SPAWN_AWAKENED = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("spiral_abyss", "spawn_awakened"));

    private MobAwakening() {}

    public static void apply(Mob mob) {
        if (!mob.getType().is(SPAWN_AWAKENED)) return;
        try {
            // Resolve both methods before changing state. Optional classes never link here.
            var awaken = mob.getClass().getMethod("setAwaken", boolean.class);
            var sleep = mob.getClass().getMethod("setSleep", boolean.class);
            awaken.invoke(mob, true);
            // finalizeSpawn can leave an unsaved sleep animation active even with Awaken NBT.
            sleep.invoke(mob, false);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Cannot awaken tagged arena mob " + mob.getType(), ex);
        }
    }
}
