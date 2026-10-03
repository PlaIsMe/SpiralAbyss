package com.pla.spiral_abyss.network;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.UUID;

public final class AbyssNetwork {
    private static final String VERSION = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    public record OpenFloors(UUID token, int claimed, int selectable, int visible, boolean inside,
                             net.minecraft.network.chat.Component hint, long resetRemainingMillis) {}
    public record CloseFloors() {}
    public record SelectFloor(UUID token, int floor) {}
    public record EncounterStatus(boolean active, int floor, int wave, int defeated, int total, int seconds) {
        public static final EncounterStatus HIDDEN = new EncounterStatus(false, 0, 0, 0, 0, 0);
    }
    private AbyssNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(OpenFloors.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeUUID(message.token); buffer.writeVarInt(message.claimed);
                    buffer.writeVarInt(message.selectable); buffer.writeVarInt(message.visible);
                    buffer.writeBoolean(message.inside); buffer.writeComponent(message.hint); buffer.writeLong(message.resetRemainingMillis);
                })
                .decoder(buffer -> new OpenFloors(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readBoolean(), buffer.readComponent(), buffer.readLong()))
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.pla.spiral_abyss.client.screen.FloorSelectionScreen.open(message)))
                .add();
        CHANNEL.messageBuilder(SelectFloor.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> { buffer.writeUUID(message.token); buffer.writeVarInt(message.floor); })
                .decoder(buffer -> new SelectFloor(buffer.readUUID(), buffer.readVarInt()))
                .consumerMainThread((message, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && message.floor >= -1 && message.floor <= 6)
                        AbyssParties.get(sender.server).portals().choose(sender, message.token, message.floor);
                }).add();
        CHANNEL.messageBuilder(EncounterStatus.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeBoolean(message.active); buffer.writeVarInt(message.floor); buffer.writeVarInt(message.wave);
                    buffer.writeVarInt(message.defeated); buffer.writeVarInt(message.total); buffer.writeVarInt(message.seconds);
                })
                .decoder(buffer -> new EncounterStatus(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay.update(message)))
                .add();
        CHANNEL.messageBuilder(CloseFloors.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {})
                .decoder(buffer -> new CloseFloors())
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.pla.spiral_abyss.client.screen.FloorSelectionScreen.closeForReset()))
                .add();
    }
    public static void openFloors(ServerPlayer player, UUID token, int claimed, int selectable, int visible, boolean inside,
                                  net.minecraft.network.chat.Component hint) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenFloors(token, claimed, selectable, visible, inside, hint,
                AbyssParties.get(player.server).resetRemainingMillis()));
    }
    public static void select(UUID token, int floor) { CHANNEL.sendToServer(new SelectFloor(token, floor)); }
    public static void encounterStatus(ServerPlayer player, EncounterStatus status) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), status);
    }
    public static void closeFloors(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseFloors());
    }
}
