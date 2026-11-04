package com.pla.spiral_abyss.network;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.encounter.data.BossOptions.PreviewAnimation;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.ComponentSerialization;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.encounter.AbyssRejection;
import com.pla.spiral_abyss.encounter.JournalPreviews;
import com.pla.spiral_abyss.encounter.RewardPool;
import com.pla.spiral_abyss.encounter.RewardPreviews;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;

public final class AbyssNetwork {
    public static final String VERSION = "24";
    private static final Map<ServerPlayer, Integer> JOURNAL_REQUESTS = new WeakHashMap<>();
    public record OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside,
                             long resetRemainingMillis, List<FloorPreview> floors, BlockPos entryPortal, int endlessWave) implements CustomPacketPayload {
        public static final Type<OpenFloors> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "open_floors"));
        @Override public Type<OpenFloors> type() { return TYPE; }
        public OpenFloors { floors = List.copyOf(floors); endlessWave = Math.max(1, endlessWave); }
        public OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside,
                          long resetRemainingMillis, List<FloorPreview> floors, BlockPos entryPortal) {
            this(token, claimed, unlocked, selectable, visible, inside, resetRemainingMillis, floors, entryPortal, 1);
        }
        public OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside,
                          long resetRemainingMillis, List<FloorPreview> floors) {
            this(token, claimed, unlocked, selectable, visible, inside, resetRemainingMillis, floors, null);
        }
    }
    public record EnemyPreview(ResourceLocation id, int count, boolean rejected, PreviewAnimation animation) {
        public EnemyPreview(ResourceLocation id, int count, boolean rejected) { this(id, count, rejected, PreviewAnimation.DEFAULT); }
        public EnemyPreview(ResourceLocation id, int count) { this(id, count, false); }
    }
    public record WavePreview(int wave, int stars, List<EnemyPreview> enemies, List<Component> challenges) {
        public WavePreview { enemies = List.copyOf(enemies); challenges = List.copyOf(challenges); }
    }
    public record RewardPreview(ResourceLocation id, boolean table, boolean rare, List<ResourceLocation> contents,
                                boolean incomplete, ItemStack icon, double displayChance) {
        public RewardPreview {
            contents = List.copyOf(contents); icon = icon.copy();
            if (!Double.isFinite(displayChance) || displayChance < 0 || displayChance > 1)
                throw new IllegalArgumentException("Invalid Abyss reward preview chance");
        }
        public RewardPreview(ResourceLocation id, boolean table, boolean rare, List<ResourceLocation> contents,
                             boolean incomplete, ItemStack icon) {
            this(id, table, rare, contents, incomplete, icon,
                    rare ? RewardPool.RARE_CHANCE : 0);
        }
        public RewardPreview(ResourceLocation id, boolean table, boolean rare, List<ResourceLocation> contents, boolean incomplete) {
            this(id, table, rare, contents, incomplete, ItemStack.EMPTY);
        }
    }
    public record FloorPreview(int floor, List<WavePreview> waves, List<RewardPreview> rewards) {
        public FloorPreview { waves = List.copyOf(waves); rewards = List.copyOf(rewards); }
        public FloorPreview(int floor, List<WavePreview> waves) { this(floor, waves, List.of()); }
    }
    public record CloseFloors() implements CustomPacketPayload {
        public static final Type<CloseFloors> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "close_floors"));
        @Override public Type<CloseFloors> type() { return TYPE; }}
    public record RequestJournal() implements CustomPacketPayload {
        public static final Type<RequestJournal> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "request_journal"));
        @Override public Type<RequestJournal> type() { return TYPE; }}
    public record OpenJournal(long resetRemainingMillis, long poseSeed, List<BannerBoss> bosses) implements CustomPacketPayload {
        public static final Type<OpenJournal> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "open_journal"));
        @Override public Type<OpenJournal> type() { return TYPE; }
        public OpenJournal { bosses = List.copyOf(bosses); }
    }
    public record BannerBoss(int floor, int wave, ResourceLocation id, List<RewardPreview> rewards, PreviewAnimation animation) {
        public BannerBoss { rewards = List.copyOf(rewards); }
        public BannerBoss(int floor, int wave, ResourceLocation id, List<RewardPreview> rewards) {
            this(floor, wave, id, rewards, PreviewAnimation.DEFAULT);
        }
    }
    public record SelectFloor(UUID token, int floor) implements CustomPacketPayload {
        public static final Type<SelectFloor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "select_floor"));
        @Override public Type<SelectFloor> type() { return TYPE; }}
    public record KeepFloorsOpen(UUID token) implements CustomPacketPayload {
        public static final Type<KeepFloorsOpen> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "keep_floors_open"));
        @Override public Type<KeepFloorsOpen> type() { return TYPE; }}
    public record RareReward(Component player, ItemStack item) implements CustomPacketPayload {
        public static final Type<RareReward> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "rare_reward"));
        @Override public Type<RareReward> type() { return TYPE; }
        public RareReward { player = player.copy(); item = item.copy(); }
    }
    public record ChallengeView(Component description, int progress, int target, boolean satisfied, boolean failed) {}
    public record EncounterStatus(boolean active, boolean preparing, int floor, int wave, int defeated, int total, int seconds, int countdownSeconds, List<ChallengeView> challenges, List<ResourceLocation> rejectedBosses) implements CustomPacketPayload {
        public static final Type<EncounterStatus> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "encounter_status"));
        @Override public Type<EncounterStatus> type() { return TYPE; }
        public EncounterStatus { challenges = List.copyOf(challenges); rejectedBosses = List.copyOf(rejectedBosses); }
        public EncounterStatus(boolean active, boolean preparing, int floor, int wave, int defeated, int total,
                               int seconds, int countdownSeconds, List<ChallengeView> challenges) {
            this(active, preparing, floor, wave, defeated, total, seconds, countdownSeconds, challenges, List.of());
        }
        public static final EncounterStatus HIDDEN = new EncounterStatus(false, false, 0, 0, 0, 0, 0, 0, List.of());
    }
    private AbyssNetwork() {}
    public static void register(AbyssPayloadRegistrar registrar) {
        registrar.playToClient(OpenFloors.TYPE, StreamCodec.of((buffer, message) -> writeFloors(message, buffer), AbyssNetwork::readFloors), (message, context) -> PortalEntryAnimation.open(message));
        registrar.playToServer(SelectFloor.TYPE, StreamCodec.of((buffer, message) -> { buffer.writeUUID(message.token); buffer.writeVarInt(message.floor); }, buffer -> new SelectFloor(buffer.readUUID(), buffer.readVarInt())), (message, context) -> {
                    ServerPlayer sender = (ServerPlayer) context.player();
                    if (sender != null && message.floor >= -1 && message.floor <= 7)
                        AbyssParties.get(sender.server).portals().choose(sender, message.token, message.floor);
                });
        registrar.playToClient(EncounterStatus.TYPE, StreamCodec.of((buffer, message) -> {
                    buffer.writeBoolean(message.active); buffer.writeBoolean(message.preparing);
                    buffer.writeVarInt(message.floor); buffer.writeVarInt(message.wave);
                    buffer.writeVarInt(message.defeated); buffer.writeVarInt(message.total); buffer.writeVarInt(message.seconds); buffer.writeVarInt(message.countdownSeconds);
                    writeChallenges(buffer, message.challenges);
                    buffer.writeCollection(message.rejectedBosses, FriendlyByteBuf::writeResourceLocation);
                }, buffer -> new EncounterStatus(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), readChallenges(buffer), readRejectedBosses(buffer))), (message, context) -> AbyssEncounterOverlay.update(message));
        registrar.playToClient(CloseFloors.TYPE, StreamCodec.of((buffer, message) -> {}, buffer -> new CloseFloors()), (message, context) -> FloorSelectionScreen.closeForReset());
        registrar.playToServer(KeepFloorsOpen.TYPE, StreamCodec.of((buffer, message) -> buffer.writeUUID(message.token), buffer -> new KeepFloorsOpen(buffer.readUUID())), (message, context) -> {
                    ServerPlayer sender = (ServerPlayer) context.player();
                    if (sender != null) AbyssParties.get(sender.server).portals().keepOpen(sender, message.token);
                });
        registrar.playToClient(RareReward.TYPE, StreamCodec.of((buffer, message) -> { ComponentSerialization.STREAM_CODEC.encode(buffer, message.player); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, message.item); }, buffer -> new RareReward(ComponentSerialization.STREAM_CODEC.decode(buffer), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer))), (message, context) -> RareRewardOverlay.enqueue(message.player, message.item));
        registrar.playToServer(RequestJournal.TYPE, StreamCodec.of((buffer, message) -> {}, buffer -> new RequestJournal()), (message, context) -> {
                    ServerPlayer sender = (ServerPlayer) context.player();
                    if (sender == null || !sender.isAlive() || sender.hasDisconnected()) return;
                    int now = sender.server.getTickCount();
                    Integer last = JOURNAL_REQUESTS.get(sender);
                    if (last != null && now - last < 5) return;
                    JOURNAL_REQUESTS.put(sender, now);
                    var parties = AbyssParties.get(sender.server);
                    if (sender.level().dimension().equals(AbyssDimension.LEVEL)) {
                        parties.portals().openArenaMenu(sender);
                        return;
                    }
                    AbyssPacketDistributor.sendToPlayer(sender,
                            new OpenJournal(parties.resetRemainingMillis(), parties.cycle().bannerPoseSeed(),
                                    JournalPreviews.bosses(sender)));
                });
        registrar.playToClient(OpenJournal.TYPE, StreamCodec.of((buffer, message) -> writeJournal(message, buffer), AbyssNetwork::readJournal), (message, context) -> JournalClientHandler.open(message));
    }
    private static void writeFloors(OpenFloors message, RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(message.token); buffer.writeVarInt(message.claimed); buffer.writeVarInt(message.unlocked);
        buffer.writeVarInt(message.selectable); buffer.writeVarInt(message.visible);
        buffer.writeBoolean(message.inside); buffer.writeLong(message.resetRemainingMillis);
        writePreviews(buffer, message.floors);
        buffer.writeNullable(message.entryPortal, (buf, pos) -> buf.writeBlockPos(pos));
        buffer.writeVarInt(message.endlessWave);
    }
    private static OpenFloors readFloors(RegistryFriendlyByteBuf buffer) {
        return new OpenFloors(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readLong(), readPreviews(buffer),
                buffer.readNullable(buf -> buf.readBlockPos()), buffer.readVarInt());
    }
    private static void writeJournal(OpenJournal message, RegistryFriendlyByteBuf buffer) {
        buffer.writeLong(message.resetRemainingMillis()); buffer.writeLong(message.poseSeed());
        buffer.writeVarInt(message.bosses().size());
        for (var boss : message.bosses()) {
            buffer.writeVarInt(boss.floor()); buffer.writeVarInt(boss.wave()); buffer.writeResourceLocation(boss.id());
            writeAnimation(buffer, boss.animation());
            buffer.writeVarInt(boss.rewards().size());
            for (var reward : boss.rewards()) {
                buffer.writeResourceLocation(reward.id()); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, reward.icon()); buffer.writeDouble(reward.displayChance());
            }
        }
    }
    private static OpenJournal readJournal(RegistryFriendlyByteBuf buffer) {
        long remaining = buffer.readLong(), poseSeed = buffer.readLong();
        List<BannerBoss> bosses = new ArrayList<>();
        for (int b = bounded(buffer, 4); b > 0; b--) {
            int floor = bounded(buffer, 6), wave = bounded(buffer, 3);
            ResourceLocation id = buffer.readResourceLocation();
            PreviewAnimation animation = readAnimation(buffer);
            List<RewardPreview> rewards = new ArrayList<>();
            for (int r = bounded(buffer, RewardPreviews.MAX_REWARDS); r > 0; r--)
                rewards.add(new RewardPreview(buffer.readResourceLocation(), false, true, List.of(), false,
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readDouble()));
            bosses.add(new BannerBoss(floor, wave, id, rewards, animation));
        }
        return new OpenJournal(remaining, poseSeed, bosses);
    }
    public static void openFloors(ServerPlayer player, UUID token, int claimed, int selectable, int visible, boolean inside) {
        openFloors(player, token, claimed, selectable, visible, inside, null);
    }
    public static void openFloors(ServerPlayer player, UUID token, int claimed, int selectable, int visible, boolean inside,
                                  BlockPos entryPortal) {
        var progress = AbyssParties.get(player.server).profile(player.getUUID()).progress;
        int unlocked = 1 << 7;
        for (int floor = 1; floor <= 6; floor++) if (progress.unlocked(floor)) unlocked |= 1 << floor;
        AbyssPacketDistributor.sendToPlayer(player, new OpenFloors(token, claimed, unlocked, selectable, visible, inside,
                AbyssParties.get(player.server).resetRemainingMillis(), previews(player), entryPortal,
                AbyssParties.get(player.server).profile(player.getUUID()).endlessWave));
    }
    /** Preview uses the same persisted cycle lineups as spawning; the client never rolls mobs. */
    private static List<FloorPreview> previews(ServerPlayer player) {
        var parties = AbyssParties.get(player.server);
        var progress = parties.profile(player.getUUID()).progress;
        List<FloorPreview> floors = new ArrayList<>();
        var rewards = new RewardPreviews(player.server);
        for (var definition : new TreeMap<>(FloorCatalog.snapshot().floors()).values()) {
            List<WavePreview> waves = new ArrayList<>();
            Set<ResourceLocation> floorEnemies = new LinkedHashSet<>();
            List<Challenge> challenges;
            try { challenges = parties.challenges(definition.number()); }
            catch (IllegalArgumentException ex) { challenges = List.of(); }
            int[] stars = progress.waveStars(definition.number());
            for (int index = 0; index < definition.waves().size(); index++) {
                Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
                try {
                    parties.cycleLineup(definition.number(), index + 1, definition.waves().get(index))
                            .forEach(enemy -> counts.merge(enemy.id(), enemy.count(), Integer::sum));
                } catch (IllegalArgumentException ex) {
                    LogUtils.getLogger().warn("Cannot preview Abyss floor {} wave {}: {}", definition.number(), index + 1, ex.getMessage());
                }
                int wavePopulation = counts.values().stream().mapToInt(Integer::intValue).sum();
                floorEnemies.addAll(counts.keySet());
                List<Component> objectives = challenges.isEmpty()
                        ? List.of(Component.translatable("message.spiral_abyss.challenges_unavailable"))
                        : challenges.stream().map(challenge -> challenge.description(wavePopulation)).toList();
                waves.add(new WavePreview(index + 1, stars[index], counts.entrySet().stream()
                        .map(entry -> new EnemyPreview(entry.getKey(), entry.getValue(),
                                AbyssRejection.isRejected(player, definition.number(), entry.getKey()),
                                MobCatalog.bossOptions(entry.getKey()).previewAnimation()))
                        .toList(), objectives));
            }
            floors.add(new FloorPreview(definition.number(), waves, rewards.floor(definition.rewards(), floorEnemies)));
        }
        return floors;
    }
    private static void writePreviews(RegistryFriendlyByteBuf buffer, List<FloorPreview> floors) {
        buffer.writeVarInt(floors.size());
        for (var floor : floors) {
            buffer.writeVarInt(floor.floor()); buffer.writeVarInt(floor.waves().size());
            for (var wave : floor.waves()) {
                buffer.writeVarInt(wave.wave()); buffer.writeVarInt(wave.stars());
                buffer.writeVarInt(wave.challenges().size()); wave.challenges().forEach(component -> ComponentSerialization.STREAM_CODEC.encode(buffer, component));
                buffer.writeVarInt(wave.enemies().size());
                for (var enemy : wave.enemies()) {
                    buffer.writeResourceLocation(enemy.id()); buffer.writeVarInt(enemy.count()); buffer.writeBoolean(enemy.rejected());
                    writeAnimation(buffer, enemy.animation());
                }
            }
            buffer.writeVarInt(floor.rewards().size());
            for (var reward : floor.rewards()) {
                buffer.writeResourceLocation(reward.id()); buffer.writeBoolean(reward.table()); buffer.writeBoolean(reward.rare());
                buffer.writeBoolean(reward.incomplete()); buffer.writeDouble(reward.displayChance());
                buffer.writeVarInt(reward.contents().size());
                reward.contents().forEach(buffer::writeResourceLocation);
                if (!reward.table()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, reward.icon());
            }
        }
    }
    private static void writeChallenges(RegistryFriendlyByteBuf buffer, List<ChallengeView> challenges) {
        buffer.writeVarInt(challenges.size());
        for (var challenge : challenges) {
            ComponentSerialization.STREAM_CODEC.encode(buffer, challenge.description());
            buffer.writeVarInt(challenge.progress()); buffer.writeVarInt(challenge.target());
            buffer.writeBoolean(challenge.satisfied()); buffer.writeBoolean(challenge.failed());
        }
    }
    private static void writeAnimation(RegistryFriendlyByteBuf buffer, PreviewAnimation animation) {
        buffer.writeUtf(animation.name(), 128); buffer.writeDouble(animation.timeSeconds());
    }
    private static PreviewAnimation readAnimation(RegistryFriendlyByteBuf buffer) {
        return new PreviewAnimation(buffer.readUtf(128), buffer.readDouble());
    }
    private static List<ChallengeView> readChallenges(RegistryFriendlyByteBuf buffer) {
        List<ChallengeView> result = new ArrayList<>();
        for (int c = bounded(buffer, 3); c > 0; c--) result.add(new ChallengeView(ComponentSerialization.STREAM_CODEC.decode(buffer),
                bounded(buffer, 1_000_000), bounded(buffer, 1_000_000), buffer.readBoolean(), buffer.readBoolean()));
        return result;
    }
    private static List<ResourceLocation> readRejectedBosses(RegistryFriendlyByteBuf buffer) {
        List<ResourceLocation> result = new ArrayList<>();
        for (int count = bounded(buffer, 128); count > 0; count--) result.add(buffer.readResourceLocation());
        return result;
    }
    private static int bounded(RegistryFriendlyByteBuf buffer, int max) {
        int number = buffer.readVarInt();
        if (number < 0 || number > max) throw new IllegalArgumentException("Invalid Abyss preview size");
        return number;
    }
    private static List<FloorPreview> readPreviews(RegistryFriendlyByteBuf buffer) {
        List<FloorPreview> floors = new ArrayList<>();
        for (int f = bounded(buffer, 6); f > 0; f--) {
            int floor = bounded(buffer, 6);
            List<WavePreview> waves = new ArrayList<>();
            for (int w = bounded(buffer, 3); w > 0; w--) {
                int wave = bounded(buffer, 3), stars = bounded(buffer, 3);
                List<Component> challenges = new ArrayList<>();
                for (int c = bounded(buffer, 3); c > 0; c--) challenges.add(ComponentSerialization.STREAM_CODEC.decode(buffer));
                List<EnemyPreview> enemies = new ArrayList<>();
                for (int e = bounded(buffer, 128); e > 0; e--)
                    enemies.add(new EnemyPreview(buffer.readResourceLocation(), bounded(buffer, 128), buffer.readBoolean(), readAnimation(buffer)));
                waves.add(new WavePreview(wave, stars, enemies, challenges));
            }
            List<RewardPreview> rewards = new ArrayList<>();
            for (int r = bounded(buffer, RewardPreviews.MAX_REWARDS); r > 0; r--) {
                ResourceLocation id = buffer.readResourceLocation();
                boolean table = buffer.readBoolean(), rare = buffer.readBoolean(), incomplete = buffer.readBoolean();
                double displayChance = buffer.readDouble();
                List<ResourceLocation> contents = new ArrayList<>();
                for (int i = bounded(buffer, RewardPreviews.MAX_CONTENTS); i > 0; i--)
                    contents.add(buffer.readResourceLocation());
                var icon = table ? ItemStack.EMPTY : ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                rewards.add(new RewardPreview(id, table, rare, contents, incomplete, icon, displayChance));
            }
            floors.add(new FloorPreview(floor, waves, rewards));
        }
        return floors;
    }
    public static void select(UUID token, int floor) { AbyssPacketDistributor.sendToServer(new SelectFloor(token, floor)); }
    public static void requestJournal() { AbyssPacketDistributor.sendToServer(new RequestJournal()); }
    public static void broadcastRareReward(Component player, ItemStack item) {
        AbyssPacketDistributor.sendToAllPlayers(new RareReward(player, item));
    }
    public static void keepFloorsOpen(UUID token) { AbyssPacketDistributor.sendToServer(new KeepFloorsOpen(token)); }
    public static void encounterStatus(ServerPlayer player, EncounterStatus status) {
        AbyssPacketDistributor.sendToPlayer(player, status);
    }
    public static void closeFloors(ServerPlayer player) {
        AbyssPacketDistributor.sendToPlayer(player, new CloseFloors());
    }
}
