package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.BossHealthBars;
import net.minecraft.server.level.ServerBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerBossEvent.class)
public abstract class ServerBossEventMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void spiralAbyss$registerBossBar(CallbackInfo ci) {
        BossHealthBars.registered((ServerBossEvent) (Object) this);
    }
}
