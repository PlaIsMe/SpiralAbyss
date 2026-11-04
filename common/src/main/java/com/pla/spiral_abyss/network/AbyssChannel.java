package com.pla.spiral_abyss.network;

import dev.architectury.injectables.annotations.ExpectPlatform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.*;

/** Keeps packet schemas and handlers shared while retaining Forge's existing wire channel. */
public abstract class AbyssChannel {
    public enum Direction {
        PLAY_TO_CLIENT,
        PLAY_TO_SERVER
    }

    public record Context(ServerPlayer sender) {
        public ServerPlayer getSender() {
            return sender;
        }
    }

    public record Registration<T>(
            Class<T> type,
            int id,
            Direction direction,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<Context>> handler) {}

    @ExpectPlatform
    public static AbyssChannel create(String version) {
        throw new AssertionError();
    }

    protected abstract <T> void register(Registration<T> registration);

    public abstract void sendToPlayer(ServerPlayer player, Object message);

    public abstract void sendToServer(Object message);

    public abstract void broadcast(Object message);

    public <T> Builder<T> messageBuilder(Class<T> type, int id, Direction direction) {
        return new Builder<>(type, id, direction);
    }

    public final class Builder<T> {
        private final Class<T> type;
        private final int id;
        private final Direction direction;
        private BiConsumer<T, FriendlyByteBuf> encoder;
        private Function<FriendlyByteBuf, T> decoder;
        private BiConsumer<T, Supplier<Context>> handler;

        private Builder(Class<T> type, int id, Direction direction) {
            this.type = type;
            this.id = id;
            this.direction = direction;
        }

        public Builder<T> encoder(BiConsumer<T, FriendlyByteBuf> value) {
            encoder = value;
            return this;
        }

        public Builder<T> decoder(Function<FriendlyByteBuf, T> value) {
            decoder = value;
            return this;
        }

        public Builder<T> consumerMainThread(BiConsumer<T, Supplier<Context>> value) {
            handler = value;
            return this;
        }

        public void add() {
            register(new Registration<>(type, id, direction, encoder, decoder, handler));
        }
    }
}
