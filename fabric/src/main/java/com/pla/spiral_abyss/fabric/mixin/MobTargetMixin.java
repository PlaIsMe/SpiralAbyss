package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.LivingChangeTargetEvent;

import net.minecraft.world.entity.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Mob.class)
public abstract class MobTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity spiralAbyss$target(LivingEntity target) {
        return FabricGameplayHooks.post(new LivingChangeTargetEvent((Mob) (Object) this, target))
                .getNewTarget();
    }
}
