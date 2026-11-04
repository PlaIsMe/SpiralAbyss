package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingEventsMixin {
    @Unique private DamageSource spiralAbyss$source;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$attack(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var entity = (LivingEntity) (Object) this;
        spiralAbyss$source = source;
        if (!entity.level().isClientSide
                && FabricGameplayHooks.post(new LivingAttackEvent(entity, source, amount))
                        .isCanceled()) cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$tick(CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide
                && FabricGameplayHooks.post(new LivingEvent.LivingTickEvent(entity)).isCanceled())
            ci.cancel();
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$death(DamageSource source, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide
                && FabricGameplayHooks.post(new LivingDeathEvent(entity, source)).isCanceled())
            ci.cancel();
    }

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float spiralAbyss$heal(float amount) {
        var entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) return amount;
        var event = FabricGameplayHooks.post(new LivingHealEvent(entity, amount));
        return event.isCanceled() ? 0 : event.getAmount();
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void spiralAbyss$totem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        var entity = (LivingEntity) (Object) this;
        if (cir.getReturnValue() && !entity.level().isClientSide)
            FabricGameplayHooks.post(
                    new LivingUseTotemEvent(entity, new ItemStack(Items.TOTEM_OF_UNDYING)));
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "hurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void spiralAbyss$shield(
            LivingEntity entity,
            float amount,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!entity.level().isClientSide && spiralAbyss$source != null)
            FabricGameplayHooks.post(new ShieldBlockEvent(entity, spiralAbyss$source, amount));
        original.call(entity, amount);
    }

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void spiralAbyss$equipment(
            CallbackInfoReturnable<
                            java.util.Map<net.minecraft.world.entity.EquipmentSlot, ItemStack>>
                    cir) {
        var entity = (LivingEntity) (Object) this;
        var changed = cir.getReturnValue();
        if (!entity.level().isClientSide && changed != null)
            changed.forEach(
                    (slot, stack) ->
                            FabricGameplayHooks.post(
                                    new LivingEquipmentChangeEvent(entity, slot, stack)));
    }
}
