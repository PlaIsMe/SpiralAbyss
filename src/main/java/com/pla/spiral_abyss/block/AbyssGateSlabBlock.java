package com.pla.spiral_abyss.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla slab merging/waterlogging, with the supplied eight-pixel-wide rail. */
public final class AbyssGateSlabBlock extends SlabBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape BOTTOM = GateShapes.box(4, 0, 0, 12, 8, 16);
    private static final VoxelShape TOP = GateShapes.box(4, 8, 0, 12, 16, 16);
    private static final VoxelShape DOUBLE = GateShapes.box(4, 0, 0, 12, 16, 16);
    private static final VoxelShape[] X_SHAPES = {GateShapes.turn(BOTTOM, Direction.EAST),
            GateShapes.turn(TOP, Direction.EAST), GateShapes.turn(DOUBLE, Direction.EAST)};
    public AbyssGateSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(AXIS);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null || context.getLevel().getBlockState(context.getClickedPos()).is(this) ? state
                : state.setValue(AXIS, context.getHorizontalDirection().getAxis());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = switch (state.getValue(TYPE)) { case BOTTOM -> 0; case TOP -> 1; case DOUBLE -> 2; };
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPES[index] : switch (index) {
            case 0 -> BOTTOM; case 1 -> TOP; default -> DOUBLE;
        };
    }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
}
