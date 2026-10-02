package com.pla.spiral_abyss.item;

import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class AbyssDraftItem extends Item {
    public AbyssDraftItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer) || !travel(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS
                : use(context.getLevel(), player, context.getHand()).getResult();
    }

    private boolean travel(ServerPlayer player) {
        var parties = AbyssParties.get(player.server);
        return player.level().dimension().equals(AbyssDimension.LEVEL) ? parties.leave(player) : parties.enterOwn(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spiral_abyss.abyss_draft.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
