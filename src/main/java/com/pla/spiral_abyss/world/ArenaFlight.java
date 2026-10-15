package com.pla.spiral_abyss.world;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.phys.Vec3;

/** Keeps vanilla free-flight AI in reach of the fight, independently of path navigation. */
public final class ArenaFlight {
    private ArenaFlight() {}

    public static boolean combatFlyer(Mob mob) {
        return mob instanceof WitherBoss || mob instanceof Ghast;
    }

    public static void steer(Mob mob, ArenaBounds bounds) {
        if (!mob.level().dimension().equals(AbyssDimension.LEVEL) || !combatFlyer(mob)) return;
        double top = ArenaBounds.CEILING_Y - mob.getBbHeight();
        var target = mob.getTarget();
        // Ghasts only acquire players within four vertical blocks; idle flight must stay in that band.
        double offset = mob instanceof Ghast ? 3 : 5;
        double hover = Mth.clamp(target != null && target.isAlive() && !target.isSpectator()
                ? target.getY() + offset : ArenaBounds.FLOOR_Y + (mob instanceof Ghast ? 3 : 8),
                ArenaBounds.FLOOR_Y + offset, top);
        var control = mob.getMoveControl();
        if (control.hasWanted() && (control.getWantedY() > hover || control.getWantedY() < ArenaBounds.FLOOR_Y
                || Math.abs(control.getWantedX() - bounds.center().x) > ArenaBounds.HALF_SIZE
                || Math.abs(control.getWantedZ() - bounds.center().z) > ArenaBounds.HALF_SIZE)) {
            mob.getNavigation().stop();
            control.setWantedPosition(Mth.clamp(control.getWantedX(), bounds.center().x - 24, bounds.center().x + 24),
                    hover, Mth.clamp(control.getWantedZ(), bounds.center().z - 24, bounds.center().z + 24), 1);
        }
        // Withers add vertical motion directly in aiStep; changing their path alone cannot stop ascent.
        if (mob.getY() > hover) {
            Vec3 velocity = mob.getDeltaMovement();
            mob.setDeltaMovement(velocity.x, Math.min(velocity.y, -0.3), velocity.z);
            mob.hasImpulse = true;
        }
    }

    public static void stop(Mob mob) {
        mob.getNavigation().stop();
        // GhastMoveControl retains its wanted position even after navigation.stop().
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0;
        mob.hasImpulse = true;
    }
}
