package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.platform.AbyssPlatform;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerDataMixin {
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$death(
            net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        if (com.pla.spiral_abyss.fabric.FabricGameplayHooks.post(
                        new com.pla.spiral_abyss.platform.event.AbyssEvents.LivingDeathEvent(
                                player, source))
                .isCanceled()) ci.cancel();
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void spiralAbyss$clone(ServerPlayer old, boolean alive, CallbackInfo ci) {
        var data = AbyssPlatform.data(old);
        if (data.contains("PlayerPersisted", 10))
            AbyssPlatform.data((ServerPlayer) (Object) this)
                    .put("PlayerPersisted", data.getCompound("PlayerPersisted").copy());
    }
}
