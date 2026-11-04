package com.pla.spiral_abyss.world;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Lifetime normal-world victories, independent of every Abyss reset and of vanilla Abyss kill stats. */
public final class BossProgress {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, Map<UUID, Progress>> SERVERS = new IdentityHashMap<>();
    private static final class Progress {
        final Set<ResourceLocation> defeated = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
        boolean writable = true;
    }
    private BossProgress() {}

    public static boolean hasDefeated(ServerPlayer player, ResourceLocation id) {
        return progress(player).defeated.contains(id);
    }
    public static void initialize(ServerPlayer player) { progress(player); }

    /** Invoked before vanilla awards its kill stat, after its cancellable death hook succeeds. */
    public static void recordKill(ServerPlayer player, Entity victim) {
        Progress progress = progress(player);
        if (victim.level().isClientSide || victim.level().dimension().equals(AbyssDimension.LEVEL)
                || AbyssPlatform.data(victim).hasUUID(WaveSpawner.RUN_TAG)) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        // Track every species so adding a boss to the datapack later preserves existing progression.
        if (id != null && progress.writable && progress.defeated.add(id)) save(player, progress);
    }
    private static Path path(ServerPlayer player) {
        return player.server.getWorldPath(LevelResource.ROOT).resolve("spiral_abyss/boss_progress")
                .resolve(player.getUUID() + ".json");
    }
    private static Progress progress(ServerPlayer player) {
        return SERVERS.computeIfAbsent(player.server, unused -> new HashMap<>())
                .computeIfAbsent(player.getUUID(), unused -> load(player));
    }
    private static Progress load(ServerPlayer player) {
        Progress progress = new Progress();
        Path path = path(player);
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.get("version").getAsInt() != 1) throw new JsonParseException("Unsupported boss progress version");
                for (JsonElement entry : root.getAsJsonArray("defeated")) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
                    if (id == null) throw new JsonParseException("Invalid boss ID");
                    progress.defeated.add(id);
                }
            } catch (Exception error) {
                // Never replace damaged progression with a fresh import containing Abyss victories.
                progress.defeated.clear(); progress.writable = false;
                LogUtils.getLogger().error("Cannot read Abyss boss progression {}; preserve and repair this file", path, error);
            }
        } else {
            // ServerStatsCounter already loads world/stats/<uuid>.json, including legacy DailyBoss kills.
            // Seed once only: historical stats cannot identify which dimension an old kill occurred in.
            for (var type : BuiltInRegistries.ENTITY_TYPE.stream().toList())
                if (player.getStats().getValue(Stats.ENTITY_KILLED.get(type)) > 0)
                    progress.defeated.add(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            save(player, progress);
        }
        return progress;
    }
    private static void save(ServerPlayer player, Progress progress) {
        Path path = path(player), temporary = path.resolveSibling(path.getFileName() + ".tmp");
        JsonObject root = new JsonObject(); root.addProperty("version", 1);
        JsonArray defeated = new JsonArray(); progress.defeated.forEach(id -> defeated.add(id.toString()));
        root.add("defeated", defeated);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException error) {
            LogUtils.getLogger().error("Cannot save Abyss boss progression {}", path, error);
        }
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) initialize(player);
    }
    public static void stopped(ServerStoppedEvent event) { SERVERS.remove(event.getServer()); }
}
