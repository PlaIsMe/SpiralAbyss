package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Scope direct scripted loot to its originating mob, including passenger ticks. */
@Mixin(ServerLevel.class)
public abstract class AbyssMobTickMixin {
    @Redirect(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void spiral_abyss$tick(Entity entity) {
        // This redirect is installed in every dimension. Keep ordinary world ticks free
        // of context lookups, projectile-owner resolution and per-entity Runnables.
        if (entity.level().dimension().equals(AbyssDimension.LEVEL)) MobTickContext.tick(entity, entity::tick);
        else entity.tick();
    }

    @Redirect(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void spiral_abyss$rideTick(Entity entity) {
        if (entity.level().dimension().equals(AbyssDimension.LEVEL)) MobTickContext.tick(entity, entity::rideTick);
        else entity.rideTick();
    }
}
