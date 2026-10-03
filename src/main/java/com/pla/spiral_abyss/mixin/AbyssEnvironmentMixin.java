package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class AbyssEnvironmentMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$clearWeather(CallbackInfo callback) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!level.dimension().equals(AbyssDimension.LEVEL)) return;
        // Only this level's interpolated weather changes; shared overworld weather data stays intact.
        level.setRainLevel(0); level.setThunderLevel(0);
        callback.cancel();
    }
    @Inject(method = "tickCustomSpawners", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$noAmbientSpawners(boolean enemies, boolean friendlies, CallbackInfo callback) {
        if (((ServerLevel) (Object) this).dimension().equals(AbyssDimension.LEVEL)) callback.cancel();
    }
}
