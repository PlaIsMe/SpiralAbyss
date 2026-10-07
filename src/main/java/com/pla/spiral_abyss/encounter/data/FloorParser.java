package com.pla.spiral_abyss.encounter.data;

import com.google.gson.*;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** The JSON contract lives here, independently of combat and the key entity. */
public final class FloorParser {
    public static final int MAX_FLOORS = 6;
    private FloorParser() {}

    public static FloorDefinition parse(int number, JsonObject floor, SortedMap<Integer, JsonObject> waves) {
        return parse(number, floor, waves, Map.of());
    }
    public static FloorDefinition parse(int number, JsonObject floor, SortedMap<Integer, JsonObject> waves, Map<ResourceLocation, FloorDefinition.WaveType> waveTypes) {
        require(number >= 1 && number <= MAX_FLOORS, "floor number must be 1..6");
        require(waves.size() == 3, "a challenge floor needs exactly three waves");
        var challenge = challenges(floor.get("challenge"));
        double health = scale(floor, "health_scale", 1);
        double damage = scale(floor, "damage_scale", 1);
        int delay = com.pla.spiral_abyss.encounter.WaveChallenges.TRANSITION_TICKS;
        int ticks = com.pla.spiral_abyss.encounter.WaveChallenges.TIME_LIMIT_TICKS;
        JsonObject loot = floor.has("rewards") ? object(floor.get("rewards"), "rewards") : new JsonObject();
        List<FloorDefinition.RewardEntry> entries = new ArrayList<>();
        if (loot.has("loot_tables")) {
            for (JsonElement entry : array(loot.get("loot_tables"), "loot_tables"))
                entries.add(new FloorDefinition.RewardEntry(id(entry), false, 1, 1, 1, 1));
        }
        if (loot.has("entries")) {
            for (JsonElement entry : array(loot.get("entries"), "entries")) {
                JsonObject spec = object(entry, "reward entry");
                entries.add(rewardEntry(spec, 1));
            }
        }
        require(entries.size() <= 64, "at most 64 reward entries are allowed");
        int rolls = integer(loot, "rolls", entries.isEmpty() ? 0 : 1, 0, 100);
        int experience = integer(loot, "experience", 0, 0, 100000);
        List<FloorDefinition.Wave> parsed = new ArrayList<>();
        int floorEnemies = 0;
        for (var entry : waves.entrySet()) {
            require(entry.getKey() == parsed.size() + 1, "wave files must be consecutive, starting at wave_1.json");
            try {
                JsonObject wave = entry.getValue();
                List<FloorDefinition.Enemy> enemies = new ArrayList<>();
                Map<MobCategory, Integer> categories = new EnumMap<>(MobCategory.class);
                int count = 0;
                for (MobCategory category : MobCategory.values()) {
                    int amount = integer(wave, category.key(), 0, 0, 128);
                    if (amount > 0) categories.put(category, amount);
                    count += amount;
                }
                Set<String> known = new HashSet<>(List.of("total_time", "health_scale", "damage_scale", "bosses", "wave_types"));
                // Legacy total_time fields are accepted; challenge timing is fixed at five minutes per wave.
                for (MobCategory category : MobCategory.values()) known.add(category.key());
                for (String key : wave.keySet()) require(known.contains(key), "unknown wave field: " + key);
                if (wave.has("wave_types")) {
                    require(Arrays.stream(MobCategory.values()).noneMatch(category -> wave.has(category.key())) && !wave.has("bosses"),
                            "wave_types cannot be combined with category counts or bosses");
                    JsonArray names = array(wave.get("wave_types"), "wave_types");
                    require(names.size() > 0 && names.size() <= 64, "wave_types needs 1..64 distinct type names");
                    List<FloorDefinition.WaveType> choices = new ArrayList<>();
                    Set<ResourceLocation> seen = new HashSet<>();
                    for (JsonElement name : names) {
                        ResourceLocation typeId = waveTypeId(name);
                        require(seen.add(typeId), "duplicate wave type: " + typeId);
                        require(waveTypes.containsKey(typeId), "unknown or invalid wave type: " + typeId);
                        choices.add(waveTypes.get(typeId));
                    }
                    var parsedWave = new FloorDefinition.Wave(scale(wave, "health_scale", health), scale(wave, "damage_scale", damage), Map.of(), List.of(), choices);
                    com.pla.spiral_abyss.encounter.BossWavePolicy.validate(number, entry.getKey(), parsedWave);
                    floorEnemies += parsedWave.minimumEnemies();
                    parsed.add(parsedWave);
                    continue;
                }
                // Explicit IDs remain supported for existing datapacks and fixed encounters.
                for (JsonElement boss : wave.has("bosses") ? array(wave.get("bosses"), "bosses") : new JsonArray()) {
                    ResourceLocation mobId;
                    int amount = 1;
                    if (boss.isJsonPrimitive()) mobId = id(boss);
                    else {
                        JsonObject spec = object(boss, "boss entry");
                        mobId = id(spec.get("id"));
                        amount = integer(spec, "count", 1, 1, 128);
                    }
                    require(ForgeRegistries.ENTITY_TYPES.containsKey(mobId), "unknown mob id: " + mobId);
                    require(ForgeRegistries.ENTITY_TYPES.getValue(mobId) != EntityType.PLAYER,
                            "players cannot be wave enemies");
                    require(!mobId.equals(new ResourceLocation("spiral_abyss", "key_entity"))
                            && !mobId.equals(new ResourceLocation("spiral_abyss", "reward_chest"))
                            && !mobId.equals(ResourceLocation.fromNamespaceAndPath("spiral_abyss", "copper_chest"))
                            && !mobId.equals(ResourceLocation.fromNamespaceAndPath("spiral_abyss", "iron_chest"))
                            && !mobId.equals(ResourceLocation.fromNamespaceAndPath("spiral_abyss", "golden_chest"))
                            && !mobId.equals(ResourceLocation.fromNamespaceAndPath("spiral_abyss", "scroll_entity")),
                            "arena furniture cannot be a wave enemy");
                    enemies.add(new FloorDefinition.Enemy(mobId, amount));
                    count += amount;
                }
                require(count > 0 && count <= 128, "each wave needs 1..128 mobs");
                floorEnemies += count;
                var parsedWave = new FloorDefinition.Wave(scale(wave, "health_scale", health), scale(wave, "damage_scale", damage), categories, enemies);
                com.pla.spiral_abyss.encounter.BossWavePolicy.validate(number, entry.getKey(), parsedWave);
                parsed.add(parsedWave);
            } catch (IllegalArgumentException | JsonParseException ex) {
                throw new JsonParseException("data/spiral_abyss/floor_" + number + "/wave_" + entry.getKey() + ".json: " + ex.getMessage(), ex);
            }
        }
        // Named encounters are balanced by category, roles and stages, not a minimum mob count.
        boolean bossOnly = parsed.stream().allMatch(FloorDefinition.Wave::bossOnly);
        require(parsed.stream().anyMatch(wave -> !wave.waveTypes().isEmpty()) || bossOnly || floorEnemies > 30, "ordinary legacy floors need at least 31 mobs in total");
        return new FloorDefinition(number, ticks, delay, new FloorDefinition.Rewards(entries, rolls, experience), parsed, challenge);
    }

