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
            com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.Camera camera,
            float partialTick,
            CallbackInfo ci) {
        if (PortalEntryAnimation.active()) ci.cancel();
    }
}
