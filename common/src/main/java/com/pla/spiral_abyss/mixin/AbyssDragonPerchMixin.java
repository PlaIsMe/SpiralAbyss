package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.DragonArena;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonLandingPhase.class)
public abstract class AbyssDragonPerchMixin extends AbstractDragonPhaseInstance {
    @Shadow private Vec3 targetLocation;
    protected AbyssDragonPerchMixin(EnderDragon dragon) { super(dragon); }
    @Inject(method = "doServerTick", at = @At("HEAD"))
    private void spiralAbyss$terrainSurface(CallbackInfo ci) {
        if (DragonArena.managed(dragon)) targetLocation = DragonArena.landingTarget(dragon);
    }
}