    public static Map<Challenge.Difficulty, List<Challenge.Type>> challenges(JsonElement json) {
        JsonObject object = object(json, "challenge");
        for (String key : object.keySet())
            require(Set.of("easy", "medium", "hard").contains(key), "unknown challenge tier: " + key);
        var result = new EnumMap<Challenge.Difficulty, List<Challenge.Type>>(Challenge.Difficulty.class);
        for (var difficulty : Challenge.Difficulty.values()) {
            String field = "challenge." + difficulty.key();
            JsonArray names = array(object.get(difficulty.key()), field);
            require(names.size() > 0 && names.size() <= Challenge.Type.values().length, field + " needs a nonempty list of distinct challenge names");
            List<Challenge.Type> types = new ArrayList<>();
            for (JsonElement name : names) {
                require(name.isJsonPrimitive() && name.getAsJsonPrimitive().isString(), field + " entries must be challenge names");
                Challenge.Type type = Arrays.stream(Challenge.Type.values()).filter(value -> value.key().equals(name.getAsString()))
                        .findFirst().orElseThrow(() -> new JsonParseException(field + ": unknown challenge " + name));
                require(!types.contains(type), field + ": duplicate challenge " + type.key());
                types.add(type);
            }
            result.put(difficulty, List.copyOf(types));
        }
        return Map.copyOf(result);
    }

