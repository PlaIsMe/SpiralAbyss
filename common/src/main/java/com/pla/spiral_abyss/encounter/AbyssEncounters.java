package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.world.*;
import net.minecraft.world.entity.Entity;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

public final class AbyssEncounters {
    private AbyssEncounters() {}
    public static void reload(AddReloadListenerEvent event) { FloorRewards.resetMissingTableLogs(); event.addListener(new MobCatalog()); event.addListener(new FloorCatalog()); event.addListener(new BossEnvironmentCatalog()); event.addListener(new ChallengeCatalog()); }
    public static void started(ServerStartedEvent event) {
        FloorRewards.validateTables(event.getServer());
        var level = event.getServer().getLevel(AbyssDimension.LEVEL);
        if (level != null) AbyssParties.configure(level);
        AbyssParties.get(event.getServer()).prepareCycle();
    }
    public static void datapacksSynced(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) FloorRewards.validateTables(event.getPlayerList().getServer());
    }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) AbyssParties.get(event.getServer()).tick();
    }
    public static void stop(ServerStoppingEvent event) { AbyssParties.stop(event.getServer()); }
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
    public static void isolateDamage(LivingAttackEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !level.dimension().equals(AbyssDimension.LEVEL)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        var parties = AbyssParties.get(level.getServer());
        var from = parties.hostOf(attacker);
        var to = parties.hostOf(event.getEntity());
        if (from != null && to != null && !from.equals(to)) event.setCanceled(true);
    }
    public static void scaleDamage(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && AbyssPlatform.data(attacker).hasUUID(WaveSpawner.RUN_TAG)) {
            double scale = AbyssPlatform.data(attacker).getDouble(WaveSpawner.DAMAGE_TAG);
            if (Double.isFinite(scale) && scale > 0) event.setAmount((float) (event.getAmount() * scale));
        }
    }
    // Compatibility for existing saved encounters; new spawns use actual above-cap HP.
    public static void scaleEffectiveHealth(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !AbyssPlatform.data(mob).hasUUID(WaveSpawner.RUN_TAG)) return;
        double ratio = AbyssPlatform.data(mob).getDouble(WaveScaling.HEALTH_DAMAGE_RATIO_TAG);
        if (Double.isFinite(ratio) && ratio > 0 && ratio != 1)
            event.setAmount((float) (event.getAmount() * ratio));
    }
    public static void scaleEffectiveHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !AbyssPlatform.data(mob).hasUUID(WaveSpawner.RUN_TAG)) return;
        double ratio = AbyssPlatform.data(mob).getDouble(WaveScaling.HEALTH_DAMAGE_RATIO_TAG);
        if (Double.isFinite(ratio) && ratio > 0 && ratio != 1)
            event.setAmount((float) (event.getAmount() * ratio));
    }
}
