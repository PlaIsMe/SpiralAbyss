package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssEncounters {
    private AbyssEncounters() {}
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { FloorRewards.resetMissingTableLogs(); event.addListener(new com.pla.spiral_abyss.encounter.data.MobCatalog()); event.addListener(new FloorCatalog()); event.addListener(new com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog()); event.addListener(new com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog()); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        FloorRewards.validateTables(event.getServer());
        var level = event.getServer().getLevel(AbyssDimension.LEVEL);
        if (level != null) AbyssParties.configure(level);
        AbyssParties.get(event.getServer()).prepareCycle();
    }
    @SubscribeEvent public static void datapacksSynced(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) FloorRewards.validateTables(event.getPlayerList().getServer());
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) AbyssParties.get(event.getServer()).tick();
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { AbyssParties.stop(event.getServer()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
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
    @SubscribeEvent public static void isolateDamage(LivingAttackEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)
                || !level.dimension().equals(AbyssDimension.LEVEL)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        var parties = AbyssParties.get(level.getServer());
        var from = parties.hostOf(attacker);
        var to = parties.hostOf(event.getEntity());
        if (from != null && to != null && !from.equals(to)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void scaleDamage(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)) {
            double scale = attacker.getPersistentData().getDouble(WaveSpawner.DAMAGE_TAG);
            if (Double.isFinite(scale) && scale > 0) event.setAmount((float) (event.getAmount() * scale));
        }
    }
    // Compatibility for existing saved encounters; new spawns use actual above-cap HP.
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void scaleEffectiveHealth(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)
                || !mob.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)) return;
        double ratio = mob.getPersistentData().getDouble(WaveScaling.HEALTH_DAMAGE_RATIO_TAG);
        if (Double.isFinite(ratio) && ratio > 0 && ratio != 1)
            event.setAmount((float) (event.getAmount() * ratio));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void scaleEffectiveHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)
                || !mob.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)) return;
        double ratio = mob.getPersistentData().getDouble(WaveScaling.HEALTH_DAMAGE_RATIO_TAG);
        if (Double.isFinite(ratio) && ratio > 0 && ratio != 1)
            event.setAmount((float) (event.getAmount() * ratio));
    }
}
