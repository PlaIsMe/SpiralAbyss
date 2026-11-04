package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class LivingDamageMixin {
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float spiralAbyss$incoming(float amount, DamageSource source, float original) {
        var entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) return amount;
        var event = FabricGameplayHooks.post(new LivingIncomingDamageEvent(entity, source, amount));
        return event.isCanceled() ? 0 : event.getAmount();
    }

    @ModifyVariable(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"),
            argsOnly = true,
            ordinal = 0)
    private float spiralAbyss$pre(float amount, DamageSource source, float original) {
        var entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) return amount;
        return FabricGameplayHooks.post(new LivingDamageEvent.Pre(entity, source, amount))
                .getNewDamage();
    }

    @Inject(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V",
                            shift = At.Shift.AFTER))
    private void spiralAbyss$post(DamageSource source, float amount, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide)
            FabricGameplayHooks.post(new LivingDamageEvent.Post(entity, source, amount));
    }
}
