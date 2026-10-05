package com.pla.spiral_abyss.network;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;

public final class AbyssNetwork {
    private static final String VERSION = "9";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    public record OpenFloors(UUID token, int claimed, int selectable, int visible, boolean inside,
                             long resetRemainingMillis, List<FloorPreview> floors) {
        public OpenFloors { floors = List.copyOf(floors); }
    }
    public record EnemyPreview(ResourceLocation id, int count) {}
    public record WavePreview(int wave, int stars, List<EnemyPreview> enemies, List<net.minecraft.network.chat.Component> challenges) {
        public WavePreview { enemies = List.copyOf(enemies); challenges = List.copyOf(challenges); }
    }
    public record FloorPreview(int floor, List<WavePreview> waves) {
        public FloorPreview { waves = List.copyOf(waves); }
    }
    public record CloseFloors() {}
    public record SelectFloor(UUID token, int floor) {}
    public record KeepFloorsOpen(UUID token) {}
    public record ChallengeView(net.minecraft.network.chat.Component description, int progress, int target, boolean satisfied, boolean failed) {}
    public record EncounterStatus(boolean active, int floor, int wave, int defeated, int total, int seconds, int countdownSeconds, List<ChallengeView> challenges) {
        public EncounterStatus { challenges = List.copyOf(challenges); }
        public static final EncounterStatus HIDDEN = new EncounterStatus(false, 0, 0, 0, 0, 0, 0, List.of());
    }
    private AbyssNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(OpenFloors.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeUUID(message.token); buffer.writeVarInt(message.claimed);
                    buffer.writeVarInt(message.selectable); buffer.writeVarInt(message.visible);
                    buffer.writeBoolean(message.inside); buffer.writeLong(message.resetRemainingMillis);
                    writePreviews(buffer, message.floors);
                })
                .decoder(buffer -> new OpenFloors(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readBoolean(), buffer.readLong(), readPreviews(buffer)))
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
                    buffer.writeVarInt(message.defeated); buffer.writeVarInt(message.total); buffer.writeVarInt(message.seconds); buffer.writeVarInt(message.countdownSeconds);
                    writeChallenges(buffer, message.challenges);
                })
                .decoder(buffer -> new EncounterStatus(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), readChallenges(buffer)))
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay.update(message)))
                .add();
        CHANNEL.messageBuilder(CloseFloors.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {})
                .decoder(buffer -> new CloseFloors())
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.pla.spiral_abyss.client.screen.FloorSelectionScreen.closeForReset()))
                .add();
        CHANNEL.messageBuilder(KeepFloorsOpen.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeUUID(message.token))
                .decoder(buffer -> new KeepFloorsOpen(buffer.readUUID()))
                .consumerMainThread((message, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null) AbyssParties.get(sender.server).portals().keepOpen(sender, message.token);
                }).add();
    }
    public static void openFloors(ServerPlayer player, UUID token, int claimed, int selectable, int visible, boolean inside) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenFloors(token, claimed, selectable, visible, inside,
                AbyssParties.get(player.server).resetRemainingMillis(), previews(player)));
    }
    /** Preview uses the same persisted cycle lineups as spawning; the client never rolls mobs. */
    private static List<FloorPreview> previews(ServerPlayer player) {
        var parties = AbyssParties.get(player.server);
        var progress = parties.profile(player.getUUID()).progress;
        List<FloorPreview> floors = new ArrayList<>();
        for (var definition : new TreeMap<>(com.pla.spiral_abyss.encounter.data.FloorCatalog.snapshot().floors()).values()) {
            List<WavePreview> waves = new ArrayList<>();
            List<com.pla.spiral_abyss.encounter.challenge.Challenge> challenges;
            try { challenges = parties.challenges(definition.number()); }
            catch (IllegalArgumentException ex) { challenges = List.of(); }
            int[] stars = progress.waveStars(definition.number());
            for (int index = 0; index < definition.waves().size(); index++) {
                Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
                try {
                    parties.cycleLineup(definition.number(), index + 1, definition.waves().get(index))
                            .forEach(enemy -> counts.merge(enemy.id(), enemy.count(), Integer::sum));
                } catch (IllegalArgumentException ex) {
                    com.mojang.logging.LogUtils.getLogger().warn("Cannot preview Abyss floor {} wave {}: {}", definition.number(), index + 1, ex.getMessage());
                }
                int wavePopulation = counts.values().stream().mapToInt(Integer::intValue).sum();
                List<net.minecraft.network.chat.Component> objectives = challenges.isEmpty()
                        ? List.of(net.minecraft.network.chat.Component.translatable("message.spiral_abyss.challenges_unavailable"))
                        : challenges.stream().map(challenge -> challenge.description(wavePopulation)).toList();
                waves.add(new WavePreview(index + 1, stars[index], counts.entrySet().stream()
                        .map(entry -> new EnemyPreview(entry.getKey(), entry.getValue())).toList(), objectives));
            }
            floors.add(new FloorPreview(definition.number(), waves));
        }
        return floors;
    }
    private static void writePreviews(FriendlyByteBuf buffer, List<FloorPreview> floors) {
        buffer.writeVarInt(floors.size());
        for (var floor : floors) {
            buffer.writeVarInt(floor.floor()); buffer.writeVarInt(floor.waves().size());
            for (var wave : floor.waves()) {
                buffer.writeVarInt(wave.wave()); buffer.writeVarInt(wave.stars());
                buffer.writeVarInt(wave.challenges().size()); wave.challenges().forEach(buffer::writeComponent);
                buffer.writeVarInt(wave.enemies().size());
                for (var enemy : wave.enemies()) { buffer.writeResourceLocation(enemy.id()); buffer.writeVarInt(enemy.count()); }
            }
        }
    }
    private static void writeChallenges(FriendlyByteBuf buffer, List<ChallengeView> challenges) {
        buffer.writeVarInt(challenges.size());
        for (var challenge : challenges) {
            buffer.writeComponent(challenge.description());
            buffer.writeVarInt(challenge.progress()); buffer.writeVarInt(challenge.target());
            buffer.writeBoolean(challenge.satisfied()); buffer.writeBoolean(challenge.failed());
        }
    }
    private static List<ChallengeView> readChallenges(FriendlyByteBuf buffer) {
        List<ChallengeView> result = new ArrayList<>();
        for (int c = bounded(buffer, 3); c > 0; c--) result.add(new ChallengeView(buffer.readComponent(),
                bounded(buffer, 1_000_000), bounded(buffer, 1_000_000), buffer.readBoolean(), buffer.readBoolean()));
        return result;
    }
    private static int bounded(FriendlyByteBuf buffer, int max) {
        int number = buffer.readVarInt();
        if (number < 0 || number > max) throw new IllegalArgumentException("Invalid Abyss preview size");
        return number;
    }
    private static List<FloorPreview> readPreviews(FriendlyByteBuf buffer) {
        List<FloorPreview> floors = new ArrayList<>();
        for (int f = bounded(buffer, 6); f > 0; f--) {
            int floor = bounded(buffer, 6);
            List<WavePreview> waves = new ArrayList<>();
            for (int w = bounded(buffer, 3); w > 0; w--) {
                int wave = bounded(buffer, 3), stars = bounded(buffer, 3);
                List<net.minecraft.network.chat.Component> challenges = new ArrayList<>();
                for (int c = bounded(buffer, 3); c > 0; c--) challenges.add(buffer.readComponent());
                List<EnemyPreview> enemies = new ArrayList<>();
                for (int e = bounded(buffer, 128); e > 0; e--)
                    enemies.add(new EnemyPreview(buffer.readResourceLocation(), bounded(buffer, 128)));
                waves.add(new WavePreview(wave, stars, enemies, challenges));
            }
            floors.add(new FloorPreview(floor, waves));
        }
        return floors;
    }
    public static void select(UUID token, int floor) { CHANNEL.sendToServer(new SelectFloor(token, floor)); }
    public static void keepFloorsOpen(UUID token) { CHANNEL.sendToServer(new KeepFloorsOpen(token)); }
    public static void encounterStatus(ServerPlayer player, EncounterStatus status) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), status);
    }
    public static void closeFloors(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseFloors());
    }
}
