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
    private static final UUID HEALTH_MODIFIER = UUID.fromString("2fce3415-4a70-4976-a728-98234fdac4e3");
    private static final double MAX_NATIVE_HEALTH = 1024;

    private WaveScaling() {}

    public static void apply(Mob mob, int floor, MobCategory category,
                             ResourceLocation mobId, double partyScale,
                             MobCatalog.ScalingSnapshot plans) {
        var scaling = plans.at(category, mobId, floor);
        double effectiveHealth = (scaling.health().absolute()
                ? scaling.health().value() : mob.getMaxHealth() * scaling.health().value()) * partyScale;
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double initial = health.getValue();
            double nativeTarget = Math.min(MAX_NATIVE_HEALTH, effectiveHealth);
            if (initial > 0 && Double.isFinite(nativeTarget) && nativeTarget > 0) {
                health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
                        "Spiral Abyss health", nativeTarget / initial - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                // The attribute caps at 1024 in 1.20.1. Reduce final incoming damage by
                // the same ratio so an above-cap boss retains its effective HP, while its
                // existing modded boss bar continues to display the correct health fraction.
                mob.getPersistentData().putDouble(HEALTH_DAMAGE_RATIO_TAG,
                        mob.getMaxHealth() / effectiveHealth);
            }
        }
        mob.setHealth(mob.getMaxHealth());
        mob.getPersistentData().putDouble(WaveSpawner.DAMAGE_TAG, scaling.damage() * partyScale);
    }
}
