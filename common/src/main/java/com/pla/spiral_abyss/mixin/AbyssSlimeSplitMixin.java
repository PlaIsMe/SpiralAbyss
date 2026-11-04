package com.pla.spiral_abyss.mixin;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.encounter.WaveSpawner;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Wave counts include the configured slimes, not untracked children created during removal. */
@Mixin(Slime.class)
public abstract class AbyssSlimeSplitMixin {
    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Slime;getSize()I"))
    private int spiralAbyss$noWaveSplitting(Slime slime) {
        return AbyssPlatform.data(slime).hasUUID(WaveSpawner.RUN_TAG) ? 1 : slime.getSize();
    }
}
