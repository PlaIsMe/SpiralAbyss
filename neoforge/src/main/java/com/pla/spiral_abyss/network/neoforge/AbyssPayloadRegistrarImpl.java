package com.pla.spiral_abyss.network.neoforge;

import com.pla.spiral_abyss.network.AbyssPayloadRegistrar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;

public final class AbyssPayloadRegistrarImpl implements AbyssPayloadRegistrar {
    private final PayloadRegistrar registrar;

    public AbyssPayloadRegistrarImpl(PayloadRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public <T extends CustomPacketPayload> void playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Context> handler) {
        registrar.playToClient(
                type,
                codec,
                (payload, context) ->
                        context.enqueueWork(
                                () -> handler.accept(payload, new Context(context.player()))));
    }

    @Override
    public <T extends CustomPacketPayload> void playToServer(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, Context> handler) {
        registrar.playToServer(
                type,
                codec,
                (payload, context) ->
                        context.enqueueWork(
                                () -> handler.accept(payload, new Context(context.player()))));
    }
}
