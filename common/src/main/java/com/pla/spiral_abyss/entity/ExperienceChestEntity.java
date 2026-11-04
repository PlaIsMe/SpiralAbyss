package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Endless checkpoint furniture. The server session owns eligibility, XP and advancement. */
public final class ExperienceChestEntity extends LootChestEntity {
    public ExperienceChestEntity(EntityType<? extends Mob> type, Level level) { super(type, level, 0); }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer)
            AbyssParties.get(serverPlayer.server).openExperienceChest(serverPlayer, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
