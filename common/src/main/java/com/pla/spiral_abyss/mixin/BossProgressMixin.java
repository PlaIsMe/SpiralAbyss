package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.BossProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use confirmed vanilla kill credit rather than a death event that another mod may cancel. */
@Mixin(ServerPlayer.class)
public abstract class BossProgressMixin {
    @Inject(method = "awardKillScore", at = @At("HEAD"))
    private void spiralAbyss$bossVictory(Entity victim, int score, DamageSource source, CallbackInfo ci) {
        BossProgress.recordKill((ServerPlayer) (Object) this, victim);
    }
}
