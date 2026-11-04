package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

/** Announce actual inventory acquisition, once per rare reward entity. */
public final class RareRewardBroadcast {
    private static final String FLOOR = "spiral_abyss_reward_floor";
    private static final String ANNOUNCED = "spiral_abyss_reward_announced";

    private RareRewardBroadcast() {}

    public static void markDrop(ChestLootEntity item, int floor) {
        if (floor >= 1 && floor <= 6) AbyssPlatform.data(item).putInt(FLOOR, floor);
    }

    public static void pickedUp(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getItemEntity() instanceof ChestLootEntity loot)
                || !loot.rare() || event.getOriginalStack().getCount() <= event.getCurrentStack().getCount()) return;
        var metadata = AbyssPlatform.data(loot);
        int floor = metadata.getInt(FLOOR);
        if (floor < 1 || floor > 6 || metadata.getBoolean(ANNOUNCED)) return;
        // Persist consumption even while disabled, so a partial legacy stack cannot announce later.
        metadata.putBoolean(ANNOUNCED, true);
        if (!AbyssConfig.BROADCAST_RARE_LOOT.get()) return;
        var stack = event.getOriginalStack().copy();
        stack.setCount(1);
        var name = player.getName().copy().withStyle(ChatFormatting.AQUA);
        var itemName = Component.empty().append(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD))
                .withStyle(style -> style.withColor(ChatFormatting.GOLD)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack))));
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.spiral_abyss.rare_reward", name, itemName, floor), false);
        AbyssNetwork.broadcastRareReward(name, stack);
    }
}
