package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.ProjectileImpactEvent;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {
    @Inject(method = "onHit", at = @At("RETURN"))
    private void spiralAbyss$impact(HitResult hit, CallbackInfo ci) {
        var projectile = (Projectile) (Object) this;
        if (!projectile.level().isClientSide)
            FabricGameplayHooks.post(new ProjectileImpactEvent(projectile, hit));
    }
}
