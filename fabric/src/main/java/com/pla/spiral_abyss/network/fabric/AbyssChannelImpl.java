package com.pla.spiral_abyss.network.fabric;

import com.pla.spiral_abyss.fabric.SpiralAbyssFabric;
import com.pla.spiral_abyss.network.AbyssChannel;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class AbyssChannelImpl extends AbyssChannel {
    private final Map<Class<?>, Registration<?>> registrations = new HashMap<>();
    private final String version;

    private AbyssChannelImpl(String version) {
        this.version = version;
    }

    public static AbyssChannel create(String version) {
        return new AbyssChannelImpl(version);
    }

    private ResourceLocation id(Registration<?> r) {
        return new ResourceLocation("spiral_abyss", "v" + version + "/packet_" + r.id());
    }

    @Override
    protected <T> void register(Registration<T> r) {
        registrations.put(r.type(), r);
        NetworkManager.Side side =
                r.direction() == Direction.PLAY_TO_CLIENT
                        ? NetworkManager.Side.S2C
                        : NetworkManager.Side.C2S;
        if (side == NetworkManager.Side.C2S || Platform.getEnvironment() == Env.CLIENT)
            NetworkManager.registerReceiver(
                    side,
                    id(r),
                    (buffer, context) -> {
                        T message = r.decoder().apply(buffer);
                        context.queue(
                                () ->
                                        r.handler()
                                                .accept(
                                                        message,
                                                        () ->
                                                                new Context(
                                                                        context.getPlayer()
                                                                                        instanceof
                                                                                        ServerPlayer
                                                                                                        player
                                                                                ? player
                                                                                : null)));
                    });
    }

    @SuppressWarnings("unchecked")
    private <T> net.minecraft.network.protocol.Packet<?> packet(T message, Direction direction) {
        Registration<T> r = (Registration<T>) registrations.get(message.getClass());
        if (r == null || r.direction() != direction)
            throw new IllegalArgumentException("Invalid Abyss packet direction");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        r.encoder().accept(message, buffer);
        return NetworkManager.toPacket(
                direction == Direction.PLAY_TO_CLIENT
                        ? NetworkManager.Side.S2C
                        : NetworkManager.Side.C2S,
                id(r),
                buffer);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        player.connection.send(packet(message, Direction.PLAY_TO_CLIENT));
    }

    @Override
    public void sendToServer(Object message) {
        ClientSender.send(packet(message, Direction.PLAY_TO_SERVER));
    }

    @Override
    public void broadcast(Object message) {
        var server = SpiralAbyssFabric.server();
        if (server != null)
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                sendToPlayer(player, message);
    }

    private static final class ClientSender {
        static void send(net.minecraft.network.protocol.Packet<?> packet) {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) connection.send(packet);
        }
    }
}
