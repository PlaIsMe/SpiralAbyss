package com.pla.spiral_abyss.fabric.mixin.client;

import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;

import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class PortalHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$hand(
            net.minecraft.client.Camera camera,
            float partialTick,
            org.joml.Matrix4f projection,
            CallbackInfo ci) {
        if (PortalEntryAnimation.active()) ci.cancel();
    }
}
