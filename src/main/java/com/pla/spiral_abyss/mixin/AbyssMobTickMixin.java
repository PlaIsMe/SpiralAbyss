package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.MobTickContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Scope direct scripted loot to its originating mob, including passenger ticks. */
@Mixin(ServerLevel.class)
public abstract class AbyssMobTickMixin {
    @Redirect(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void spiral_abyss$tick(Entity entity) { MobTickContext.tick(entity, entity::tick); }

    @Redirect(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void spiral_abyss$rideTick(Entity entity) { MobTickContext.tick(entity, entity::rideTick); }
}
