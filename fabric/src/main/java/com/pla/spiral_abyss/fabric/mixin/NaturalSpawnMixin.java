package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class NaturalSpawnMixin {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void spiralAbyss$ambient(
            EntityType<?> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.getLevel().dimension().equals(AbyssDimension.LEVEL)
                && (reason == MobSpawnType.NATURAL
                        || reason == MobSpawnType.CHUNK_GENERATION
                        || reason == MobSpawnType.PATROL)) cir.setReturnValue(false);
    }
}
