package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssMobDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops configured death-effect blocks before vanilla removes the source position. */
@Mixin(FallingBlockEntity.class)
public abstract class AbyssFallingBlockMixin {
    @Inject(method = "fall", at = @At("HEAD"), cancellable = true)
    private static void spiral_abyss$suppressBossFallingBlock(Level level, BlockPos pos, BlockState state,
                                                                 CallbackInfoReturnable<FallingBlockEntity> callback) {
        if (AbyssMobDrops.suppressFallingBlock(level, pos, state)) callback.setReturnValue(null);
    }
}
