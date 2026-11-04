package com.pla.spiral_abyss.fabric;

import com.pla.spiral_abyss.encounter.AbyssEncounters;
import com.pla.spiral_abyss.encounter.AbyssRejection;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.challenge.ChallengeEvents;
import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import com.pla.spiral_abyss.event.AbyssBlockProtection;
import com.pla.spiral_abyss.event.AbyssMobDrops;
import com.pla.spiral_abyss.event.AbyssPlayerLifecycle;
import com.pla.spiral_abyss.event.AbyssRewardCarriers;
import com.pla.spiral_abyss.event.AbyssVoidMigration;
import com.pla.spiral_abyss.event.BossDeathBlocks;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.BossProgress;

public final class FabricGameplayHooks {
    public static <T extends AbyssEvents.Event> T post(T event) {
        if (event instanceof AbyssEvents.LivingDeathEvent e && !e.isCanceled())
            AbyssEncounters.preventPlayerDeath(e);
        if (event instanceof AbyssEvents.LivingAttackEvent e && !e.isCanceled())
            WaveAllies.attack(e);
        if (event instanceof AbyssEvents.LivingHurtEvent e && !e.isCanceled()) WaveAllies.hurt(e);
        if (event instanceof AbyssEvents.PlayerInteractEvent.EntityInteract e && !e.isCanceled())
            AbyssMobDrops.auxiliaryInteract(e);
        if (event instanceof AbyssEvents.PlayerInteractEvent.EntityInteractSpecific e
                && !e.isCanceled()) AbyssMobDrops.auxiliaryInteractAt(e);
        if (event instanceof AbyssEvents.LivingEvent.LivingTickEvent e && !e.isCanceled())
            AbyssRewardCarriers.tick(e);
        if (event instanceof AbyssEvents.LivingDeathEvent e) BossDeathBlocks.died(e);
        if (event instanceof AbyssEvents.ServerStartedEvent e && !e.isCanceled())
            AbyssEncounters.started(e);
        if (event instanceof AbyssEvents.TickEvent.ServerTickEvent e && !e.isCanceled())
            AbyssEncounters.tick(e);
        if (event instanceof AbyssEvents.ServerStoppingEvent e && !e.isCanceled())
            AbyssEncounters.stop(e);
        if (event instanceof AbyssEvents.LivingAttackEvent e && !e.isCanceled())
            AbyssEncounters.isolateDamage(e);
        if (event instanceof AbyssEvents.EntityLeaveLevelEvent e && !e.isCanceled())
            WaveAllies.leave(e);
        if (event instanceof AbyssEvents.ServerStartedEvent e && !e.isCanceled())
            WaveAllies.started(e);
        if (event instanceof AbyssEvents.TickEvent.LevelTickEvent e && !e.isCanceled())
            AbyssArchitectureRepair.tick(e);
        if (event instanceof AbyssEvents.LevelEvent.Unload e && !e.isCanceled())
            AbyssArchitectureRepair.unload(e);
        if (event instanceof AbyssEvents.EntityMobGriefingEvent e && !e.isCanceled())
            AbyssBlockProtection.preventDirectMobGriefing(e);
        if (event instanceof AbyssEvents.BlockEvent.BreakEvent e && !e.isCanceled())
            AbyssBlockProtection.preventBreaking(e);
        if (event instanceof AbyssEvents.LivingDestroyBlockEvent e && !e.isCanceled())
            AbyssBlockProtection.protectBossTerrain(e);
        if (event instanceof AbyssEvents.BlockEvent.EntityPlaceEvent e && !e.isCanceled())
            AbyssBlockProtection.preventPlacement(e);
        if (event instanceof AbyssEvents.PlayerInteractEvent.RightClickBlock e && !e.isCanceled())
            AbyssBlockProtection.preventBlockItemUse(e);
        if (event instanceof AbyssEvents.PlayerInteractEvent.RightClickItem e && !e.isCanceled())
            AbyssBlockProtection.preventBucketUse(e);
        if (event instanceof AbyssEvents.FillBucketEvent e && !e.isCanceled())
            AbyssBlockProtection.preventBucketFill(e);
        if (event instanceof AbyssEvents.ExplosionEvent.Detonate e && !e.isCanceled())
            AbyssBlockProtection.preserveArenaDuringExplosion(e);
        if (event instanceof AbyssEvents.PlayerEvent.PlayerLoggedInEvent e && !e.isCanceled())
            AbyssPlayerLifecycle.login(e);
        if (event instanceof AbyssEvents.PlayerEvent.PlayerLoggedOutEvent e && !e.isCanceled())
            AbyssPlayerLifecycle.logout(e);
        if (event instanceof AbyssEvents.ChunkEvent.Load e && !e.isCanceled())
            AbyssVoidMigration.load(e);
        if (event instanceof AbyssEvents.TickEvent.LevelTickEvent e && !e.isCanceled())
            BossDeathBlocks.tick(e);
        if (event instanceof AbyssEvents.LevelEvent.Unload e && !e.isCanceled())
            BossDeathBlocks.unload(e);
        if (event instanceof AbyssEvents.PlayerEvent.ItemPickupEvent e && !e.isCanceled())
            RareRewardBroadcast.pickedUp(e);
        if (event instanceof AbyssEvents.PlayerEvent.PlayerLoggedInEvent e && !e.isCanceled())
            BossProgress.login(e);
        if (event instanceof AbyssEvents.ServerStoppedEvent e && !e.isCanceled())
            BossProgress.stopped(e);
        if (event instanceof AbyssEvents.LivingEquipmentChangeEvent e && !e.isCanceled())
            ChallengeEvents.equipment(e);
        if (event instanceof AbyssEvents.LivingHurtEvent e && !e.isCanceled())
            AbyssEncounters.scaleDamage(e);
        if (event instanceof AbyssEvents.LivingDamageEvent e && !e.isCanceled())
            AbyssEncounters.scaleEffectiveHealth(e);
        if (event instanceof AbyssEvents.LivingHealEvent e && !e.isCanceled())
            AbyssEncounters.scaleEffectiveHealing(e);
        if (event instanceof AbyssEvents.LivingDamageEvent e && !e.isCanceled())
            AbyssRejection.scaleDamage(e);
        if (event instanceof AbyssEvents.EntityJoinLevelEvent e && !e.isCanceled())
            WaveAllies.join(e);
        if (event instanceof AbyssEvents.LivingChangeTargetEvent e && !e.isCanceled())
            WaveAllies.target(e);
        if (event instanceof AbyssEvents.BlockEvent.BreakEvent e && !e.isCanceled())
            AbyssArchitectureRepair.creativeBreak(e);
        if (event instanceof AbyssEvents.EntityJoinLevelEvent e) AbyssMobDrops.auxiliarySpawn(e);
        if (event instanceof AbyssEvents.LivingDropsEvent e) AbyssMobDrops.drops(e);
        if (event instanceof AbyssEvents.LivingExperienceDropEvent e) AbyssMobDrops.experience(e);
        if (event instanceof AbyssEvents.EntityJoinLevelEvent e) AbyssMobDrops.thrownItem(e);
        if (event instanceof AbyssEvents.EntityJoinLevelEvent e) AbyssRewardCarriers.join(e);
        if (event instanceof AbyssEvents.ExplosionEvent.Start e)
            AbyssRewardCarriers.deathExplosion(e);
        if (event instanceof AbyssEvents.EntityJoinLevelEvent e && !e.isCanceled())
            BossDeathBlocks.joined(e);
        if (event instanceof AbyssEvents.LivingDamageEvent e && !e.isCanceled())
            ChallengeEvents.damage(e);
        if (event instanceof AbyssEvents.LivingDeathEvent e && !e.isCanceled())
            ChallengeEvents.death(e);
        if (event instanceof AbyssEvents.EntityJoinLevelEvent e && !e.isCanceled())
            ChallengeEvents.projectile(e);
        if (event instanceof AbyssEvents.ProjectileImpactEvent e && !e.isCanceled())
            ChallengeEvents.impact(e);
        if (event instanceof AbyssEvents.LivingUseTotemEvent e && !e.isCanceled())
            ChallengeEvents.totem(e);
        if (event instanceof AbyssEvents.ShieldBlockEvent e && !e.isCanceled())
            ChallengeEvents.shield(e);
        if (event instanceof AbyssEvents.PlayerInteractEvent.RightClickItem e && !e.isCanceled())
            ChallengeEvents.useItem(e);
        if (event instanceof AbyssEvents.ArrowLooseEvent e && !e.isCanceled())
            ChallengeEvents.looseArrow(e);
        if (event instanceof AbyssEvents.PlayerInteractEvent.RightClickBlock e && !e.isCanceled())
            ChallengeEvents.useBlock(e);
        return event;
    }
}
