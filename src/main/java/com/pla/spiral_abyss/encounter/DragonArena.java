package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

/** Arena-relative dragon geometry and death policy; no EndDragonFight is created. */
public final class DragonArena {
    public static final String ORIGIN = "spiral_abyss_dragon_origin";
    private DragonArena() {}
    public static boolean abyss(EnderDragon dragon) { return dragon.level().dimension().equals(AbyssDimension.LEVEL); }
    public static boolean managed(EnderDragon dragon) { return abyss(dragon) && dragon.getPersistentData().contains(ORIGIN); }
    /** Only inspect the bounded center column, above the unmodified foundation. */
    public static Vec3 landingTarget(EnderDragon dragon) {
        BlockPos origin = dragon.getFightOrigin();
        double y = origin.getY();
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos pos = origin.above(dy);
            var shape = dragon.level().getBlockState(pos).getCollisionShape(dragon.level(), pos);
            if (!shape.isEmpty()) y = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return new Vec3(origin.getX() + 0.5, y, origin.getZ() + 0.5);
    }
    public static void prepare(Mob mob, Vec3 center) {
        if (!(mob instanceof EnderDragon dragon) || !abyss(dragon)) return;
        BlockPos origin = BlockPos.containing(center);
        dragon.getPersistentData().putLong(ORIGIN, origin.asLong());
        dragon.setFightOrigin(origin);
        dragon.setDragonFight(null);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }
    public static boolean defeated(Mob mob) {
        return mob.isDeadOrDying() || mob.isRemoved() || !mob.isAlive() || mob instanceof EnderDragon dragon && dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
    }
    public static boolean finished(Mob mob, ServerLevel level) {
        if (mob.isRemoved() || mob.level() != level || level.getEntity(mob.getUUID()) != mob) return true;
        // Keep dragons and delayed mutant deaths (including zombie revivals) in the wave
        // until their own lifecycle removes them. Their death events may arrive much later.
        return !MobDeathLifecycle.waitsForRemoval(mob) && (mob.isDeadOrDying() || !mob.isAlive());
    }
    public static BlockPos node(BlockPos origin, int index) {
        if (index < 0 || index >= 24) throw new IllegalArgumentException("Dragon node index must be 0..23");
        int count = index < 12 ? 12 : index < 20 ? 8 : 4;
        int start = index < 12 ? 0 : index < 20 ? 12 : 20;
        int radius = index < 12 ? 60 : index < 20 ? 40 : 20;
        double angle = (index - start) * Math.PI * 2 / count;
        return origin.offset(Mth.floor(radius * Math.cos(angle)), index >= 12 && index < 20 ? 24 : 14, Mth.floor(radius * Math.sin(angle)));
    }
    public static Vec3 approach(BlockPos origin, Vec3 player) {
        Vec3 direction = player == null ? new Vec3(-1, 0, 0) : new Vec3(player.x - origin.getX(), 0, player.z - origin.getZ()).normalize();
        return new Vec3(origin.getX() - direction.x * 40, origin.getY() + 40, origin.getZ() - direction.z * 40);
    }
}
