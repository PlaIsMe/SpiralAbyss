package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssMobDrops;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {
    // Stop the pipeline before loot tables, equipment, XP and overridden custom-death-loot
    // hooks execute. This also covers custom hooks that directly add ItemEntity instances.
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$preventMobDeathLoot(DamageSource source, CallbackInfo callback) {
        if (AbyssMobDrops.suppress((Entity) (Object) this)) callback.cancel();
    }
}
