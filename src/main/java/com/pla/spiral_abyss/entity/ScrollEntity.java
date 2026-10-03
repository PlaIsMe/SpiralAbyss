package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ScrollEntity extends ArenaInteractable {
    public ScrollEntity(EntityType<? extends Mob> type, Level level) { super(type, level); }
    @Override public void tick() {
        super.tick();
        Player nearest = level().getNearestPlayer(this, 10);
        if (nearest != null) lookAt(EntityAnchorArgument.Anchor.EYES, nearest.position());
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) AbyssParties.get(serverPlayer.server).openScroll(serverPlayer, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
