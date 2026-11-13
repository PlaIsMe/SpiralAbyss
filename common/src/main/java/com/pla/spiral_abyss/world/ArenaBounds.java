package com.pla.spiral_abyss.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/** Player feet stay above the arena floor; the top of the player stays below world Y=120. */
public record ArenaBounds(Vec3 center) {
    public static final double HALF_SIZE = 30;
    public static final double FLIGHT_RADIUS = 128;
    public static final double FLOOR_Y = 65;
    public static final double CEILING_Y = 120;
    public static final double FLIGHT_CEILING_Y = 240;

    public Vec3 entrance() { return center.add(0, 0, 8); }
    public AABB structureArea() { return new AABB(center.x - 34, 64, center.z - 34, center.x + 35, 121, center.z + 35); }

    public double combatRadius(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !level.dimension().equals(AbyssDimension.LEVEL)) return HALF_SIZE;
        var parties = AbyssParties.get(level.getServer());
        var session = parties.hostedBy(parties.hostOf(entity));
        return session == null || session.bounds().center().distanceToSqr(center) >= 1 ? HALF_SIZE
                : session.controller().bossEnvironments().combatRadius();
    }

    public boolean outsidePlayer(Entity entity) {
        double radius = combatRadius(entity);
        AABB box = entity.getBoundingBox();
        return box.minX < center.x - radius || box.maxX > center.x + radius
                || box.minZ < center.z - radius || box.maxZ > center.z + radius
                || box.minY < feetFloor(entity) || box.maxY > CEILING_Y;
    }

    public boolean rescueMob(Entity entity, boolean ghost) {
        // Above the floor, flight/jump logic can use the larger, bounded flight area.
        if (Math.abs(entity.getX() - center.x) > FLIGHT_RADIUS
                || Math.abs(entity.getZ() - center.z) > FLIGHT_RADIUS
                || entity.getBoundingBox().maxY > (entity instanceof Mob mob
                && ArenaFlight.combatFlyer(mob) ? CEILING_Y : FLIGHT_CEILING_Y)) return true;
        // Ghost types may pursue targets below the arena floor without being returned to center.
        if (ghost && entity.getY() < FLOOR_Y) return false;
        if (entity.getY() < feetFloor(entity)) return true;
        return entity.getY() <= FLOOR_Y + 0.05
                && (Math.abs(entity.getX() - center.x) > combatRadius(entity) - entity.getBbWidth() / 2
                || Math.abs(entity.getZ() - center.z) > combatRadius(entity) - entity.getBbWidth() / 2);
    }
    private double feetFloor(Entity entity) {
        var surface = BlockPos.containing(entity.getX(), FLOOR_Y - 1, entity.getZ());
        return entity.level().getBlockState(surface).is(Blocks.SOUL_SAND) ? FLOOR_Y - 0.125 : FLOOR_Y;
    }
}
