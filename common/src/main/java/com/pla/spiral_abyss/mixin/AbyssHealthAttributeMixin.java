package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.WaveScaling;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Expand only encounter-scaled max health, on both server and synchronized clients. */
@Mixin(AttributeInstance.class)
public abstract class AbyssHealthAttributeMixin {
    @Redirect(method = "calculateValue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"))
    private double spiralAbyss$scaledHealth(Attribute attribute, double value) {
        double sanitized = attribute.sanitizeValue(value);
        if (attribute == Attributes.MAX_HEALTH && Double.isFinite(value) && value > 1024
                && ((AttributeInstance) (Object) this).getModifier(WaveScaling.HEALTH_MODIFIER) != null)
            return Math.max(sanitized, Math.min(Float.MAX_VALUE, value));
        return sanitized;
    }
}
