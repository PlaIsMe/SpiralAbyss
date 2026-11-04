package com.pla.spiral_abyss.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.world.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

/** The outward-facing controller between the two top horizontal gate slabs. */
public final class AbyssGateActivationBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<AbyssGateActivationBlock> CODEC = simpleCodec(AbyssGateActivationBlock::new);
    @Override public MapCodec<AbyssGateActivationBlock> codec() { return CODEC; }
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) SHAPES[facing.get2DDataValue()] = GateShapes.turn(GateShapes.ACTIVATION, facing);
    }
    public AbyssGateActivationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The supplied decorated face is south, so rotate it toward the placer.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AbyssPortalBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == BlockEntityInit.ABYSS_PORTAL.get()
                ? (world, pos, current, entity) -> AbyssPortalBlockEntity.tick(world, pos, current) : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && AbyssPortalFrame.fromActivation(pos, state).activate(server)) {
            level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1F);
            if (placer instanceof ServerPlayer player)
                player.sendSystemMessage(Component.translatable("message.spiral_abyss.portal_activated"));
        }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && (!state.is(replacement.getBlock()) || state.getValue(FACING) != replacement.getValue(FACING)))
            AbyssPortalFrame.fromActivation(pos, state).clearOpening(level);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }
    @Override public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1F; }
}
