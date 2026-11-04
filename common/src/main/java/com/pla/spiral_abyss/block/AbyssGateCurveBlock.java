package com.pla.spiral_abyss.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** A fixed architectural curve with stair-style facing/half placement, not corner joining. */
public final class AbyssGateCurveBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<AbyssGateCurveBlock> CODEC = simpleCodec(AbyssGateCurveBlock::new);
    @Override public MapCodec<AbyssGateCurveBlock> codec() { return CODEC; }
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape[][] SHAPES = new VoxelShape[2][4];
    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES[0][facing.get2DDataValue()] = GateShapes.turn(GateShapes.CURVE_BOTTOM, facing);
            SHAPES[1][facing.get2DDataValue()] = GateShapes.turn(GateShapes.CURVE_TOP, facing);
        }
    }
    public AbyssGateCurveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM).setValue(WATERLOGGED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, WATERLOGGED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        boolean top = face == Direction.DOWN || face != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(HALF, top ? Half.TOP : Half.BOTTOM)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(HALF) == Half.TOP ? 1 : 0][state.getValue(FACING).get2DDataValue()];
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
