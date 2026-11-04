package com.pla.spiral_abyss.fabric;

import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

public final class FabricInteractions {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(
                (level, player, pos, state, blockEntity) ->
                        !FabricGameplayHooks.post(
                                        new BlockEvent.BreakEvent(level, pos, state, player))
                                .isCanceled());
        UseBlockCallback.EVENT.register(
                (player, level, hand, hit) -> {
                    var event =
                            FabricGameplayHooks.post(
                                    new PlayerInteractEvent.RightClickBlock(
                                            player,
                                            player.getItemInHand(hand),
                                            hit.getBlockPos(),
                                            hit.getDirection(),
                                            null));
                    return event.isCanceled()
                            ? event.getCancellationResult()
                            : InteractionResult.PASS;
                });
        UseItemCallback.EVENT.register(
                (player, level, hand) -> {
                    var event =
                            FabricGameplayHooks.post(
                                    new PlayerInteractEvent.RightClickItem(
                                            player,
                                            player.getItemInHand(hand),
                                            player.blockPosition(),
                                            null,
                                            null));
                    return new InteractionResultHolder<>(
                            event.isCanceled()
                                    ? event.getCancellationResult()
                                    : InteractionResult.PASS,
                            player.getItemInHand(hand));
                });
        UseEntityCallback.EVENT.register(
                (player, level, hand, entity, hit) -> {
                    var event =
                            FabricGameplayHooks.post(
                                    new PlayerInteractEvent.EntityInteract(
                                            player,
                                            player.getItemInHand(hand),
                                            entity.blockPosition(),
                                            null,
                                            entity));
                    return event.isCanceled()
                            ? event.getCancellationResult()
                            : InteractionResult.PASS;
                });
    }
}
