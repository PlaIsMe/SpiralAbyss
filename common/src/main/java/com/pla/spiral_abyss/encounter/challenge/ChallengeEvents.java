package com.pla.spiral_abyss.encounter.challenge;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.EntityHitResult;

/** Damage hooks collect evidence; FloorController confirms death and evaluates stars. */
public final class ChallengeEvents {
    private ChallengeEvents() {}
    private static ChallengeTracker tracker(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !level.dimension().equals(AbyssDimension.LEVEL)) return null;
        var parties = AbyssParties.get(level.getServer());
        var session = parties.hostedBy(parties.hostOf(mob));
        return session == null ? null : session.controller().challengeTracker();
    }
    public static void damage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(AbyssDimension.LEVEL)) {
            var session = AbyssParties.get(player.server).sessionFor(player);
            if (session != null && session.controller().challengeTracker() != null)
                session.controller().challengeTracker().playerDamaged(player, event);
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) return;
        ChallengeTracker tracker = tracker(mob);
        if (tracker != null) tracker.damaged(mob, event);
    }
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        ChallengeTracker tracker = tracker(mob);
        if (tracker != null) tracker.died(mob, event);
    }
    public static void projectile(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile) || event.getLevel().isClientSide()
                || !event.getLevel().dimension().equals(AbyssDimension.LEVEL)
                || !(projectile.getOwner() instanceof ServerPlayer player)) return;
        var session = AbyssParties.get(player.server).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null)
            session.controller().challengeTracker().projectileLaunched(projectile);
    }
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof FishingHook || event.getProjectile() instanceof ThrownEnderpearl)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Mob mob)) return;
        ChallengeTracker tracker = tracker(mob);
        if (tracker != null) tracker.projectileImpacted(event, mob);
    }
    public static void totem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(AbyssDimension.LEVEL)) return;
        var session = AbyssParties.get(player.server).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null)
            session.controller().challengeTracker().totemUsed(player, event);
    }
    public static void shield(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(AbyssDimension.LEVEL)) return;
        var session = AbyssParties.get(player.server).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null)
            session.controller().challengeTracker().shieldBlocked(player, event);
    }
    public static void equipment(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(AbyssDimension.LEVEL)) return;
        var session = AbyssParties.get(player.server).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null)
            session.controller().challengeTracker().equipmentChanged(player, event.getSlot(), event.getTo());
    }
    public static void useItem(PlayerInteractEvent.RightClickItem event) { use(event); }
    public static void looseArrow(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(AbyssDimension.LEVEL)) return;
        var session = AbyssParties.get(player.server).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null)
            session.controller().challengeTracker().itemUsed(player, event.getBow());
    }
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) { use(event); }
    private static void use(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(AbyssDimension.LEVEL)) return;
        var session = AbyssParties.get(player.server).sessionFor(player);
        if (session != null && session.controller().challengeTracker() != null)
            session.controller().challengeTracker().itemUsed(player, event.getItemStack());
    }
}
