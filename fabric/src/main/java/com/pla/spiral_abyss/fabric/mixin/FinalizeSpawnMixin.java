package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.event.AbyssNaturalSpawns;
import com.pla.spiral_abyss.fabric.FabricSpawnAdmission;
import com.pla.spiral_abyss.platform.event.AbyssEvents.FinalizeSpawnEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers ambient spawn paths that finalize without checking SpawnPlacements. */
@Mixin(Mob.class)
public abstract class FinalizeSpawnMixin implements FabricSpawnAdmission {
    @Unique private boolean spiralAbyss$spawnBlocked;
    @Override public boolean spiralAbyss$spawnBlocked() { return spiralAbyss$spawnBlocked; }
    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void spiralAbyss$finalize(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData group, CallbackInfoReturnable<SpawnGroupData> cir) {
        var event = new FinalizeSpawnEvent(level, reason);
        AbyssNaturalSpawns.finalizeSpawn(event);
        spiralAbyss$spawnBlocked = event.isSpawnCancelled();
    }
}