    public static FloorDefinition.WaveType waveType(ResourceLocation id, JsonObject object) {
        try {
            for (String key : object.keySet()) require(Set.of("groups", "sequence", "tier").contains(key), "unknown wave type field: " + key + "=" + object.get(key));
            require(object.has("groups") != object.has("sequence"), "provide exactly one of groups or sequence");
            int tier = integer(object, "tier", 1, 1, 7);
            List<FloorDefinition.WaveStage> stages = new ArrayList<>();
            if (object.has("groups")) {
                for (var group : groups(object.get("groups"), "groups")) stages.add(new FloorDefinition.WaveStage(List.of(group)));
            } else {
                JsonArray sequence = array(object.get("sequence"), "sequence");
                require(sequence.size() > 0 && sequence.size() <= 128, "sequence needs 1..128 stages; got " + sequence.size());
                for (int index = 0; index < sequence.size(); index++) {
                    JsonObject stage = object(sequence.get(index), "sequence[" + index + "]");
                    for (String key : stage.keySet()) require(key.equals("groups"), "unknown sequence[" + index + "] field: " + key + "=" + stage.get(key));
                    stages.add(new FloorDefinition.WaveStage(groups(stage.get("groups"), "sequence[" + index + "].groups")));
                }
            }
            var result = new FloorDefinition.WaveType(id, stages, tier);
            require(result.count() <= 128, "wave type total count must be 1..128; got " + result.count());
            return result;
        } catch (JsonParseException ex) {
            throw new JsonParseException(id + " (data/" + id.getNamespace() + "/wave_types/" + id.getPath() + ".json): " + ex.getMessage(), ex);
        }
    }
    private static List<FloorDefinition.WaveGroup> groups(JsonElement value, String path) {
        JsonArray groups = array(value, path);
        require(groups.size() > 0 && groups.size() <= 128, path + " needs 1..128 groups; got " + groups.size());
        List<FloorDefinition.WaveGroup> result = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            String field = path + "[" + index + "]";
            JsonObject group = object(groups.get(index), field);
            for (String key : group.keySet()) require(Set.of("category", "count", "role").contains(key), field + ": unknown field " + key + "=" + group.get(key));
            require(group.has("category") && group.get("category").isJsonPrimitive() && group.get("category").getAsJsonPrimitive().isString(), field + ".category must be a string; got " + group.get("category"));
            MobCategory category = Arrays.stream(MobCategory.values()).filter(candidate -> candidate.key().equals(group.get("category").getAsString())).findFirst().orElseThrow(() -> new JsonParseException(field + ".category unknown: " + group.get("category")));
            require(group.has("count"), field + " needs count");
            int count;
            try { count = integer(group, "count", 1, 1, 128); }
            catch (JsonParseException ex) { throw new JsonParseException(field + ".count=" + group.get("count") + ": " + ex.getMessage()); }
            CombatRole role = CombatRole.ANY;
            if (group.has("role")) {
                JsonElement roleValue = group.get("role");
                require(roleValue.isJsonPrimitive() && roleValue.getAsJsonPrimitive().isString(), field + ".role must be melee, ranged or any; got " + roleValue);
                role = Arrays.stream(CombatRole.values()).filter(candidate -> candidate.key().equals(roleValue.getAsString())).findFirst().orElseThrow(() -> new JsonParseException(field + ".role unknown: " + roleValue));
            }
            result.add(new FloorDefinition.WaveGroup(category, count, role));
        }
        return result;
    }
    private static ResourceLocation waveTypeId(JsonElement name) {
        ResourceLocation parsed = id(name);
        return name.getAsString().contains(":") ? parsed : ResourceLocation.fromNamespaceAndPath("spiral_abyss", parsed.getPath());
    }

    public static FloorDefinition.RewardEntry rewardEntry(JsonObject spec, double defaultChance) {
        require(spec.has("item") != spec.has("loot_table"), "reward needs exactly one of item or loot_table");
        boolean item = spec.has("item");
        ResourceLocation rewardId = id(spec.get(item ? "item" : "loot_table"));
        int min = integer(spec, "min_count", integer(spec, "count", 1, 1, 4096), 1, 4096);
        int max = integer(spec, "max_count", min, min, 4096);
        int weight = integer(spec, "weight", 1, 1, 10000);
        double chance = defaultChance;
        if (spec.has("chance")) {
            JsonElement value = spec.get("chance");
            require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "chance must be a number");
            chance = value.getAsDouble();
            require(Double.isFinite(chance) && chance >= 0 && chance <= 1, "chance must be 0..1");
        }
        Map<ResourceLocation, Integer> enchantments = new LinkedHashMap<>();
        if (spec.has("enchantment")) {
            require(item, "enchantment is only supported on item rewards; use loot-table functions for tables");
            JsonObject configured = object(spec.get("enchantment"), "enchantment");
            require(configured.size() <= 64, "at most 64 reward enchantments are allowed");
            for (var entry : configured.entrySet()) {
                ResourceLocation enchantment = id(new JsonPrimitive(entry.getKey()));
                require(!enchantments.containsKey(enchantment), "duplicate reward enchantment: " + enchantment);
                // Minecraft reads enchantment levels in 0..255. Do not clamp to the enchantment's natural maximum.
                enchantments.put(enchantment, integer(configured, entry.getKey(), 1, 1, 255));
            }
        }
        return new FloorDefinition.RewardEntry(rewardId, item, min, max, weight, chance, false, enchantments);
    }

    private static double scale(JsonObject obj, String key, double fallback) {
        if (!obj.has(key)) return fallback;
        JsonElement value = obj.get(key);
        require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), key + " must be a number");
        double number = value.getAsDouble();
        require(Double.isFinite(number) && number > 0 && number <= 1000, key + " must be > 0 and <= 1000");
        return number;
    }

    private static int integer(JsonObject obj, String key, int fallback, int min, int max) {
        if (!obj.has(key)) return fallback;
        JsonElement value = obj.get(key);
        require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), key + "=" + value + " must be an integer");
        double number = value.getAsDouble();
        require(Double.isFinite(number) && number == Math.rint(number) && number >= min && number <= max,
                key + "=" + value + " must be an integer in " + min + ".." + max);
        return (int) number;
    }

    public static ResourceLocation id(JsonElement element) {
        require(element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(),
                "resource ids must be strings");
        ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
        require(id != null, "invalid resource id: " + element);
        return id;
    }

    private static JsonArray array(JsonElement value, String name) {
        require(value != null && value.isJsonArray(), name + " must be an array");
        return value.getAsJsonArray();
    }

    private static JsonObject object(JsonElement value, String name) {
        require(value != null && value.isJsonObject(), name + " must be an object");
        return value.getAsJsonObject();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new JsonParseException(message);
    }
}
