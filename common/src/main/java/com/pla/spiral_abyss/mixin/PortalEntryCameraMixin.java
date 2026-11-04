package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Kept in the client-only mixin list; never changes perspective options or the player entity. */
@Mixin(Camera.class)
public abstract class PortalEntryCameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void spiralAbyss$entryCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                        float partialTick, CallbackInfo callback) {
        var pose = PortalEntryAnimation.camera();
        if (pose == null) return;
        this.detached = true;
        setPosition(pose.position());
        setRotation(pose.yaw(), pose.pitch());
    }
}
