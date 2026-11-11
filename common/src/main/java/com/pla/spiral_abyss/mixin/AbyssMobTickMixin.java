package com.pla.spiral_abyss.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scope direct scripted loot to its originating mob, including passenger ticks. */
@Mixin(ServerLevel.class)
public abstract class AbyssMobTickMixin {
    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void spiral_abyss$tick(Entity entity, Operation<Void> original) {
        // Delegate through the operation so other mods' tick guards remain in the chain.
        // This wrapper is installed in every dimension. Keep ordinary world ticks free
        // of context lookups, projectile-owner resolution and per-entity Runnables.
        if (entity.level().dimension().equals(AbyssDimension.LEVEL)) MobTickContext.tick(entity, () -> original.call(entity));
        else original.call(entity);
    }

    @WrapOperation(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void spiral_abyss$rideTick(Entity entity, Operation<Void> original) {
        if (entity.level().dimension().equals(AbyssDimension.LEVEL)) MobTickContext.tick(entity, () -> original.call(entity));
        else original.call(entity);
    }
}
