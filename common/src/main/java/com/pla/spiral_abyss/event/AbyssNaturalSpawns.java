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
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
    public static void finalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().getLevel().dimension().equals(AbyssDimension.LEVEL) && ambient(event.getSpawnType()))
            event.setSpawnCancelled(true);
    }
}
