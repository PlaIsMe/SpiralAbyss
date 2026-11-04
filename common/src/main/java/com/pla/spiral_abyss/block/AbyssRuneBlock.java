package com.pla.spiral_abyss.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/** A horizontal pillar whose rune face points toward the player when placed. */
public final class AbyssRuneBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AbyssRuneBlock> CODEC = simpleCodec(AbyssRuneBlock::new);
    @Override public MapCodec<AbyssRuneBlock> codec() { return CODEC; }
    public AbyssRuneBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
