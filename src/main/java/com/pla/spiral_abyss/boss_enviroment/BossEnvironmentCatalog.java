package com.pla.spiral_abyss.boss_enviroment;

import com.pla.spiral_abyss.encounter.data.FloorParser;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.Reader;
import java.util.*;

/** Extensible entity-ID profiles; no boss class switches in terrain generation. */
public final class BossEnvironmentCatalog extends SimplePreparableReloadListener<Map<ResourceLocation, BossEnvironmentCatalog.Profile>> {
    public record Pillars(BlockState block, int minimum, int maximum, int minHeight, int maxHeight, boolean crystals) {}
    public record Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks,
                          BlockState hazard, double hazardChance, Pillars pillars, List<BlockState> decorations, boolean snowDirt,
                          int waterDepth, List<EnvironmentPlacements.Entry> entities, List<EnvironmentPlacements.Entry> blocks) {
        public Profile {
            palette = List.copyOf(palette); decorations = List.copyOf(decorations);
            entities = List.copyOf(entities); blocks = List.copyOf(blocks);
        }
        public boolean centered() { return !entities.isEmpty() || !blocks.isEmpty(); }
        public Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks,
                       BlockState hazard, double hazardChance, Pillars pillars, List<BlockState> decorations, boolean snowDirt) {
            this(id, palette, radius, height, patches, ticks, hazard, hazardChance, pillars, decorations, snowDirt, 0, List.of(), List.of());
        }
        public Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks,
                       BlockState hazard, double hazardChance, Pillars pillars, List<BlockState> decorations) {
            this(id, palette, radius, height, patches, ticks, hazard, hazardChance, pillars, decorations, false);
        }
        public Profile(ResourceLocation id, List<BlockState> palette, int radius, int height, int patches, int ticks,
                       BlockState hazard, double hazardChance, Pillars pillars) {
            this(id, palette, radius, height, patches, ticks, hazard, hazardChance, pillars,
                    hazard == null ? List.of() : List.of(hazard));
        }
    }
    private static volatile Map<ResourceLocation, Profile> profiles = Map.of();
    public static Profile get(ResourceLocation entity) { return profiles.get(entity); }
    public static Map<ResourceLocation, Profile> snapshot() { return profiles; }
    @Override protected Map<ResourceLocation, Profile> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Profile> result = new TreeMap<>();
        Set<ResourceLocation> overridden = new HashSet<>();
        manager.listResources("boss_environments", id -> id.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Profile profile = parse(entry.getKey(), json);
                Set<ResourceLocation> ids = new HashSet<>();
                for (JsonElement entity : json.getAsJsonArray("entities")) if (!ids.add(FloorParser.id(entity))) throw new JsonParseException("duplicate entity ID in profile");
                for (ResourceLocation id : ids) if (overridden.contains(id)) throw new JsonParseException("duplicate boss environment entity " + id);
                ids.forEach(id -> result.put(id, profile));
                overridden.addAll(ids);
            } catch (Exception ex) { LogUtils.getLogger().error("Invalid boss environment {}: {}", entry.getKey(), ex.getMessage()); }
        });
        overlayBossPool(manager, result);
        return Map.copyOf(result);
    }
    @Override protected void apply(Map<ResourceLocation, Profile> loaded, ResourceManager manager, ProfilerFiller profiler) { profiles = loaded; }
    public static Profile parse(ResourceLocation id, JsonObject json) {
        Set<String> fields = Set.of("entities", "palette", "radius", "height", "patches", "animation_ticks", "hazard", "hazard_chance", "pillars");
        for (String key : json.keySet()) if (!fields.contains(key)) throw new JsonParseException("unknown field " + key);
        if (!json.has("entities") || json.getAsJsonArray("entities").isEmpty()) throw new JsonParseException("entities needs at least one entity ID");
        List<BlockState> palette = new ArrayList<>();
        for (JsonElement block : json.getAsJsonArray("palette")) palette.add(block(block));
        if (palette.isEmpty() || palette.size() > 16) throw new JsonParseException("palette needs 1..16 installed non-air blocks");
        BlockState hazard = json.has("hazard") ? block(json.get("hazard"), true) : null;
        double chance = json.has("hazard_chance") ? json.get("hazard_chance").getAsDouble() : 0;
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) throw new JsonParseException("hazard_chance must be 0..1");
        Pillars pillars = null;
        if (json.has("pillars")) {
            JsonObject spec = json.getAsJsonObject("pillars");
            for (String key : spec.keySet()) if (!Set.of("block", "min_count", "max_count", "min_height", "max_height", "healing_crystals").contains(key)) throw new JsonParseException("unknown pillar field " + key);
            int minimum = integer(spec, "min_count", 4, 1, 5), minHeight = integer(spec, "min_height", 18, 1, 32);
            pillars = new Pillars(block(spec.get("block")), minimum, integer(spec, "max_count", 5, minimum, 5), minHeight, integer(spec, "max_height", 26, minHeight, 32), spec.has("healing_crystals") && spec.get("healing_crystals").getAsBoolean());
        }
        return new Profile(id, palette, integer(json, "radius", 32, 12, 32), integer(json, "height", 3, 1, 4), integer(json, "patches", 14, 1, 32), integer(json, "animation_ticks", 100, 1, 100), hazard, chance, pillars);
    }
    private static void overlayBossPool(ResourceManager manager, Map<ResourceLocation, Profile> result) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("spiral_abyss", "mob_pools/boss.json");
        var resource = manager.getResource(file);
        if (resource.isEmpty()) return;
        try (Reader reader = resource.get().openAsReader()) {
            JsonArray pool = JsonParser.parseReader(reader).getAsJsonArray();
            Set<ResourceLocation> seen = new HashSet<>();
            for (JsonElement value : pool) {
                if (!value.isJsonArray()) continue;
                JsonArray entry = value.getAsJsonArray();
                if (entry.isEmpty()) continue;
                ResourceLocation entity = FloorParser.id(entry.get(0));
                for (int index = 1; index < entry.size(); index++) {
                    JsonElement metadata = entry.get(index);
                    if (!metadata.isJsonObject() || !isMetadata(metadata.getAsJsonObject())) continue;
                    if (!seen.add(entity)) throw new JsonParseException("duplicate environment for " + entity);
                    try {
                        result.put(entity, poolEnvironment(entity, metadata.getAsJsonObject(), result.get(entity)));
                    } catch (Exception ex) {
                        LogUtils.getLogger().error("Invalid boss environment in {} for {}: {}", file, entity, ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) { LogUtils.getLogger().error("Invalid boss environment metadata in {}: {}", file, ex.getMessage()); }
    }
    public static boolean isMetadata(JsonObject object) {
        return object.has("environment") || object.has("decoration_environment") || object.has("entity_place") || object.has("block_place");
    }
    /** Geometry and dragon pillars keep their profile while pool metadata supplies effects. */
    public static Profile poolEnvironment(ResourceLocation entity, JsonObject metadata, Profile base) {
        JsonObject json;
        JsonElement environment = metadata.get("environment");
        if (environment != null && environment.isJsonObject()) {
            if (metadata.size() != 1) throw new JsonParseException("advanced environment object cannot have sibling decoration fields");
            json = environment.getAsJsonObject();
        } else {
            for (String key : metadata.keySet()) if (!Set.of("environment", "decoration_environment",
                    "decoration_chance", "decoration_placement", "entity_place", "block_place").contains(key))
                throw new JsonParseException("unknown boss environment metadata field " + key);
            json = new JsonObject();
            if (environment != null) json.add("terrain_blocks", environment);
            for (String field : List.of("entity_place", "block_place")) if (metadata.has(field)) json.add(field, metadata.get(field));
            if (metadata.has("decoration_environment")) {
                JsonObject decoration = new JsonObject();
                decoration.add("blocks", metadata.get("decoration_environment"));
                if (metadata.has("decoration_chance")) decoration.add("chance", metadata.get("decoration_chance"));
                if (metadata.has("decoration_placement")) decoration.add("placement", metadata.get("decoration_placement"));
                json.add("decoration_environment", decoration);
            } else if (metadata.has("decoration_chance") || metadata.has("decoration_placement"))
                throw new JsonParseException("decoration_environment is required for decoration chance/placement");
        }
        for (String key : json.keySet()) if (!Set.of("terrain_blocks", "decoration_environment", "entity_place", "block_place").contains(key))
            throw new JsonParseException("unknown environment field " + key);
        List<BlockState> terrain = base == null ? List.of() : base.palette();
        if (json.has("terrain_blocks")) {
            List<BlockState> chosen = new ArrayList<>();
            JsonArray blocks = blockList(json.get("terrain_blocks"));
            if (blocks.size() > 16) throw new JsonParseException("terrain_blocks supports at most 16 blocks");
            for (JsonElement value : blocks) {
                try { chosen.add(block(value)); }
                catch (JsonParseException ex) {
                    LogUtils.getLogger().warn("Ignoring boss terrain for {}: {}", entity, ex.getMessage());
                }
            }
            if (!chosen.isEmpty()) terrain = chosen;
            else if (terrain.isEmpty()) throw new JsonParseException("terrain_blocks has no installed stable blocks");
        }

        List<BlockState> decorations = base == null ? List.of() : base.decorations();
        boolean snowDirt = base != null && base.snowDirt();
        int waterDepth = base == null ? 0 : base.waterDepth();
        double chance = base == null ? 0 : base.hazardChance();
        if (json.has("decoration_environment")) {
            JsonObject decoration = json.getAsJsonObject("decoration_environment");
            for (String key : decoration.keySet()) if (!Set.of("blocks", "chance", "placement").contains(key))
                throw new JsonParseException("unknown decoration_environment field " + key);
            if (!decoration.has("blocks")) throw new JsonParseException("decoration_environment requires blocks");
            List<BlockState> chosen = new ArrayList<>();
            JsonArray blocks = blockList(decoration.get("blocks"));
            if (blocks.size() > 16) throw new JsonParseException("decoration_environment supports at most 16 blocks");
            snowDirt = false;
            waterDepth = 0;
            for (JsonElement value : blocks) {
                String preset = value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        ? value.getAsString().replace("spiral_abyss:", "") : "";
                if (Set.of("full_water", "half_water").contains(preset)) {
                    if (blocks.size() != 1) throw new JsonParseException("Water must be the only decoration_environment entry");
                    waterDepth = preset.equals("full_water") ? 120 : 15;
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && Set.of("snow_dirt", "spiral_abyss:snow_dirt").contains(value.getAsString())) {
                    if (blocks.size() != 1) throw new JsonParseException("snow_dirt must be the only decoration_environment entry");
                    snowDirt = true;
                } else {
                    try { chosen.add(block(value, true)); }
                    catch (JsonParseException ex) {
                        LogUtils.getLogger().warn("Ignoring boss decoration for {}: {}; keeping its terrain", entity, ex.getMessage());
                    }
                }
            }
            String placement = decoration.has("placement") ? decoration.get("placement").getAsString() : "surface";
            if (!placement.equals("surface")) throw new JsonParseException("decoration_environment placement must be surface");
            chance = decoration.has("chance") ? decoration.get("chance").getAsDouble() : 0.08;
            if (!Double.isFinite(chance) || chance < 0 || chance > 1) throw new JsonParseException("decoration chance must be 0..1");
            decorations = chosen;
        }
        // A composite surface preset, not a registered block or a randomly scattered decoration.
        if (snowDirt) terrain = List.of(Blocks.DIRT.defaultBlockState());
        int radius = base == null ? 32 : base.radius(), height = base == null ? 3 : base.height();
        int patches = base == null ? 14 : base.patches(), ticks = base == null ? 100 : base.ticks();
        Pillars pillars = base == null ? null : base.pillars();
        BlockState legacyHazard = decorations.isEmpty() ? null : decorations.get(0);
        List<EnvironmentPlacements.Entry> entities = json.has("entity_place") ? EnvironmentPlacements.parse(json, "entity_place", "entity")
                : base == null ? List.of() : base.entities();
        List<EnvironmentPlacements.Entry> blocks = json.has("block_place") ? EnvironmentPlacements.parse(json, "block_place", "block")
                : base == null ? List.of() : base.blocks();
        return new Profile(entity, terrain, radius, height, patches, ticks, legacyHazard, chance, pillars, decorations, snowDirt,
                waterDepth, entities, blocks);
    }
    private static JsonArray blockList(JsonElement value) {
        if (value == null) throw new JsonParseException("block list is required");
        if (value.isJsonArray()) return value.getAsJsonArray();
        if (!value.isJsonPrimitive()) throw new JsonParseException("block list must be a block ID or array");
        JsonArray list = new JsonArray(); list.add(value); return list;
    }
    static BlockState block(JsonElement value) { return block(value, false); }
    private static BlockState block(JsonElement value, boolean decoration) {
        ResourceLocation id = FloorParser.id(value);
        if (!ForgeRegistries.BLOCKS.containsKey(id) || ForgeRegistries.BLOCKS.getValue(id).defaultBlockState().isAir()) throw new JsonParseException("unavailable block " + id);
        BlockState state = ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
        if (state.hasBlockEntity()) throw new JsonParseException("block entities are not supported: " + id);
        if (state.getBlock() instanceof net.minecraft.world.level.block.FallingBlock || !state.getFluidState().isEmpty()
                || state.is(net.minecraft.world.level.block.Blocks.TNT)
                || !decoration && state.getBlock() instanceof net.minecraft.world.level.block.BushBlock)
            throw new JsonParseException("unsupported terrain/decoration block: " + id);
        return state;
    }
    static int integer(JsonObject json, String key, int fallback, int min, int max) {
        if (json.has(key) && (!json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isNumber())) throw new JsonParseException(key + " must be a number");
        double value = json.has(key) ? json.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value != Math.rint(value) || value < min || value > max) throw new JsonParseException(key + " must be integer " + min + ".." + max);
        return (int) value;
    }
}
