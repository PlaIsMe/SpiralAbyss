package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** Applies mob-pool difficulty after the mob and its mod have initialized attributes. */
public final class WaveScaling {
    public static final String HEALTH_DAMAGE_RATIO_TAG = "spiral_abyss_health_damage_ratio";
    public static final UUID HEALTH_MODIFIER = UUID.fromString("2fce3415-4a70-4976-a728-98234fdac4e3");

    private WaveScaling() {}

    public static void applyEndless(Mob mob, int wave, MobCategory category, double partyScale) {
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HEALTH_MODIFIER);
            health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
                    "Spiral Abyss health", 0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            double initial = health.getValue();
            double target = (category == MobCategory.BOSS ? EndlessWaves.bossHealth(wave)
                    : initial * EndlessWaves.mobScale(wave)) * partyScale;
            health.removeModifier(HEALTH_MODIFIER);
            if (initial > 0 && Double.isFinite(target)) health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
                    "Spiral Abyss health", target / initial - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
            mob.getPersistentData().remove(HEALTH_DAMAGE_RATIO_TAG);
        }
        mob.setHealth(mob.getMaxHealth());
        mob.getPersistentData().putDouble(WaveSpawner.DAMAGE_TAG, EndlessWaves.mobScale(wave) * partyScale);
    }

    public static void apply(Mob mob, int floor, MobCategory category,
                             ResourceLocation mobId, double partyScale,
                             MobCatalog.ScalingSnapshot plans) {
        var scaling = plans.at(category, mobId, floor);
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            // Opt in before reading the baseline, including modded base values above 1024.
            health.removeModifier(HEALTH_MODIFIER);
            health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
                    "Spiral Abyss health", 0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            double initial = health.getValue();
            double nativeTarget = (scaling.health().absolute()
                    ? scaling.health().value() : initial * scaling.health().value()) * partyScale;
            health.removeModifier(HEALTH_MODIFIER);
            if (initial > 0 && Double.isFinite(nativeTarget) && nativeTarget > 0) {
                health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
                        "Spiral Abyss health", nativeTarget / initial - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                // This synchronized modifier opts only this attribute into the expanded
                // health range. New spawns use real HP, without damage/healing conversion.
                mob.getPersistentData().remove(HEALTH_DAMAGE_RATIO_TAG);
            }
        }
        mob.setHealth(mob.getMaxHealth());
        mob.getPersistentData().putDouble(WaveSpawner.DAMAGE_TAG, scaling.damage() * partyScale);
    }
}
