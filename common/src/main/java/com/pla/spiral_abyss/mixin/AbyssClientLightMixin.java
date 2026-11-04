package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Full ambient light at the rendering input, including shader and terrain-section consumers. */
@Mixin(LightEngine.class)
public abstract class AbyssClientLightMixin {
    @Shadow @Final protected LightChunkGetter chunkSource;

    @Unique
    private boolean spiralAbyss$fullAmbientLight() {
        return chunkSource.getLevel() instanceof Level level && level.isClientSide
                && level.dimension().equals(AbyssDimension.LEVEL);
    }

    @Inject(method = "getLightValue", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$ambientLight(BlockPos pos, CallbackInfoReturnable<Integer> callback) {
        if (spiralAbyss$fullAmbientLight()) callback.setReturnValue(15);
    }

    @Inject(method = "getDataLayerData", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$ambientSection(SectionPos section, CallbackInfoReturnable<DataLayer> callback) {
        if (spiralAbyss$fullAmbientLight()) callback.setReturnValue(new DataLayer(15));
    }
}
