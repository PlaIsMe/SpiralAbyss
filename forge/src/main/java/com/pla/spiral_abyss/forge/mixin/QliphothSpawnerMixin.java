package com.pla.spiral_abyss.forge.mixin;

import com.pla.spiral_abyss.encounter.QliphothEncounters;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An arena spawner supplies native context; only the Abyss controller may start a fight. */
@Pseudo
@Mixin(targets = "com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity", remap = false)
public abstract class QliphothSpawnerMixin {
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$controllerOwnsSpawn(CallbackInfo ci) {
        if (QliphothEncounters.helper((Entity) (Object) this)) ci.cancel();
    }
    @Inject(method = "setActive", at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$keepArenaSpawnerInactive(boolean active, CallbackInfo ci) {
        if (active && QliphothEncounters.helper((Entity) (Object) this)) ci.cancel();
    }
}
