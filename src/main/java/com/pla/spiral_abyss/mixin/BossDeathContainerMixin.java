package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class BossDeathContainerMixin {
    @Inject(method = "setLootTable(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD"), cancellable = true)
    private static void spiral_abyss$suppressDeathLoot(BlockGetter reader, RandomSource random, BlockPos pos,
                                                      ResourceLocation table, CallbackInfo callback) {
        if (reader instanceof Level level && BossDeathBlocks.suppressLoot(level, pos)) callback.cancel();
    }
}
