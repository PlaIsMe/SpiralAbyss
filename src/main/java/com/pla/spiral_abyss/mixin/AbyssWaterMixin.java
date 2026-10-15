package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Abyss water is a static arena volume, including saved water before a slot is rebuilt. */
@Mixin(FlowingFluid.class)
public abstract class AbyssWaterMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$stillWater(Level level, BlockPos pos, FluidState state, CallbackInfo callback) {
        if (level.dimension().equals(AbyssDimension.LEVEL) && state.is(FluidTags.WATER)) callback.cancel();
    }
}
