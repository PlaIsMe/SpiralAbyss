package com.pla.spiral_abyss.encounter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Set;

/** Optional custom death sequences whose zero-health state is not the end of an encounter. */
public final class MobDeathLifecycle {
    private static final TagKey<EntityType<?>> WAIT_FOR_REMOVAL = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "waits_for_removal"));
    private static final TagKey<EntityType<?>> DELAYS_DEATH_EVENT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "delays_death_event"));
    private static final Set<ResourceLocation> DELAYED_DEATH_EVENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("mutantmonsters", "mutant_zombie"),
            ResourceLocation.fromNamespaceAndPath("mutantmonsters", "mutant_creeper"),
            ResourceLocation.fromNamespaceAndPath("mutantmonsters", "mutant_enderman"),
            ResourceLocation.fromNamespaceAndPath("goety", "apostle"));
    // Goety 2.5.59.2 bosses remove themselves after their death animation.
    // Apostle also defers die()/custom loot until that sequence finishes.
    private static final Set<ResourceLocation> DELAYED_GOETY = Set.of(
            ResourceLocation.fromNamespaceAndPath("goety", "vizier"),
            ResourceLocation.fromNamespaceAndPath("goety", "ender_keeper"),
            ResourceLocation.fromNamespaceAndPath("goety", "hostile_redstone_monstrosity"));
    private MobDeathLifecycle() {}

    public static boolean delaysDeathEvent(Mob mob) {
        return mob.getType().is(DELAYS_DEATH_EVENT)
                || DELAYED_DEATH_EVENTS.contains(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
    }

    public static boolean waitsForRemoval(Mob mob) {
        return mob instanceof EnderDragon || delaysDeathEvent(mob) || mob.getType().is(WAIT_FOR_REMOVAL)
                || DELAYED_GOETY.contains(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
    }
}
