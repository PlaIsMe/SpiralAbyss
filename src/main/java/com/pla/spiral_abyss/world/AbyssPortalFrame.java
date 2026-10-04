package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.block.*;
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

/** Five-wide, six-high gate ring, rooted at its bottom-center slab. The axis runs across its width. */
public record AbyssPortalFrame(BlockPos base, Direction front) {
    public static final int TOP = 5;
    public static final int RADIUS = 2;
    public AbyssPortalFrame {
        base = base.immutable();
        if (!front.getAxis().isHorizontal()) throw new IllegalArgumentException("Horizontal portal facing required");
    }
    public Direction.Axis axis() { return front.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X; }
    public BlockPos anchor() { return at(0, TOP).relative(front); }
    public static AbyssPortalFrame fromActivation(BlockPos pos, BlockState state) {
        Direction front = state.getValue(AbyssGateActivationBlock.FACING);
        return new AbyssPortalFrame(pos.relative(front.getOpposite()).below(TOP), front);
    }
    private boolean hasActivator(Level level) {
        BlockState state = level.getBlockState(anchor());
        return state.is(BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get()) && state.getValue(AbyssGateActivationBlock.FACING) == front;
    }

    public BlockPos at(int across, int up) {
        return base.offset(axis() == Direction.Axis.X ? across : 0, up, axis() == Direction.Axis.Z ? across : 0);
    }

    public List<BlockPos> opening() {
        List<BlockPos> result = new ArrayList<>(12);
        for (int y = 1; y < TOP; y++) {
            int width = 1;
            for (int x = -width; x <= width; x++) result.add(at(x, y));
        }
        return result;
    }

    public boolean loaded(Level level) {
        return level.hasChunksAt(at(-RADIUS, 0), at(RADIUS, TOP)) && level.hasChunkAt(anchor());
    }

    public boolean validFrame(Level level) {
        if (!loaded(level)) return false;
        for (int y = 0; y <= TOP; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                BlockState expected = expectedFrameState(x, y);
                if (expected == null) continue;
                BlockState actual = level.getBlockState(at(x, y));
                if (!actual.equals(expected)) return false;
            }
        }
        return true;
    }

    /** Null marks positions outside the frame, including its opening. */
    public BlockState expectedFrameState(int x, int y) {
        if ((y == 0 || y == TOP) && Math.abs(x) <= 1) {
            boolean bottom = (y == 0) == (x == 0);
            return BlockInit.ABYSS_GATE_HORIZONTAL.get().defaultBlockState().setValue(AbyssGateSlabBlock.AXIS, axis())
                    .setValue(SlabBlock.TYPE, bottom ? SlabType.BOTTOM : SlabType.TOP);
        }
        if ((y == 1 || y == TOP - 1) && Math.abs(x) == 2) {
            Direction inward = axis() == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
            if (x < 0) inward = inward.getOpposite();
            return BlockInit.ABYSS_GATE_CURVE.get().defaultBlockState()
                    .setValue(AbyssGateCurveBlock.FACING, inward)
                    .setValue(AbyssGateCurveBlock.HALF, y == 1 ? Half.TOP : Half.BOTTOM);
        }
        if (y >= 2 && y < TOP - 1 && Math.abs(x) == RADIUS) {
            Direction outward = axis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            if (x < 0) outward = outward.getOpposite();
            return BlockInit.ABYSS_GATE_VERTICAL.get().defaultBlockState().setValue(AbyssGateVerticalBlock.FACING, outward);
        }
        return null;
    }

    public boolean active(Level level) {
        return loaded(level) && hasActivator(level) && level.getBlockState(anchor()).getValue(AbyssGateActivationBlock.ACTIVE) && validFrame(level)
                && opening().stream().allMatch(p -> portalCell(level.getBlockState(p)));
    }

    private boolean portalCell(BlockState state) {
        return state.is(BlockInit.ABYSS_PORTAL.get()) && state.getValue(AbyssPortalBlock.AXIS) == axis();
    }

    public boolean activate(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD) || !validFrame(level) || !hasActivator(level)
                || !opening().stream().allMatch(p -> (level.getBlockState(p).isAir() || portalCell(level.getBlockState(p))))
                || !level.getWorldBorder().isWithinBounds(anchor())
                || !level.getWorldBorder().isWithinBounds(at(-RADIUS, 0))
                || !level.getWorldBorder().isWithinBounds(at(RADIUS, TOP))) return false;
        level.setBlock(anchor(), level.getBlockState(anchor()).setValue(AbyssGateActivationBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
        for (BlockPos pos : opening()) level.setBlock(pos, BlockInit.ABYSS_PORTAL.get().defaultBlockState()
                .setValue(AbyssPortalBlock.AXIS, axis()), Block.UPDATE_CLIENTS);
        if (active(level)) return true;
        deactivate(level);
        return false;
    }

    public void deactivate(Level level) {
        clearOpening(level);
        if (level.hasChunkAt(anchor()) && hasActivator(level))
            level.setBlock(anchor(), level.getBlockState(anchor()).setValue(AbyssGateActivationBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
    }

    public void clearOpening(Level level) {
        // Never load chunks to tear down a portal. Its anchor validates again after they load.
        for (BlockPos pos : opening()) {
            if (level.hasChunkAt(pos) && portalCell(level.getBlockState(pos)))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static Optional<AbyssPortalFrame> fromOpening(Level level, BlockPos pos, Direction.Axis axis) {
        for (int y = 1; y < TOP; y++) {
            int width = 1;
            for (int x = -width; x <= width; x++) {
                BlockPos base = pos.offset(axis == Direction.Axis.X ? -x : 0, -y, axis == Direction.Axis.Z ? -x : 0);
                for (Direction front : Direction.Plane.HORIZONTAL) {
                    if (front.getAxis() == axis) continue;
                    var frame = new AbyssPortalFrame(base, front);
                    if (level.hasChunkAt(frame.anchor()) && frame.hasActivator(level)
                            && level.getBlockState(frame.anchor()).getValue(AbyssGateActivationBlock.ACTIVE)) return Optional.of(frame);
                }
            }
        }
        return Optional.empty();
    }

    /** Save a grounded return point beside the ring, rather than inside its teleport surface. */
    public Vec3 returnPosition(ServerPlayer player) {
        Level level = player.level();
        double side = axis() == Direction.Axis.X ? player.getZ() - base.getZ() - 0.5 : player.getX() - base.getX() - 0.5;
        int sign = side < 0 ? -1 : 1;
        for (int facing : new int[] {sign, -sign}) {
            for (int distance = 2; distance <= 4; distance++) {
                BlockPos column = base.offset(axis() == Direction.Axis.Z ? facing * distance : 0, 0,
                        axis() == Direction.Axis.X ? facing * distance : 0);
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
