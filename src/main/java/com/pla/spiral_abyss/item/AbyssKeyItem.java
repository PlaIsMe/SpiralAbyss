package com.pla.spiral_abyss.item;

import com.pla.spiral_abyss.block.AbyssPortalBlock;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.world.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class AbyssKeyItem extends Item {
    public AbyssKeyItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getPlayer() == null || context.getPlayer().isSpectator()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (!level.dimension().equals(Level.OVERWORLD)) return message(player, "portal_overworld_only");
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var found = state.is(BlockInit.ABYSS_PORTAL.get())
                ? AbyssPortalFrame.fromOpening(level, pos, state.getValue(AbyssPortalBlock.AXIS))
                : AbyssPortalFrame.fromBottomSlab(level, pos);
        if (found.isEmpty()) return message(player, "invalid_portal");
        var frame = found.get();
        boolean reset = frame.active(level);
        if (reset) AbyssParties.get(player.server).reset(player);
        else if (!frame.activate((ServerLevel) level)) return message(player, "blocked_portal");
        context.getItemInHand().shrink(1);
        player.getCooldowns().addCooldown(this, 20);
        level.playSound(null, frame.base(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.0F);
        if (!reset) player.sendSystemMessage(Component.translatable("message.spiral_abyss.portal_activated"));
        return InteractionResult.CONSUME;
    }
    private static InteractionResult message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.spiral_abyss." + key), true);
        return InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spiral_abyss.abyss_key.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
