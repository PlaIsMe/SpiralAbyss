package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssMobDrops;
import com.pla.spiral_abyss.encounter.MobTickContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {
    // Native die overrides can add items before/after super.die, outside dropAllDeathLoot.
    // Scope the entire virtual call, including when damage originates in a player's tick.
    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;die(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void spiralAbyss$scopeScriptedDeath(LivingEntity entity, DamageSource source) {
        MobTickContext.tick(entity, () -> entity.die(source));
    }

    // Stop the pipeline before loot tables, equipment, XP and overridden custom-death-loot
    // hooks execute. This also covers custom hooks that directly add ItemEntity instances.
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$preventMobDeathLoot(ServerLevel level, DamageSource source, CallbackInfo callback) {
        if (AbyssMobDrops.suppress((Entity) (Object) this)) callback.cancel();
    }
}
