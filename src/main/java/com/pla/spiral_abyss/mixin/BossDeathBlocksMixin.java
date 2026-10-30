package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla placement boundary: reject the reward before a block entity or its loot exists. */
@Mixin(Level.class)
public abstract class BossDeathBlocksMixin {
    // destroyBlock drops resources before calling setBlock: stop it before either effect.
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$preventDirectGriefing(BlockPos pos, boolean drops, Entity source, int recursion,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (BossDeathBlocks.suppressMobChange((Level) (Object) this)) callback.setReturnValue(false);
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$suppressDeathStructure(BlockPos pos, BlockState state, int flags, int recursion,
                                                     CallbackInfoReturnable<Boolean> callback) {
        if (BossDeathBlocks.suppress((Level) (Object) this, pos, state)) callback.setReturnValue(false);
    }
}
