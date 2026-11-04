package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.MobSpawnType;

public final class AbyssNaturalSpawns {
    private AbyssNaturalSpawns() {}
    private static boolean ambient(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL;
    }
    public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel().getLevel().dimension().equals(AbyssDimension.LEVEL) && ambient(event.getSpawnType()))
            event.setResult(Event.Result.DENY);
    }
    public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel().getLevel().dimension().equals(AbyssDimension.LEVEL) && ambient(event.getSpawnType()))
            event.setSpawnCancelled(true);
    }
}
