package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.block.AbyssPortalAnchorBlock;
import com.pla.spiral_abyss.block.AbyssPortalBlock;
import com.pla.spiral_abyss.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Seven-by-seven ring, rooted at its bottom-center slab. The axis runs across its width. */
public record AbyssPortalFrame(BlockPos base, Direction.Axis axis) {
    public AbyssPortalFrame { base = base.immutable(); }

    public BlockPos at(int across, int up) {
        return base.offset(axis == Direction.Axis.X ? across : 0, up, axis == Direction.Axis.Z ? across : 0);
    }

    public List<BlockPos> opening() {
        List<BlockPos> result = new ArrayList<>(21);
        for (int y = 1; y <= 5; y++) {
            int width = y == 1 || y == 5 ? 1 : 2;
            for (int x = -width; x <= width; x++) result.add(at(x, y));
        }
        return result;
    }

    public boolean loaded(Level level) {
        return level.hasChunksAt(at(-3, 0), at(3, 6));
    }

    public boolean validFrame(Level level) {
        if (!loaded(level)) return false;
        for (int y = 0; y <= 6; y++) {
            for (int x = -3; x <= 3; x++) {
                BlockState expected = expectedFrameState(x, y);
                if (expected == null) continue;
                BlockState actual = level.getBlockState(at(x, y));
                if (x == 0 && y == 0 && actual.is(BlockInit.ABYSS_PORTAL_ANCHOR.get())) {
                    if (actual.getValue(AbyssPortalAnchorBlock.AXIS) != axis
                            || actual.getValue(SlabBlock.TYPE) != SlabType.BOTTOM
                            || actual.getValue(SlabBlock.WATERLOGGED)) return false;
                } else if (!actual.equals(expected)) return false;
            }
        }
        return true;
    }

    /** Null marks positions outside the frame, including its opening. */
    public BlockState expectedFrameState(int x, int y) {
        if ((y == 0 || y == 6) && Math.abs(x) <= 1) {
            boolean bottom = (y == 0) == (x == 0);
            return Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, bottom ? SlabType.BOTTOM : SlabType.TOP);
        }
        if ((y == 1 || y == 5) && Math.abs(x) == 2) {
            Direction outward = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            if (x < 0) outward = outward.getOpposite();
            return Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, outward)
                    .setValue(StairBlock.HALF, y == 1 ? Half.BOTTOM : Half.TOP)
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        }
        return y >= 2 && y <= 4 && Math.abs(x) == 3 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : null;
    }

    public boolean active(Level level) {
        return level.getBlockState(base).is(BlockInit.ABYSS_PORTAL_ANCHOR.get()) && validFrame(level)
                && opening().stream().allMatch(p -> portalCell(level.getBlockState(p)));
    }

    private boolean portalCell(BlockState state) {
        return state.is(BlockInit.ABYSS_PORTAL.get()) && state.getValue(AbyssPortalBlock.AXIS) == axis;
    }

    public boolean activate(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD) || !validFrame(level)
                || !opening().stream().allMatch(p -> level.getBlockState(p).isAir())
                || !level.getWorldBorder().isWithinBounds(at(-3, 0))
                || !level.getWorldBorder().isWithinBounds(at(3, 6))) return false;
        level.setBlock(base, BlockInit.ABYSS_PORTAL_ANCHOR.get().defaultBlockState()
                .setValue(AbyssPortalAnchorBlock.AXIS, axis), Block.UPDATE_CLIENTS);
        for (BlockPos pos : opening()) level.setBlock(pos, BlockInit.ABYSS_PORTAL.get().defaultBlockState()
                .setValue(AbyssPortalBlock.AXIS, axis), Block.UPDATE_CLIENTS);
        if (active(level)) return true;
        deactivate(level);
        return false;
    }

    public void deactivate(Level level) {
        clearOpening(level);
        if (level.getBlockState(base).is(BlockInit.ABYSS_PORTAL_ANCHOR.get()))
            level.setBlock(base, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(), Block.UPDATE_ALL);
    }

    public void clearOpening(Level level) {
        // Never load chunks to tear down a portal. Its anchor validates again after they load.
        for (BlockPos pos : opening()) {
            if (level.hasChunkAt(pos) && portalCell(level.getBlockState(pos)))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static Optional<AbyssPortalFrame> fromBottomSlab(Level level, BlockPos pos) {
        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            var frame = new AbyssPortalFrame(pos, axis);
            if (frame.validFrame(level)) return Optional.of(frame);
        }
        return Optional.empty();
    }

    public static Optional<AbyssPortalFrame> fromOpening(Level level, BlockPos pos, Direction.Axis axis) {
        for (int y = 1; y <= 5; y++) {
            int width = y == 1 || y == 5 ? 1 : 2;
            for (int x = -width; x <= width; x++) {
                var frame = new AbyssPortalFrame(pos.offset(axis == Direction.Axis.X ? -x : 0, -y,
                        axis == Direction.Axis.Z ? -x : 0), axis);
                if (!level.hasChunkAt(frame.base)) continue;
                BlockState state = level.getBlockState(frame.base);
                if (state.is(BlockInit.ABYSS_PORTAL_ANCHOR.get()) && state.getValue(AbyssPortalAnchorBlock.AXIS) == axis)
                    return Optional.of(frame);
            }
        }
        return Optional.empty();
    }

    /** Save a grounded return point beside the ring, rather than inside its teleport surface. */
    public Vec3 returnPosition(ServerPlayer player) {
        Level level = player.level();
        double side = axis == Direction.Axis.X ? player.getZ() - base.getZ() - 0.5 : player.getX() - base.getX() - 0.5;
        int sign = side < 0 ? -1 : 1;
        for (int facing : new int[] {sign, -sign}) {
            for (int distance = 2; distance <= 4; distance++) {
                BlockPos column = base.offset(axis == Direction.Axis.Z ? facing * distance : 0, 0,
                        axis == Direction.Axis.X ? facing * distance : 0);
                for (int y = 2; y >= -4; y--) {
                    BlockPos floor = column.above(y);
                    if (!level.hasChunkAt(floor)) continue;
                    var shape = level.getBlockState(floor).getCollisionShape(level, floor);
                    if (shape.isEmpty()) continue;
                    Vec3 candidate = new Vec3(floor.getX() + 0.5, floor.getY() + shape.max(Direction.Axis.Y), floor.getZ() + 0.5);
                    if (level.getWorldBorder().isWithinBounds(BlockPos.containing(candidate))
                            && level.getFluidState(floor).isEmpty()
                            && !level.containsAnyLiquid(player.getBoundingBox().move(candidate.subtract(player.position())))
                            && level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) return candidate;
                }
            }
        }
        return player.position();
    }
}
