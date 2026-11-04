package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.ExplosionEvent;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionEventsMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Unique private boolean spiralAbyss$canceled;

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$start(CallbackInfo ci) {
        spiralAbyss$canceled =
                FabricGameplayHooks.post(new ExplosionEvent.Start(level)).isCanceled();
        if (spiralAbyss$canceled) ci.cancel();
    }

    @Inject(method = "finalizeExplosion", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$detonate(boolean particles, CallbackInfo ci) {
        if (spiralAbyss$canceled) {
            ci.cancel();
            return;
        }
        FabricGameplayHooks.post(new ExplosionEvent.Detonate(level, toBlow));
    }
}
