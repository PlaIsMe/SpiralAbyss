package com.pla.spiral_abyss.block;

import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import org.jetbrains.annotations.Nullable;

/** The activated bottom slab retains the original slab's appearance and collision. */
public final class AbyssPortalAnchorBlock extends SlabBlock implements EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public AbyssPortalAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(AXIS);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbyssPortalBlockEntity(pos, state);
    }
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == BlockEntityInit.ABYSS_PORTAL.get()
                ? (world, pos, block, entity) -> AbyssPortalBlockEntity.tick(world, pos, block) : null;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide)
            new AbyssPortalFrame(pos, state.getValue(AXIS)).clearOpening(level);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
