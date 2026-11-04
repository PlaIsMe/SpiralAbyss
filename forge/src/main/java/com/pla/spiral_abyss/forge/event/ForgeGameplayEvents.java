package com.pla.spiral_abyss.forge.event;

import com.pla.spiral_abyss.encounter.AbyssEncounters;
import com.pla.spiral_abyss.encounter.AbyssRejection;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.challenge.ChallengeEvents;
import com.pla.spiral_abyss.event.AbyssArchitectureRepair;
import com.pla.spiral_abyss.event.AbyssBlockProtection;
import com.pla.spiral_abyss.event.AbyssMobDrops;
import com.pla.spiral_abyss.event.AbyssNaturalSpawns;
import com.pla.spiral_abyss.event.AbyssPlayerLifecycle;
import com.pla.spiral_abyss.event.AbyssRewardCarriers;
import com.pla.spiral_abyss.event.AbyssVoidMigration;
import com.pla.spiral_abyss.event.BossDeathBlocks;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.BossProgress;

import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "spiral_abyss")
public final class ForgeGameplayEvents {
    @SubscribeEvent
    public static void AbyssEncounters_reload(AddReloadListenerEvent event) {
        var shared = new AbyssEvents.AddReloadListenerEvent(event::addListener);
        AbyssEncounters.reload(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_started(ServerStartedEvent event) {
        var shared = new AbyssEvents.ServerStartedEvent(event.getServer());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssEncounters.started(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_datapacksSynced(OnDatapackSyncEvent event) {
        var shared = new AbyssEvents.OnDatapackSyncEvent(event.getPlayer(), event.getPlayerList());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssEncounters.datapacksSynced(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_tick(TickEvent.ServerTickEvent event) {
        var shared =
                new AbyssEvents.TickEvent.ServerTickEvent(
                        event.getServer(), AbyssEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssEncounters.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_stop(ServerStoppingEvent event) {
        var shared = new AbyssEvents.ServerStoppingEvent(event.getServer());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssEncounters.stop(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AbyssEncounters_preventPlayerDeath(LivingDeathEvent event) {
        var shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssEncounters.preventPlayerDeath(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_isolateDamage(LivingAttackEvent event) {
        var shared =
                new AbyssEvents.LivingAttackEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        AbyssEncounters.isolateDamage(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void AbyssEncounters_scaleDamage(LivingHurtEvent event) {
        var shared =
                new AbyssEvents.LivingHurtEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        AbyssEncounters.scaleDamage(shared);
        if (shared.amount != event.getAmount()) event.setAmount(shared.amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssEncounters_scaleEffectiveHealth(LivingDamageEvent event) {
        var shared =
                new AbyssEvents.LivingDamageEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        AbyssEncounters.scaleEffectiveHealth(shared);
        if (shared.amount != event.getAmount()) event.setAmount(shared.amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssEncounters_scaleEffectiveHealing(LivingHealEvent event) {
        var shared = new AbyssEvents.LivingHealEvent(event.getEntity(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        AbyssEncounters.scaleEffectiveHealing(shared);
        if (shared.amount != event.getAmount()) event.setAmount(shared.amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssRejection_scaleDamage(LivingDamageEvent event) {
        var shared =
                new AbyssEvents.LivingDamageEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        AbyssRejection.scaleDamage(shared);
        if (shared.amount != event.getAmount()) event.setAmount(shared.amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void WaveAllies_join(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        WaveAllies.join(shared);
    }

    @SubscribeEvent
    public static void WaveAllies_leave(EntityLeaveLevelEvent event) {
        var shared = new AbyssEvents.EntityLeaveLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        WaveAllies.leave(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void WaveAllies_target(LivingChangeTargetEvent event) {
        var shared =
                new AbyssEvents.LivingChangeTargetEvent(event.getEntity(), event.getNewTarget());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindNewTarget(event::getNewTarget);
        WaveAllies.target(shared);
        if (shared.newTarget != event.getNewTarget()) event.setNewTarget(shared.newTarget);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void WaveAllies_attack(LivingAttackEvent event) {
        var shared =
                new AbyssEvents.LivingAttackEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        WaveAllies.attack(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void WaveAllies_hurt(LivingHurtEvent event) {
        var shared =
                new AbyssEvents.LivingHurtEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        WaveAllies.hurt(shared);
        if (shared.amount != event.getAmount()) event.setAmount(shared.amount);
    }

    @SubscribeEvent
    public static void WaveAllies_started(ServerStartedEvent event) {
        var shared = new AbyssEvents.ServerStartedEvent(event.getServer());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        WaveAllies.started(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssArchitectureRepair_creativeBreak(BlockEvent.BreakEvent event) {
        var shared =
                new AbyssEvents.BlockEvent.BreakEvent(
                        event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssArchitectureRepair.creativeBreak(shared);
    }

    @SubscribeEvent
    public static void AbyssArchitectureRepair_tick(TickEvent.LevelTickEvent event) {
        var shared =
                new AbyssEvents.TickEvent.LevelTickEvent(
                        event.level, AbyssEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssArchitectureRepair.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssArchitectureRepair_unload(LevelEvent.Unload event) {
        var shared = new AbyssEvents.LevelEvent.Unload(event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssArchitectureRepair.unload(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventDirectMobGriefing(EntityMobGriefingEvent event) {
        var shared = new AbyssEvents.EntityMobGriefingEvent(event.getEntity());
        shared.setResult(AbyssEvents.Event.Result.valueOf(event.getResult().name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preventDirectMobGriefing(shared);
        event.setResult(
                net.minecraftforge.eventbus.api.Event.Result.valueOf(shared.getResult().name()));
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBreaking(BlockEvent.BreakEvent event) {
        var shared =
                new AbyssEvents.BlockEvent.BreakEvent(
                        event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preventBreaking(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_protectBossTerrain(LivingDestroyBlockEvent event) {
        var shared =
                new AbyssEvents.LivingDestroyBlockEvent(
                        event.getEntity(), event.getPos(), event.getState());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.protectBossTerrain(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventPlacement(BlockEvent.EntityPlaceEvent event) {
        var shared =
                new AbyssEvents.BlockEvent.EntityPlaceEvent(
                        event.getLevel(),
                        event.getPos(),
                        event.getPlacedBlock(),
                        event.getEntity());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preventPlacement(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBlockItemUse(
            PlayerInteractEvent.RightClickBlock event) {
        var shared =
                new AbyssEvents.PlayerInteractEvent.RightClickBlock(
                        event.getEntity(),
                        event.getItemStack(),
                        event.getPos(),
                        event.getFace(),
                        null);
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preventBlockItemUse(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBucketUse(
            PlayerInteractEvent.RightClickItem event) {
        var shared =
                new AbyssEvents.PlayerInteractEvent.RightClickItem(
                        event.getEntity(),
                        event.getItemStack(),
                        event.getPos(),
                        event.getFace(),
                        null);
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preventBucketUse(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBucketFill(FillBucketEvent event) {
        var shared = new AbyssEvents.FillBucketEvent(event.getEntity());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preventBucketFill(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preserveArenaDuringExplosion(
            ExplosionEvent.Detonate event) {
        var shared =
                new AbyssEvents.ExplosionEvent.Detonate(
                        event.getLevel(), event.getAffectedBlocks());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssBlockProtection.preserveArenaDuringExplosion(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_auxiliarySpawn(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssMobDrops.auxiliarySpawn(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AbyssMobDrops_auxiliaryInteract(PlayerInteractEvent.EntityInteract event) {
        var shared =
                new AbyssEvents.PlayerInteractEvent.EntityInteract(
                        event.getEntity(),
                        event.getItemStack(),
                        event.getPos(),
                        event.getFace(),
                        event.getTarget());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssMobDrops.auxiliaryInteract(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AbyssMobDrops_auxiliaryInteractAt(
            PlayerInteractEvent.EntityInteractSpecific event) {
        var shared =
                new AbyssEvents.PlayerInteractEvent.EntityInteractSpecific(
                        event.getEntity(),
                        event.getItemStack(),
                        event.getPos(),
                        event.getFace(),
                        event.getTarget());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssMobDrops.auxiliaryInteractAt(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_drops(LivingDropsEvent event) {
        var shared = new AbyssEvents.LivingDropsEvent(event.getEntity(), event.getDrops());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssMobDrops.drops(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_experience(LivingExperienceDropEvent event) {
        var shared =
                new AbyssEvents.LivingExperienceDropEvent(
                        event.getEntity(), event.getDroppedExperience());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindDroppedExperience(event::getDroppedExperience);
        AbyssMobDrops.experience(shared);
        if (shared.droppedExperience != event.getDroppedExperience())
            event.setDroppedExperience(shared.droppedExperience);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_thrownItem(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssMobDrops.thrownItem(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssNaturalSpawns_placement(MobSpawnEvent.SpawnPlacementCheck event) {
        var shared =
                new AbyssEvents.MobSpawnEvent.SpawnPlacementCheck(
                        event.getLevel(), event.getSpawnType());
        shared.setResult(AbyssEvents.Event.Result.valueOf(event.getResult().name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssNaturalSpawns.placement(shared);
        event.setResult(
                net.minecraftforge.eventbus.api.Event.Result.valueOf(shared.getResult().name()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssNaturalSpawns_finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        var shared =
                new AbyssEvents.MobSpawnEvent.FinalizeSpawn(event.getLevel(), event.getSpawnType());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssNaturalSpawns.finalizeSpawn(shared);
        if (shared.isSpawnCancelled()) event.setSpawnCancelled(true);
    }

    @SubscribeEvent
    public static void AbyssPlayerLifecycle_login(PlayerEvent.PlayerLoggedInEvent event) {
        var shared = new AbyssEvents.PlayerEvent.PlayerLoggedInEvent(event.getEntity());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssPlayerLifecycle.login(shared);
    }

    @SubscribeEvent
    public static void AbyssPlayerLifecycle_logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var shared = new AbyssEvents.PlayerEvent.PlayerLoggedOutEvent(event.getEntity());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssPlayerLifecycle.logout(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssRewardCarriers_join(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssRewardCarriers.join(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssRewardCarriers_deathExplosion(ExplosionEvent.Start event) {
        var shared = new AbyssEvents.ExplosionEvent.Start(event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssRewardCarriers.deathExplosion(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AbyssRewardCarriers_tick(LivingEvent.LivingTickEvent event) {
        var shared = new AbyssEvents.LivingEvent.LivingTickEvent(event.getEntity());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssRewardCarriers.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssVoidMigration_load(ChunkEvent.Load event) {
        var shared = new AbyssEvents.ChunkEvent.Load(event.getLevel(), event.getChunk());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssVoidMigration.load(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void BossDeathBlocks_joined(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        BossDeathBlocks.joined(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void BossDeathBlocks_died(LivingDeathEvent event) {
        var shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        BossDeathBlocks.died(shared);
    }

    @SubscribeEvent
    public static void BossDeathBlocks_tick(TickEvent.LevelTickEvent event) {
        var shared =
                new AbyssEvents.TickEvent.LevelTickEvent(
                        event.level, AbyssEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        BossDeathBlocks.tick(shared);
    }

    @SubscribeEvent
    public static void BossDeathBlocks_unload(LevelEvent.Unload event) {
        var shared = new AbyssEvents.LevelEvent.Unload(event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        BossDeathBlocks.unload(shared);
    }

    @SubscribeEvent
    public static void RareRewardBroadcast_pickedUp(PlayerEvent.ItemPickupEvent event) {
        var shared =
                new AbyssEvents.PlayerEvent.ItemPickupEvent(
                        event.getEntity(), event.getOriginalEntity(), event.getStack());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        RareRewardBroadcast.pickedUp(shared);
    }

    @SubscribeEvent
    public static void BossProgress_login(PlayerEvent.PlayerLoggedInEvent event) {
        var shared = new AbyssEvents.PlayerEvent.PlayerLoggedInEvent(event.getEntity());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        BossProgress.login(shared);
    }

    @SubscribeEvent
    public static void BossProgress_stopped(ServerStoppedEvent event) {
        var shared = new AbyssEvents.ServerStoppedEvent(event.getServer());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        BossProgress.stopped(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_damage(LivingDamageEvent event) {
        var shared =
                new AbyssEvents.LivingDamageEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindAmount(event::getAmount);
        ChallengeEvents.damage(shared);
        if (shared.amount != event.getAmount()) event.setAmount(shared.amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_death(LivingDeathEvent event) {
        var shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.death(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_projectile(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.projectile(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_impact(ProjectileImpactEvent event) {
        var shared =
                new AbyssEvents.ProjectileImpactEvent(
                        event.getProjectile(), event.getRayTraceResult());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindImpact(
                () ->
                        AbyssEvents.ProjectileImpactEvent.ImpactResult.valueOf(
                                event.getImpactResult().name()));
        ChallengeEvents.impact(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_totem(LivingUseTotemEvent event) {
        var shared = new AbyssEvents.LivingUseTotemEvent(event.getEntity(), event.getTotem());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.totem(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_shield(ShieldBlockEvent event) {
        var shared =
                new AbyssEvents.ShieldBlockEvent(
                        event.getEntity(), event.getDamageSource(), event.getBlockedDamage());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        shared.bindBlockedDamage(event::getBlockedDamage);
        ChallengeEvents.shield(shared);
        if (shared.blockedDamage != event.getBlockedDamage())
            event.setBlockedDamage(shared.blockedDamage);
    }

    @SubscribeEvent
    public static void ChallengeEvents_equipment(LivingEquipmentChangeEvent event) {
        var shared =
                new AbyssEvents.LivingEquipmentChangeEvent(
                        event.getEntity(), event.getSlot(), event.getTo());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.equipment(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_useItem(PlayerInteractEvent.RightClickItem event) {
        var shared =
                new AbyssEvents.PlayerInteractEvent.RightClickItem(
                        event.getEntity(),
                        event.getItemStack(),
                        event.getPos(),
                        event.getFace(),
                        null);
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.useItem(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_looseArrow(ArrowLooseEvent event) {
        var shared = new AbyssEvents.ArrowLooseEvent(event.getEntity(), event.getBow());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.looseArrow(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_useBlock(PlayerInteractEvent.RightClickBlock event) {
        var shared =
                new AbyssEvents.PlayerInteractEvent.RightClickBlock(
                        event.getEntity(),
                        event.getItemStack(),
                        event.getPos(),
                        event.getFace(),
                        null);
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        ChallengeEvents.useBlock(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }
}
