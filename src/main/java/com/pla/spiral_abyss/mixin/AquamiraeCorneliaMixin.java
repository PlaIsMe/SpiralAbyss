package com.pla.spiral_abyss.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Cornelia's scripted Shipbreaker bypasses ordinary mob and custom-spawner guards. */
@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.entity.CaptainCorneliaBoss", remap = false)
public abstract class AquamiraeCorneliaMixin {
    @Coerce
    @WrapOperation(method = "rage", at = @At(value = "INVOKE", target =
            "Ldev/obscuria/aquamirae/server/event/ShipbreakerEvent;trigger(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)Ldev/obscuria/aquamirae/server/event/ShipbreakerEvent;"))
    private Object spiralAbyss$preventShipbreaker(ServerLevel level, Vec3 position, Operation<Object> original) {
        // rage() discards the return value; other dimensions retain the complete native event.
        return level.dimension().equals(AbyssDimension.LEVEL) ? null : original.call(level, position);
    }
}
