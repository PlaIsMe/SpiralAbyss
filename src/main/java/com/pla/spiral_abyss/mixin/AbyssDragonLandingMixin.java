package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.DragonArena;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.*;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonLandingApproachPhase.class)
public abstract class AbyssDragonLandingMixin extends AbstractDragonPhaseInstance {
    @Shadow private Path currentPath;
    @Shadow protected abstract void navigateToNextPathNode();
    protected AbyssDragonLandingMixin(EnderDragon dragon) { super(dragon); }
    @Inject(method = "findNewTarget", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$localApproach(CallbackInfo ci) {
        if (!DragonArena.managed(dragon)) return;
        if (currentPath == null || currentPath.isDone()) {
            int start = dragon.findClosestNode();
            var origin = dragon.getFightOrigin();
            var target = dragon.getTarget();
            var approach = DragonArena.approach(origin, target == null ? null : target.position());
            int finish = dragon.findClosestNode(approach.x, approach.y, approach.z);
            var landing = net.minecraft.core.BlockPos.containing(DragonArena.landingTarget(dragon));
            currentPath = dragon.findPath(start, finish, new Node(landing.getX(), landing.getY(), landing.getZ()));
            if (currentPath != null) currentPath.advance();
        }
        navigateToNextPathNode();
        if (currentPath != null && currentPath.isDone()) dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING);
        ci.cancel();
    }
}
