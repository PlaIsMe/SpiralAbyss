package com.pla.spiral_abyss.neoforge.event;

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

import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.*;

@EventBusSubscriber(modid = "spiral_abyss")
public final class NeoForgeGameplayEvents {
    private static void bindCancellation(AbyssEvents.Event shared, Object nativeEvent) {
        if (nativeEvent instanceof net.neoforged.bus.api.ICancellableEvent event)
            shared.bindCancellation(event::isCanceled, event::setCanceled);
    }

    @SubscribeEvent
    public static void AbyssEncounters_reload(AddReloadListenerEvent event) {
        var shared =
                new AbyssEvents.AddReloadListenerEvent(
                        event.getRegistryAccess(), event::addListener);
        AbyssEncounters.reload(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_started(ServerStartedEvent event) {
        var shared = new AbyssEvents.ServerStartedEvent(event.getServer());
        bindCancellation(shared, event);
        AbyssEncounters.started(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_datapacksSynced(OnDatapackSyncEvent event) {
        var shared = new AbyssEvents.OnDatapackSyncEvent(event.getPlayer(), event.getPlayerList());
        bindCancellation(shared, event);
        AbyssEncounters.datapacksSynced(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_tick(ServerTickEvent.Post event) {
        var shared = new AbyssEvents.ServerTickEvent.Post(event.getServer());
        bindCancellation(shared, event);
        AbyssEncounters.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_stop(ServerStoppingEvent event) {
        var shared = new AbyssEvents.ServerStoppingEvent(event.getServer());
        bindCancellation(shared, event);
        AbyssEncounters.stop(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AbyssEncounters_preventPlayerDeath(LivingDeathEvent event) {
        var shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        bindCancellation(shared, event);
        AbyssEncounters.preventPlayerDeath(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounters_isolateDamage(LivingIncomingDamageEvent event) {
        var shared =
                new AbyssEvents.LivingIncomingDamageEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        bindCancellation(shared, event);
        shared.bindAmount(event::getAmount, event::setAmount);
        AbyssEncounters.isolateDamage(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void AbyssEncounters_scaleDamage(LivingIncomingDamageEvent event) {
        var shared =
                new AbyssEvents.LivingIncomingDamageEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        bindCancellation(shared, event);
        shared.bindAmount(event::getAmount, event::setAmount);
        AbyssEncounters.scaleDamage(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssEncounters_scaleEffectiveHealth(LivingDamageEvent.Pre event) {
        var shared =
                new AbyssEvents.LivingDamageEvent.Pre(
                        event.getEntity(), event.getSource(), event.getNewDamage());
        bindCancellation(shared, event);
        shared.bindNewDamage(event::getNewDamage, event::setNewDamage);
        AbyssEncounters.scaleEffectiveHealth(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssEncounters_scaleEffectiveHealing(LivingHealEvent event) {
        var shared = new AbyssEvents.LivingHealEvent(event.getEntity(), event.getAmount());
        bindCancellation(shared, event);
        shared.bindAmount(event::getAmount, event::setAmount);
        AbyssEncounters.scaleEffectiveHealing(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssRejection_scaleDamage(LivingDamageEvent.Pre event) {
        var shared =
                new AbyssEvents.LivingDamageEvent.Pre(
                        event.getEntity(), event.getSource(), event.getNewDamage());
        bindCancellation(shared, event);
        shared.bindNewDamage(event::getNewDamage, event::setNewDamage);
        AbyssRejection.scaleDamage(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void WaveAllies_join(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
        WaveAllies.join(shared);
    }

    @SubscribeEvent
    public static void WaveAllies_leave(EntityLeaveLevelEvent event) {
        var shared = new AbyssEvents.EntityLeaveLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
        WaveAllies.leave(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void WaveAllies_target(LivingChangeTargetEvent event) {
        var shared =
                new AbyssEvents.LivingChangeTargetEvent(
                        event.getEntity(), event.getNewAboutToBeSetTarget());
        bindCancellation(shared, event);
        shared.bindNewAboutToBeSetTarget(
                event::getNewAboutToBeSetTarget, event::setNewAboutToBeSetTarget);
        WaveAllies.target(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void WaveAllies_attack(LivingIncomingDamageEvent event) {
        var shared =
                new AbyssEvents.LivingIncomingDamageEvent(
                        event.getEntity(), event.getSource(), event.getAmount());
        bindCancellation(shared, event);
        shared.bindAmount(event::getAmount, event::setAmount);
        WaveAllies.attack(shared);
    }

    @SubscribeEvent
    public static void WaveAllies_started(ServerStartedEvent event) {
        var shared = new AbyssEvents.ServerStartedEvent(event.getServer());
        bindCancellation(shared, event);
        WaveAllies.started(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssArchitectureRepair_creativeBreak(BlockEvent.BreakEvent event) {
        var shared =
                new AbyssEvents.BlockEvent.BreakEvent(
                        event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        bindCancellation(shared, event);
        AbyssArchitectureRepair.creativeBreak(shared);
    }

    @SubscribeEvent
    public static void AbyssArchitectureRepair_tick(LevelTickEvent.Post event) {
        var shared = new AbyssEvents.LevelTickEvent.Post(event.getLevel());
        bindCancellation(shared, event);
        AbyssArchitectureRepair.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssArchitectureRepair_unload(LevelEvent.Unload event) {
        var shared = new AbyssEvents.LevelEvent.Unload(event.getLevel());
        bindCancellation(shared, event);
        AbyssArchitectureRepair.unload(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventDirectMobGriefing(EntityMobGriefingEvent event) {
        var shared = new AbyssEvents.EntityMobGriefingEvent(event.getEntity(), event.canGrief());
        bindCancellation(shared, event);
        shared.bindCanGrief(event::canGrief, event::setCanGrief);
        AbyssBlockProtection.preventDirectMobGriefing(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preventBreaking(BlockEvent.BreakEvent event) {
        var shared =
                new AbyssEvents.BlockEvent.BreakEvent(
                        event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        bindCancellation(shared, event);
        AbyssBlockProtection.preventBreaking(shared);
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_protectBossTerrain(LivingDestroyBlockEvent event) {
        var shared =
                new AbyssEvents.LivingDestroyBlockEvent(
                        event.getEntity(), event.getPos(), event.getState());
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
        AbyssBlockProtection.preventBucketUse(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent
    public static void AbyssBlockProtection_preserveArenaDuringExplosion(
            ExplosionEvent.Detonate event) {
        var shared =
                new AbyssEvents.ExplosionEvent.Detonate(
                        event.getLevel(), event.getAffectedBlocks());
        bindCancellation(shared, event);
        AbyssBlockProtection.preserveArenaDuringExplosion(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_auxiliarySpawn(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
        AbyssMobDrops.auxiliaryInteractAt(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_drops(LivingDropsEvent event) {
        var shared = new AbyssEvents.LivingDropsEvent(event.getEntity(), event.getDrops());
        bindCancellation(shared, event);
        AbyssMobDrops.drops(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_experience(LivingExperienceDropEvent event) {
        var shared =
                new AbyssEvents.LivingExperienceDropEvent(
                        event.getEntity(), event.getDroppedExperience());
        bindCancellation(shared, event);
        shared.bindDroppedExperience(event::getDroppedExperience, event::setDroppedExperience);
        AbyssMobDrops.experience(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssMobDrops_thrownItem(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
        AbyssMobDrops.thrownItem(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void AbyssNaturalSpawns_placement(MobSpawnEvent.SpawnPlacementCheck event) {
        var shared =
                new AbyssEvents.MobSpawnEvent.SpawnPlacementCheck(
                        event.getLevel(), event.getSpawnType());
        bindCancellation(shared, event);
        shared.setResult(
                AbyssEvents.MobSpawnEvent.SpawnPlacementCheck.Result.valueOf(
                        event.getResult().name()));
        AbyssNaturalSpawns.placement(shared);
        event.setResult(
                MobSpawnEvent.SpawnPlacementCheck.Result.valueOf(shared.getResult().name()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssNaturalSpawns_finalizeSpawn(FinalizeSpawnEvent event) {
        var shared = new AbyssEvents.FinalizeSpawnEvent(event.getLevel(), event.getSpawnType());
        bindCancellation(shared, event);
        AbyssNaturalSpawns.finalizeSpawn(shared);
        if (shared.isSpawnCancelled()) event.setSpawnCancelled(true);
    }

    @SubscribeEvent
    public static void AbyssPlayerLifecycle_login(PlayerEvent.PlayerLoggedInEvent event) {
        var shared = new AbyssEvents.PlayerEvent.PlayerLoggedInEvent(event.getEntity());
        bindCancellation(shared, event);
        AbyssPlayerLifecycle.login(shared);
    }

    @SubscribeEvent
    public static void AbyssPlayerLifecycle_logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var shared = new AbyssEvents.PlayerEvent.PlayerLoggedOutEvent(event.getEntity());
        bindCancellation(shared, event);
        AbyssPlayerLifecycle.logout(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssRewardCarriers_join(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
        AbyssRewardCarriers.join(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void AbyssRewardCarriers_deathExplosion(ExplosionEvent.Start event) {
        var shared = new AbyssEvents.ExplosionEvent.Start(event.getLevel());
        bindCancellation(shared, event);
        AbyssRewardCarriers.deathExplosion(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AbyssRewardCarriers_tick(EntityTickEvent.Pre event) {
        var shared = new AbyssEvents.EntityTickEvent.Pre(event.getEntity());
        bindCancellation(shared, event);
        AbyssRewardCarriers.tick(shared);
    }

    @SubscribeEvent
    public static void AbyssVoidMigration_load(ChunkEvent.Load event) {
        var shared = new AbyssEvents.ChunkEvent.Load(event.getLevel(), event.getChunk());
        bindCancellation(shared, event);
        AbyssVoidMigration.load(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void BossDeathBlocks_joined(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
        BossDeathBlocks.joined(shared);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void BossDeathBlocks_died(LivingDeathEvent event) {
        var shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        bindCancellation(shared, event);
        BossDeathBlocks.died(shared);
    }

    @SubscribeEvent
    public static void BossDeathBlocks_tick(LevelTickEvent.Post event) {
        var shared = new AbyssEvents.LevelTickEvent.Post(event.getLevel());
        bindCancellation(shared, event);
        BossDeathBlocks.tick(shared);
    }

    @SubscribeEvent
    public static void BossDeathBlocks_unload(LevelEvent.Unload event) {
        var shared = new AbyssEvents.LevelEvent.Unload(event.getLevel());
        bindCancellation(shared, event);
        BossDeathBlocks.unload(shared);
    }

    @SubscribeEvent
    public static void RareRewardBroadcast_pickedUp(ItemEntityPickupEvent.Post event) {
        var shared =
                new AbyssEvents.ItemEntityPickupEvent.Post(
                        event.getPlayer(),
                        event.getItemEntity(),
                        event.getOriginalStack(),
                        event.getCurrentStack());
        bindCancellation(shared, event);
        RareRewardBroadcast.pickedUp(shared);
    }

    @SubscribeEvent
    public static void BossProgress_login(PlayerEvent.PlayerLoggedInEvent event) {
        var shared = new AbyssEvents.PlayerEvent.PlayerLoggedInEvent(event.getEntity());
        bindCancellation(shared, event);
        BossProgress.login(shared);
    }

    @SubscribeEvent
    public static void BossProgress_stopped(ServerStoppedEvent event) {
        var shared = new AbyssEvents.ServerStoppedEvent(event.getServer());
        bindCancellation(shared, event);
        BossProgress.stopped(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_damage(LivingDamageEvent.Post event) {
        var shared =
                new AbyssEvents.LivingDamageEvent.Post(
                        event.getEntity(), event.getSource(), event.getNewDamage());
        bindCancellation(shared, event);
        shared.bindNewDamage(event::getNewDamage, null);
        ChallengeEvents.damage(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_death(LivingDeathEvent event) {
        var shared = new AbyssEvents.LivingDeathEvent(event.getEntity(), event.getSource());
        bindCancellation(shared, event);
        ChallengeEvents.death(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_projectile(EntityJoinLevelEvent event) {
        var shared = new AbyssEvents.EntityJoinLevelEvent(event.getEntity(), event.getLevel());
        bindCancellation(shared, event);
        ChallengeEvents.projectile(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_impact(ProjectileImpactEvent event) {
        var shared =
                new AbyssEvents.ProjectileImpactEvent(
                        event.getProjectile(), event.getRayTraceResult());
        bindCancellation(shared, event);
        ChallengeEvents.impact(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_totem(LivingUseTotemEvent event) {
        var shared = new AbyssEvents.LivingUseTotemEvent(event.getEntity(), event.getTotem());
        bindCancellation(shared, event);
        ChallengeEvents.totem(shared);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_shield(LivingShieldBlockEvent event) {
        var shared =
                new AbyssEvents.LivingShieldBlockEvent(
                        event.getEntity(),
                        event.getDamageSource(),
                        event.getBlockedDamage(),
                        event.getBlocked());
        bindCancellation(shared, event);
        shared.bindBlockedDamage(event::getBlockedDamage, event::setBlockedDamage);
        shared.bindBlocked(event::getBlocked, event::setBlocked);
        ChallengeEvents.shield(shared);
    }

    @SubscribeEvent
    public static void ChallengeEvents_equipment(LivingEquipmentChangeEvent event) {
        var shared =
                new AbyssEvents.LivingEquipmentChangeEvent(
                        event.getEntity(), event.getSlot(), event.getTo());
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
        ChallengeEvents.useItem(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ChallengeEvents_looseArrow(ArrowLooseEvent event) {
        var shared = new AbyssEvents.ArrowLooseEvent(event.getEntity(), event.getBow());
        bindCancellation(shared, event);
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
        bindCancellation(shared, event);
        ChallengeEvents.useBlock(shared);
        if (shared.isCanceled()) event.setCancellationResult(shared.getCancellationResult());
    }
}
