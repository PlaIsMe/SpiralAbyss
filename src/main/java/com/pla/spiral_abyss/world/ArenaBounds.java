package com.pla.spiral_abyss.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Player feet stay above the arena floor; the top of the player stays below world Y=120. */
public record ArenaBounds(Vec3 center) {
    public static final double HALF_SIZE = 30;
    public static final double FLIGHT_RADIUS = 128;
    public static final double FLOOR_Y = 65;
    public static final double CEILING_Y = 120;

    public Vec3 entrance() { return center.add(0, 0, 8); }
    public AABB structureArea() { return new AABB(center.x - 34, 64, center.z - 34, center.x + 35, 121, center.z + 35); }

    public boolean outsidePlayer(Entity entity) {
        AABB box = entity.getBoundingBox();
        return box.minX < center.x - HALF_SIZE || box.maxX > center.x + HALF_SIZE
                || box.minZ < center.z - HALF_SIZE || box.maxZ > center.z + HALF_SIZE
                || box.minY < feetFloor(entity) || box.maxY > CEILING_Y;
    }

    public boolean rescueMob(Entity entity) {
        // Above the floor, flight/jump logic can use the larger, bounded flight area.
        return Math.abs(entity.getX() - center.x) > FLIGHT_RADIUS
                || Math.abs(entity.getZ() - center.z) > FLIGHT_RADIUS || entity.getY() > 240
                || entity.getY() < feetFloor(entity) || (entity.getY() <= FLOOR_Y + 0.05
                && (Math.abs(entity.getX() - center.x) > HALF_SIZE - entity.getBbWidth() / 2
                || Math.abs(entity.getZ() - center.z) > HALF_SIZE - entity.getBbWidth() / 2));
    }
    private double feetFloor(Entity entity) {
        var surface = net.minecraft.core.BlockPos.containing(entity.getX(), FLOOR_Y - 1, entity.getZ());
        return entity.level().getBlockState(surface).is(net.minecraft.world.level.block.Blocks.SOUL_SAND) ? FLOOR_Y - 0.125 : FLOOR_Y;
    }
}
