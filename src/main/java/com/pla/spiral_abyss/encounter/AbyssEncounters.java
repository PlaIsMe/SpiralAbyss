package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssEncounters {
    private AbyssEncounters() {}
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new FloorCatalog()); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var level = event.getServer().getLevel(AbyssDimension.LEVEL);
        if (level != null) AbyssParties.configure(level);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) AbyssParties.get(event.getServer()).tick();
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { AbyssParties.stop(event.getServer()); }
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
}
