package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.fabric.FabricPlacementContext;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class BlockDestroyMixin {
    @Inject(
            method =
                    "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void spiralAbyss$place(
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            int flags,
            int recursion,
            CallbackInfoReturnable<Boolean> cir) {
        var player = FabricPlacementContext.player();
        if (player != null
                && FabricGameplayHooks.post(
                                new BlockEvent.EntityPlaceEvent(
                                        (Level) (Object) this, pos, state, player))
                        .isCanceled()) cir.setReturnValue(false);
    }

    @Inject(
            method =
                    "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void spiralAbyss$destroy(
            BlockPos pos,
            boolean drops,
            Entity source,
            int recursion,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(source instanceof LivingEntity living)) return;
        var level = (Level) (Object) this;
        var grief = FabricGameplayHooks.post(new EntityMobGriefingEvent(source));
        if (grief.getResult() == Event.Result.DENY
                || FabricGameplayHooks.post(
                                new LivingDestroyBlockEvent(living, pos, level.getBlockState(pos)))
                        .isCanceled()) cir.setReturnValue(false);
    }
}
