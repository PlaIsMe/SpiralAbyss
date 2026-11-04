package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

/** Per-dragon encounter pacing. Vanilla phases still own movement, attacks and their cleanup. */
public final class DragonCombat {
    private EnderDragonPhase<?> previous;
    private int phaseTicks, flightTicks, nextAttack;

    public static ServerPlayer target(EnderDragon dragon) {
        if (!DragonArena.managed(dragon) || dragon.level().isClientSide
                || !(dragon.getTarget() instanceof ServerPlayer player) || !player.isAlive()
                || player.isSpectator() || player.isCreative() || player.level() != dragon.level()
                || !AbyssPlatform.data(dragon).hasUUID("spiral_abyss_host")) return null;
        var session = AbyssParties.get(player.server).hostedBy(AbyssPlatform.data(dragon).getUUID("spiral_abyss_host"));
        return session != null && session.isCombatant(player) && !session.bounds().outsidePlayer(player) ? player : null;
    }

    public void tick(EnderDragon dragon) {
        if (!DragonArena.managed(dragon) || dragon.level().isClientSide || dragon.isNoAi() || DragonArena.defeated(dragon)) return;
        var manager = dragon.getPhaseManager();
        var phase = manager.getCurrentPhase().getPhase();
        if (phase != previous) { previous = phase; phaseTicks = 0; }
        phaseTicks++;
        var player = target(dragon);
        if (phase == EnderDragonPhase.HOLDING_PATTERN || phase == EnderDragonPhase.STRAFE_PLAYER) {
            if (player == null) {
                flightTicks = 0;
                if (phase == EnderDragonPhase.STRAFE_PLAYER) manager.setPhase(EnderDragonPhase.HOLDING_PATTERN);
                return;
            }
            // A strafe can otherwise circle forever when it cannot line up a fireball.
            if (++flightTicks < 200) return;
            flightTicks = 0;
            switch (nextAttack++ % 3) {
                case 0 -> {
                    manager.setPhase(EnderDragonPhase.CHARGING_PLAYER);
                    manager.getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(player.position());
                }
                case 1 -> manager.setPhase(EnderDragonPhase.LANDING_APPROACH);
                default -> {
                    manager.setPhase(EnderDragonPhase.STRAFE_PLAYER);
                    manager.getPhase(EnderDragonPhase.STRAFE_PLAYER).setTarget(player);
                }
            }
        } else {
            flightTicks = 0;
            if (phase == EnderDragonPhase.SITTING_SCANNING) {
                if (player != null) {
                    double dx = player.getX() - dragon.getX(), dz = player.getZ() - dragon.getZ();
                    float yaw = (float) (180 - Mth.atan2(dx, dz) * 180 / Math.PI);
                    dragon.setYRot(Mth.approachDegrees(dragon.getYRot(), yaw, 6));
                    if (phaseTicks >= 30) manager.setPhase(EnderDragonPhase.SITTING_ATTACKING);
                } else if (phaseTicks >= 100) manager.setPhase(EnderDragonPhase.TAKEOFF);
            } else if (phase == EnderDragonPhase.SITTING_FLAMING && phaseTicks >= 160) {
                manager.setPhase(EnderDragonPhase.TAKEOFF);
            } else if (phase == EnderDragonPhase.CHARGING_PLAYER && (player == null || phaseTicks >= 100)
                    || phase == EnderDragonPhase.TAKEOFF && phaseTicks >= 100
                    || phase == EnderDragonPhase.LANDING && phaseTicks >= 240) {
                manager.setPhase(EnderDragonPhase.HOLDING_PATTERN);
            } else if (phase == EnderDragonPhase.LANDING_APPROACH && phaseTicks >= 200) {
                manager.setPhase(EnderDragonPhase.LANDING);
            }
        }
    }
}
