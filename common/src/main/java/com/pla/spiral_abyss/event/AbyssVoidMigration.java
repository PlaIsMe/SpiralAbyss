package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

/** Remove the retired flat generator's bedrock layer from existing Abyss chunks as they load. */
public final class AbyssVoidMigration {
    private AbyssVoidMigration() {}
    public static void load(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(AbyssDimension.LEVEL)) return;
        var chunk = event.getChunk();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            pos.set(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z);
            if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                chunk.setUnsaved(true);
            }
        }
    }
}
