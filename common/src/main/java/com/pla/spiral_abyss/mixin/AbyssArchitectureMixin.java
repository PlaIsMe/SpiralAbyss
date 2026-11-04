package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class AbyssArchitectureMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void spiral_abyss$queueRepair(BlockPos pos, BlockState state, boolean moving,
                                         CallbackInfoReturnable<BlockState> callback) {
        AbyssArchitectureRepair.removed(((LevelChunk) (Object) this).getLevel(), pos,
                callback.getReturnValue(), state);
    }
}
