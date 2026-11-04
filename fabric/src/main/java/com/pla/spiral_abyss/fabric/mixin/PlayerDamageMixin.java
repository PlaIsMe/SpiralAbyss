package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerDamageMixin {
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float spiralAbyss$hurt(float amount, DamageSource source, float original) {
        var entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) return amount;
        var event = FabricGameplayHooks.post(new LivingHurtEvent(entity, source, amount));
        return event.isCanceled() ? 0 : event.getAmount();
    }

    @ModifyVariable(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"),
            argsOnly = true,
            ordinal = 0)
    private float spiralAbyss$damage(float amount, DamageSource source, float original) {
        var entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) return amount;
        var event = FabricGameplayHooks.post(new LivingDamageEvent(entity, source, amount));
        return event.isCanceled() ? 0 : event.getAmount();
    }
}
