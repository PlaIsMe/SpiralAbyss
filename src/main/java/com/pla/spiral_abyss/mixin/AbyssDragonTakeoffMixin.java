package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.DragonArena;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(DragonTakeoffPhase.class)
public abstract class AbyssDragonTakeoffMixin {
    @Redirect(method = "findNewTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;findClosestNode(DDD)I"))
    private int spiralAbyss$localTakeoff(EnderDragon dragon, double x, double y, double z) {
        if (!DragonArena.managed(dragon)) return dragon.findClosestNode(x, y, z);
        var origin = dragon.getFightOrigin();
        return dragon.findClosestNode(origin.getX() + x, origin.getY() + 40, origin.getZ() + z);
    }
}
