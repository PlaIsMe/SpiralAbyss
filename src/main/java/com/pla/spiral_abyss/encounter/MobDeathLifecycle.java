package com.pla.spiral_abyss.encounter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Set;

/** Optional custom death sequences whose zero-health state is not the end of an encounter. */
public final class MobDeathLifecycle {
    private static final Set<ResourceLocation> DELAYED_MUTANTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("mutantmonsters", "mutant_zombie"),
            ResourceLocation.fromNamespaceAndPath("mutantmonsters", "mutant_creeper"),
            ResourceLocation.fromNamespaceAndPath("mutantmonsters", "mutant_enderman"));
    private MobDeathLifecycle() {}

    public static boolean delayedMutant(Mob mob) {
        return DELAYED_MUTANTS.contains(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
    }

    public static boolean waitsForRemoval(Mob mob) {
        return mob instanceof EnderDragon || delayedMutant(mob);
    }
}
