package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla slab merging/waterlogging with collision matching the exported rail models. */
public final class AbyssGateSlabBlock extends SlabBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public enum Extension implements StringRepresentable {
        NONE("none", null), NORTH("north", Direction.NORTH), EAST("east", Direction.EAST),
        SOUTH("south", Direction.SOUTH), WEST("west", Direction.WEST);

        private final String name;
        private final Direction direction;
        Extension(String name, Direction direction) { this.name = name; this.direction = direction; }
        @Override public String getSerializedName() { return name; }
        static Extension toward(Direction direction) {
            return switch (direction) {
                case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST;
                default -> NONE;
            };
        }
    }
    public static final EnumProperty<Extension> EXTENSION = EnumProperty.create("extension", Extension.class);
    private static final VoxelShape BOTTOM = GateShapes.box(2, 0, 0, 14, 12, 16);
    private static final VoxelShape TOP = GateShapes.box(2, 8, 0, 14, 20, 16);
    private static final VoxelShape DOUBLE = GateShapes.box(2, 0, 0, 14, 16, 16);
    // The extra pieces in abyss_gate_horizontal_activate project ten model pixels past one beam end.
    private static final VoxelShape EXTENDED_NORTH = Shapes.or(BOTTOM,
            GateShapes.box(2, 0, -10, 14, 1, -9), GateShapes.box(2, 0, -9, 14, 2, 0),
            GateShapes.box(2, 2, -8, 14, 3, 0), GateShapes.box(2, 3, -7, 14, 4, -6),
            GateShapes.box(2, 3, -6, 14, 5, -5), GateShapes.box(2, 3, -5, 14, 6, -4),
            GateShapes.box(2, 3, -4, 14, 7, -3), GateShapes.box(2, 3, -3, 14, 8, -2),
            GateShapes.box(2, 3, -2, 14, 9, -1), GateShapes.box(2, 3, -1, 14, 10, 0));
    private static final VoxelShape EXTENDED_EAST = GateShapes.turn(EXTENDED_NORTH, Direction.EAST);
    private static final VoxelShape EXTENDED_SOUTH = GateShapes.turn(EXTENDED_NORTH, Direction.SOUTH);
    private static final VoxelShape EXTENDED_WEST = GateShapes.turn(EXTENDED_NORTH, Direction.WEST);
    private static final VoxelShape[] X_SHAPES = {GateShapes.turn(BOTTOM, Direction.EAST),
            GateShapes.turn(TOP, Direction.EAST), GateShapes.turn(DOUBLE, Direction.EAST)};
    public AbyssGateSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(EXTENSION, Extension.NONE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(AXIS, EXTENSION);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(this))
            state = state.setValue(AXIS, context.getHorizontalDirection().getAxis());
        return state.setValue(EXTENSION, extension(state, context.getLevel(), context.getClickedPos()));
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
        Extension extension = state.getValue(EXTENSION);
        return extension == Extension.NONE ? rotated
                : rotated.setValue(EXTENSION, Extension.toward(rotation.rotate(extension.direction)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        Extension extension = state.getValue(EXTENSION);
        return extension == Extension.NONE ? state : state.rotate(mirror.getRotation(extension.direction));
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        if (!direction.getAxis().isHorizontal()) return updated;
        Direction.Axis axis = updated.getValue(AXIS);
        if (direction.getAxis() != axis) return updated;
        return updated.setValue(EXTENSION, extension(updated, level, pos, direction, neighbor));
    }
    private static Extension extension(BlockState state, BlockGetter level, BlockPos pos) {
        return extension(state, level, pos, null, null);
    }
    private static Extension extension(BlockState state, BlockGetter level, BlockPos pos,
                                       Direction changed, BlockState neighbor) {
        if (state.getValue(TYPE) != SlabType.BOTTOM) return Extension.NONE;
        Direction first = state.getValue(AXIS) == Direction.Axis.Z ? Direction.NORTH : Direction.WEST;
        for (Direction direction : new Direction[] {first, first.getOpposite()}) {
            BlockState adjacent = direction == changed ? neighbor : level.getBlockState(pos.relative(direction));
            if (adjacent.is(BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get())) return Extension.toward(direction);
        }
        return Extension.NONE;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = switch (state.getValue(TYPE)) { case BOTTOM -> 0; case TOP -> 1; case DOUBLE -> 2; };
        if (index == 0 && state.getValue(EXTENSION).direction != null
                && state.getValue(EXTENSION).direction.getAxis() == state.getValue(AXIS))
            return switch (state.getValue(EXTENSION)) {
                case NORTH -> EXTENDED_NORTH; case EAST -> EXTENDED_EAST;
                case SOUTH -> EXTENDED_SOUTH; case WEST -> EXTENDED_WEST;
                default -> BOTTOM;
            };
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPES[index] : switch (index) {
            case 0 -> BOTTOM; case 1 -> TOP; default -> DOUBLE;
        };
    }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
}
