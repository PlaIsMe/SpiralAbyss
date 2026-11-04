package com.pla.spiral_abyss.network;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.encounter.data.BossOptions.PreviewAnimation;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
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

public final class AbyssNetwork {
    private static final String VERSION = "23";
    private static final Map<ServerPlayer, Integer> JOURNAL_REQUESTS = new WeakHashMap<>();
    private static final AbyssChannel CHANNEL = AbyssChannel.create(VERSION);
    public record OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside,
                             long resetRemainingMillis, List<FloorPreview> floors, BlockPos entryPortal, int endlessWave) {
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
    public record CloseFloors() {}
    public record RequestJournal() {}
    public record OpenJournal(long resetRemainingMillis, long poseSeed, List<BannerBoss> bosses) {
        public OpenJournal { bosses = List.copyOf(bosses); }
    }
    public record BannerBoss(int floor, int wave, ResourceLocation id, List<RewardPreview> rewards, PreviewAnimation animation) {
        public BannerBoss { rewards = List.copyOf(rewards); }
        public BannerBoss(int floor, int wave, ResourceLocation id, List<RewardPreview> rewards) {
            this(floor, wave, id, rewards, PreviewAnimation.DEFAULT);
        }
    }
    public record SelectFloor(UUID token, int floor) {}
    public record KeepFloorsOpen(UUID token) {}
    public record RareReward(Component player, ItemStack item) {
        public RareReward { player = player.copy(); item = item.copy(); }
    }
    public record ChallengeView(Component description, int progress, int target, boolean satisfied, boolean failed) {}
    public record EncounterStatus(boolean active, boolean preparing, int floor, int wave, int defeated, int total, int seconds, int countdownSeconds, List<ChallengeView> challenges, List<ResourceLocation> rejectedBosses) {
        public EncounterStatus { challenges = List.copyOf(challenges); rejectedBosses = List.copyOf(rejectedBosses); }
        public EncounterStatus(boolean active, boolean preparing, int floor, int wave, int defeated, int total,
                               int seconds, int countdownSeconds, List<ChallengeView> challenges) {
            this(active, preparing, floor, wave, defeated, total, seconds, countdownSeconds, challenges, List.of());
        }
        public static final EncounterStatus HIDDEN = new EncounterStatus(false, false, 0, 0, 0, 0, 0, 0, List.of());
    }
    private AbyssNetwork() {}
    private static void client(Runnable action) { if (dev.architectury.platform.Platform.getEnvironment() == dev.architectury.utils.Env.CLIENT) action.run(); }
    public static void register() {
        CHANNEL.messageBuilder(OpenFloors.class, 0, AbyssChannel.Direction.PLAY_TO_CLIENT)
                .encoder(AbyssNetwork::writeFloors)
                .decoder(AbyssNetwork::readFloors)
                .consumerMainThread((message, context) -> client(() -> PortalEntryAnimation.open(message)))
                .add();
        CHANNEL.messageBuilder(SelectFloor.class, 1, AbyssChannel.Direction.PLAY_TO_SERVER)
                .encoder((message, buffer) -> { buffer.writeUUID(message.token); buffer.writeVarInt(message.floor); })
                .decoder(buffer -> new SelectFloor(buffer.readUUID(), buffer.readVarInt()))
                .consumerMainThread((message, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && message.floor >= -1 && message.floor <= 7)
                        AbyssParties.get(sender.server).portals().choose(sender, message.token, message.floor);
                }).add();
        CHANNEL.messageBuilder(EncounterStatus.class, 2, AbyssChannel.Direction.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeBoolean(message.active); buffer.writeBoolean(message.preparing);
                    buffer.writeVarInt(message.floor); buffer.writeVarInt(message.wave);
                    buffer.writeVarInt(message.defeated); buffer.writeVarInt(message.total); buffer.writeVarInt(message.seconds); buffer.writeVarInt(message.countdownSeconds);
                    writeChallenges(buffer, message.challenges);
                    buffer.writeCollection(message.rejectedBosses, FriendlyByteBuf::writeResourceLocation);
                })
                .decoder(buffer -> new EncounterStatus(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), readChallenges(buffer), readRejectedBosses(buffer)))
                .consumerMainThread((message, context) -> client(() -> AbyssEncounterOverlay.update(message)))
                .add();
        CHANNEL.messageBuilder(CloseFloors.class, 3, AbyssChannel.Direction.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {})
                .decoder(buffer -> new CloseFloors())
                .consumerMainThread((message, context) -> client(() -> FloorSelectionScreen.closeForReset()))
                .add();
        CHANNEL.messageBuilder(KeepFloorsOpen.class, 4, AbyssChannel.Direction.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeUUID(message.token))
                .decoder(buffer -> new KeepFloorsOpen(buffer.readUUID()))
                .consumerMainThread((message, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null) AbyssParties.get(sender.server).portals().keepOpen(sender, message.token);
                }).add();
        CHANNEL.messageBuilder(RareReward.class, 5, AbyssChannel.Direction.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> { buffer.writeComponent(message.player); buffer.writeItem(message.item); })
                .decoder(buffer -> new RareReward(buffer.readComponent(), buffer.readItem()))
                .consumerMainThread((message, context) -> client(() -> RareRewardOverlay.enqueue(message.player, message.item)))
                .add();
        CHANNEL.messageBuilder(RequestJournal.class, 6, AbyssChannel.Direction.PLAY_TO_SERVER)
                .encoder((message, buffer) -> {})
                .decoder(buffer -> new RequestJournal())
                .consumerMainThread((message, context) -> {
                    ServerPlayer sender = context.get().getSender();
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
                    CHANNEL.sendToPlayer(sender,
                            new OpenJournal(parties.resetRemainingMillis(), parties.cycle().bannerPoseSeed(),
                                    JournalPreviews.bosses(sender)));
                }).add();
        CHANNEL.messageBuilder(OpenJournal.class, 7, AbyssChannel.Direction.PLAY_TO_CLIENT)
                .encoder(AbyssNetwork::writeJournal)
                .decoder(AbyssNetwork::readJournal)
                .consumerMainThread((message, context) -> client(() -> JournalClientHandler.open(message)))
                .add();
    }
    private static void writeFloors(OpenFloors message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.token); buffer.writeVarInt(message.claimed); buffer.writeVarInt(message.unlocked);
        buffer.writeVarInt(message.selectable); buffer.writeVarInt(message.visible);
        buffer.writeBoolean(message.inside); buffer.writeLong(message.resetRemainingMillis);
        writePreviews(buffer, message.floors);
        buffer.writeNullable(message.entryPortal, FriendlyByteBuf::writeBlockPos);
        buffer.writeVarInt(message.endlessWave);
    }
    private static OpenFloors readFloors(FriendlyByteBuf buffer) {
        return new OpenFloors(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readLong(), readPreviews(buffer),
                buffer.readNullable(FriendlyByteBuf::readBlockPos), buffer.readVarInt());
    }
    private static void writeJournal(OpenJournal message, FriendlyByteBuf buffer) {
        buffer.writeLong(message.resetRemainingMillis()); buffer.writeLong(message.poseSeed());
        buffer.writeVarInt(message.bosses().size());
        for (var boss : message.bosses()) {
            buffer.writeVarInt(boss.floor()); buffer.writeVarInt(boss.wave()); buffer.writeResourceLocation(boss.id());
            writeAnimation(buffer, boss.animation());
            buffer.writeVarInt(boss.rewards().size());
            for (var reward : boss.rewards()) {
                buffer.writeResourceLocation(reward.id()); buffer.writeItem(reward.icon()); buffer.writeDouble(reward.displayChance());
            }
        }
    }
    private static OpenJournal readJournal(FriendlyByteBuf buffer) {
        long remaining = buffer.readLong(), poseSeed = buffer.readLong();
        List<BannerBoss> bosses = new ArrayList<>();
        for (int b = bounded(buffer, 4); b > 0; b--) {
            int floor = bounded(buffer, 6), wave = bounded(buffer, 3);
            ResourceLocation id = buffer.readResourceLocation();
            PreviewAnimation animation = readAnimation(buffer);
            List<RewardPreview> rewards = new ArrayList<>();
            for (int r = bounded(buffer, RewardPreviews.MAX_REWARDS); r > 0; r--)
                rewards.add(new RewardPreview(buffer.readResourceLocation(), false, true, List.of(), false,
                        buffer.readItem(), buffer.readDouble()));
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
        CHANNEL.sendToPlayer(player, new OpenFloors(token, claimed, unlocked, selectable, visible, inside,
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
    private static void writePreviews(FriendlyByteBuf buffer, List<FloorPreview> floors) {
        buffer.writeVarInt(floors.size());
        for (var floor : floors) {
            buffer.writeVarInt(floor.floor()); buffer.writeVarInt(floor.waves().size());
            for (var wave : floor.waves()) {
                buffer.writeVarInt(wave.wave()); buffer.writeVarInt(wave.stars());
                buffer.writeVarInt(wave.challenges().size()); wave.challenges().forEach(buffer::writeComponent);
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
                if (!reward.table()) buffer.writeItem(reward.icon());
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
    private static void writeAnimation(FriendlyByteBuf buffer, PreviewAnimation animation) {
        buffer.writeUtf(animation.name(), 128); buffer.writeDouble(animation.timeSeconds());
    }
    private static PreviewAnimation readAnimation(FriendlyByteBuf buffer) {
        return new PreviewAnimation(buffer.readUtf(128), buffer.readDouble());
    }
    private static List<ChallengeView> readChallenges(FriendlyByteBuf buffer) {
        List<ChallengeView> result = new ArrayList<>();
        for (int c = bounded(buffer, 3); c > 0; c--) result.add(new ChallengeView(buffer.readComponent(),
                bounded(buffer, 1_000_000), bounded(buffer, 1_000_000), buffer.readBoolean(), buffer.readBoolean()));
        return result;
    }
    private static List<ResourceLocation> readRejectedBosses(FriendlyByteBuf buffer) {
        List<ResourceLocation> result = new ArrayList<>();
        for (int count = bounded(buffer, 128); count > 0; count--) result.add(buffer.readResourceLocation());
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
                List<Component> challenges = new ArrayList<>();
                for (int c = bounded(buffer, 3); c > 0; c--) challenges.add(buffer.readComponent());
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
                var icon = table ? ItemStack.EMPTY : buffer.readItem();
                rewards.add(new RewardPreview(id, table, rare, contents, incomplete, icon, displayChance));
            }
            floors.add(new FloorPreview(floor, waves, rewards));
        }
        return floors;
    }
    public static void select(UUID token, int floor) { CHANNEL.sendToServer(new SelectFloor(token, floor)); }
    public static void requestJournal() { CHANNEL.sendToServer(new RequestJournal()); }
    public static void broadcastRareReward(Component player, ItemStack item) {
        CHANNEL.broadcast( new RareReward(player, item));
    }
    public static void keepFloorsOpen(UUID token) { CHANNEL.sendToServer(new KeepFloorsOpen(token)); }
    public static void encounterStatus(ServerPlayer player, EncounterStatus status) {
        CHANNEL.sendToPlayer(player, status);
    }
    public static void closeFloors(ServerPlayer player) {
        CHANNEL.sendToPlayer(player, new CloseFloors());
    }
}
