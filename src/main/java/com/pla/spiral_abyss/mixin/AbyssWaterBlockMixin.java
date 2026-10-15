package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public abstract class AbyssWaterBlockMixin {
    // Avoid scheduling half a million fluid ticks while filling a full-water environment.
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$staticWater(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving, CallbackInfo callback) {
        if (level.dimension().equals(AbyssDimension.LEVEL) && state.getFluidState().is(FluidTags.WATER)) callback.cancel();
    }
}
