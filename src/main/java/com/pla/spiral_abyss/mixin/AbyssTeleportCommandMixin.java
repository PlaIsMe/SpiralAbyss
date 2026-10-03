package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.world.AbyssDimension;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

/** Covers /tp, /teleport, selectors, and execute-in destinations at the actual teleport operation. */
@Mixin(TeleportCommand.class)
public abstract class AbyssTeleportCommandMixin {
    @Inject(method = "performTeleport", at = @At("HEAD"))
    private static void spiralAbyss$blockTeleport(CommandSourceStack source, Entity entity, ServerLevel destination,
            double x, double y, double z, Set<RelativeMovement> relative, float yaw, float pitch,
            @Coerce Object facing, CallbackInfo callback) throws CommandSyntaxException {
        if (entity.level().dimension().equals(AbyssDimension.LEVEL) || destination.dimension().equals(AbyssDimension.LEVEL)
                || (source.getEntity() != null && source.getEntity().level().dimension().equals(AbyssDimension.LEVEL)))
            throw new SimpleCommandExceptionType(Component.translatable("message.spiral_abyss.teleport_blocked")).create();
    }
}
