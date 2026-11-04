package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.world.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class AbyssPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_PLANE = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape Z_PLANE = Block.box(7, 0, 0, 9, 16, 16);
    public AbyssPortalBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_PLANE : Z_PLANE;
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Clean up orphan cells if the anchor was broken while this chunk was unloaded.
        if (level.hasChunksAt(pos.offset(-3, -5, -3), pos.offset(3, 5, 3))
                && AbyssPortalFrame.fromOpening(level, pos, state.getValue(AXIS)).isEmpty())
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.isSpectator() || !player.isAlive()
                || !level.dimension().equals(Level.OVERWORLD)) return;
        if (!player.getBoundingBox().intersects(getShape(state, level, pos, CollisionContext.empty()).bounds().move(pos))) return;
        if (player.isOnPortalCooldown()) { player.setPortalCooldown(); return; }
        player.setPortalCooldown(); // Also throttle failed admission while standing in the surface.
        AbyssPortalFrame.fromOpening(level, pos, state.getValue(AXIS)).filter(frame -> frame.active(level))
                .ifPresent(frame -> AbyssParties.get(player.server).portals().touch(player, frame));
    }
}
