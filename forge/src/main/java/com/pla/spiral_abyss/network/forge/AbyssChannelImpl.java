package com.pla.spiral_abyss.network.forge;

import com.pla.spiral_abyss.network.AbyssChannel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AbyssChannelImpl extends AbyssChannel {
    private final SimpleChannel channel;

    private AbyssChannelImpl(String version) {
        channel =
                NetworkRegistry.newSimpleChannel(
                        new ResourceLocation("spiral_abyss", "main"),
                        () -> version,
                        version::equals,
                        version::equals);
    }

    public static AbyssChannel create(String version) {
        return new AbyssChannelImpl(version);
    }

    @Override
    protected <T> void register(Registration<T> r) {
        channel.messageBuilder(
                        r.type(),
                        r.id(),
                        r.direction() == Direction.PLAY_TO_CLIENT
                                ? NetworkDirection.PLAY_TO_CLIENT
                                : NetworkDirection.PLAY_TO_SERVER)
                .encoder(r.encoder())
                .decoder(r.decoder())
                .consumerMainThread(
                        (message, context) ->
                                r.handler()
                                        .accept(
                                                message,
                                                () -> new Context(context.get().getSender())))
                .add();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public void sendToServer(Object message) {
        channel.sendToServer(message);
    }

    @Override
    public void broadcast(Object message) {
        channel.send(PacketDistributor.ALL.noArg(), message);
    }
}
