package com.pla.spiral_abyss.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public interface AbyssPayloadRegistrar {
    record Context(Player player) {}

    <T extends CustomPacketPayload> void playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Context> handler);

    <T extends CustomPacketPayload> void playToServer(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Context> handler);
}
