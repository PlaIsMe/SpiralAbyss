package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Announce actual inventory acquisition, once per rare reward entity. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class RareRewardBroadcast {
    private static final String FLOOR = "spiral_abyss_reward_floor";
    private static final String ANNOUNCED = "spiral_abyss_reward_announced";

    private RareRewardBroadcast() {}

    public static void markDrop(ChestLootEntity item, int floor) {
        if (floor >= 1 && floor <= 6) item.getPersistentData().putInt(FLOOR, floor);
    }

    @SubscribeEvent
    public static void pickedUp(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getOriginalEntity() instanceof ChestLootEntity loot)
                || !loot.rare() || event.getStack().isEmpty()) return;
        var metadata = loot.getPersistentData();
        int floor = metadata.getInt(FLOOR);
        if (floor < 1 || floor > 6 || metadata.getBoolean(ANNOUNCED)) return;
        // Persist consumption even while disabled, so a partial legacy stack cannot announce later.
        metadata.putBoolean(ANNOUNCED, true);
        if (!AbyssConfig.BROADCAST_RARE_LOOT.get()) return;
        var stack = event.getStack().copy();
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
