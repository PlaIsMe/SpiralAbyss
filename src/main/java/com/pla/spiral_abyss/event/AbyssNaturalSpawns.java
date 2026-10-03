package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssNaturalSpawns {
    private AbyssNaturalSpawns() {}
    private static boolean ambient(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel().getLevel().dimension().equals(AbyssDimension.LEVEL) && ambient(event.getSpawnType()))
            event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel().getLevel().dimension().equals(AbyssDimension.LEVEL) && ambient(event.getSpawnType()))
            event.setSpawnCancelled(true);
    }
}
