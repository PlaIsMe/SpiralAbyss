package com.pla.spiral_abyss.fabric.mixin.client;

import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;

import net.minecraft.client.gui.Gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class PortalPresentationMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$hud(
            net.minecraft.client.gui.GuiGraphics graphics,
            net.minecraft.client.DeltaTracker deltaTracker,
            CallbackInfo ci) {
        if (PortalEntryAnimation.active()) ci.cancel();
    }
}
