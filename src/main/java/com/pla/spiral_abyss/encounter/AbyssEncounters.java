package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.compat.FlashbackCompat;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.world.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

@EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssEncounters {
    private AbyssEncounters() {}
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { FloorRewards.resetMissingTableLogs(); event.addListener(new MobCatalog()); event.addListener(new FloorCatalog()); event.addListener(new BossEnvironmentCatalog()); event.addListener(new ChallengeCatalog(event.getRegistryAccess())); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (FlashbackCompat.isReplayServer(event.getServer())) return;
        FloorRewards.validateTables(event.getServer());
        var level = event.getServer().getLevel(AbyssDimension.LEVEL);
        if (level != null) AbyssParties.configure(level);
        AbyssParties.get(event.getServer()).prepareCycle();
    }
    @SubscribeEvent public static void datapacksSynced(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) FloorRewards.validateTables(event.getPlayerList().getServer());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (FlashbackCompat.isReplayServer(event.getServer())) return;
        AbyssParties.get(event.getServer()).tick();
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { AbyssParties.stop(event.getServer()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().equals(AbyssDimension.LEVEL)) return;
        event.setCanceled(true);
        var parties = AbyssParties.get(player.server);
        var session = parties.sessionFor(player);
        if (session == null) {
            // Cover command-based arrivals before their first arena tick as well.
            player.setHealth(player.getMaxHealth());
            parties.enterOwn(player); session = parties.sessionFor(player);
        }
        if (session != null) session.defeat(player);
        else { ArenaSession.restoreSurvival(player); parties.leave(player); }
    }
    @SubscribeEvent public static void isolateDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !level.dimension().equals(AbyssDimension.LEVEL)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        var parties = AbyssParties.get(level.getServer());
        var from = parties.hostOf(attacker);
        var to = parties.hostOf(event.getEntity());
        if (from != null && to != null && !from.equals(to)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void scaleDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)) {
            double scale = attacker.getPersistentData().getDouble(WaveSpawner.DAMAGE_TAG);
            if (Double.isFinite(scale) && scale > 0) event.setAmount((float) (event.getAmount() * scale));
        }
    }
    // Compatibility for existing saved encounters; new spawns use actual above-cap HP.
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void scaleEffectiveHealth(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !mob.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)) return;
        double ratio = mob.getPersistentData().getDouble(WaveScaling.HEALTH_DAMAGE_RATIO_TAG);
        if (Double.isFinite(ratio) && ratio > 0 && ratio != 1)
            event.setNewDamage((float) (event.getNewDamage() * ratio));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void scaleEffectiveHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !mob.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)) return;
        double ratio = mob.getPersistentData().getDouble(WaveScaling.HEALTH_DAMAGE_RATIO_TAG);
        if (Double.isFinite(ratio) && ratio > 0 && ratio != 1)
            event.setAmount((float) (event.getAmount() * ratio));
    }
}
