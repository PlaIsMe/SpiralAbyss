package com.pla.spiral_abyss.network.fabric;

import com.pla.spiral_abyss.network.AbyssPayloadRegistrar;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.BiConsumer;

public final class AbyssPayloadRegistrarImpl implements AbyssPayloadRegistrar {
    @Override
    public <T extends CustomPacketPayload> void playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Context> handler) {
        if (Platform.getEnvironment() == Env.CLIENT)
            NetworkManager.registerReceiver(
                    NetworkManager.Side.S2C,
                    type,
                    codec,
                    (payload, context) ->
                            context.queue(
                                    () ->
                                            handler.accept(
                                                    payload, new Context(context.getPlayer()))));
        else NetworkManager.registerS2CPayloadType(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void playToServer(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Context> handler) {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                type,
                codec,
                (payload, context) ->
                        context.queue(
                                () -> handler.accept(payload, new Context(context.getPlayer()))));
    }
}
