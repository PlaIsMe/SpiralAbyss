package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** An immobile interaction point. All floor, wave, timer and reward logic belongs to the dimension. */
public class KeyEntity extends ArenaInteractable {
    public KeyEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        Player player = level().getNearestPlayer(this, 10);
        if (player != null) lookAt(EntityAnchorArgument.Anchor.EYES, player.position());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer && level() instanceof ServerLevel level
                && level.dimension().equals(AbyssDimension.LEVEL)) {
            AbyssParties.get(serverPlayer.server).startKey(serverPlayer, this);
        }
        // Consume even rejected starts so a held draft does not accidentally teleport the player.
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

}
