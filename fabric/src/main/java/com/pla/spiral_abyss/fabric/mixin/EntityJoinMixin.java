package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.fabric.FabricSpawnAdmission;
import com.pla.spiral_abyss.platform.event.AbyssEvents.EntityJoinLevelEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
public abstract class EntityJoinMixin {
    @Inject(
            method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void spiralAbyss$join(
            EntityAccess access, boolean existing, CallbackInfoReturnable<Boolean> cir) {
        if (access instanceof FabricSpawnAdmission admission && admission.spiralAbyss$spawnBlocked()) {
            cir.setReturnValue(false);
            return;
        }
        if (access instanceof Entity entity
                && !entity.level().isClientSide
                && FabricGameplayHooks.post(new EntityJoinLevelEvent(entity, entity.level()))
                        .isCanceled()) cir.setReturnValue(false);
    }
}
