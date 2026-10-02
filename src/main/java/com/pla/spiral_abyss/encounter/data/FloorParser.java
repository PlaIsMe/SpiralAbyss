package com.pla.spiral_abyss.encounter.data;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** The JSON contract lives here, independently of combat and the key entity. */
public final class FloorParser {
    public static final int MAX_FLOORS = 6;
    private FloorParser() {}

    public static FloorDefinition parse(int number, JsonObject floor, SortedMap<Integer, JsonObject> waves) {
        require(number >= 1 && number <= MAX_FLOORS, "floor number must be 1..6");
        require(!waves.isEmpty() && waves.size() <= 100, "a floor needs 1..100 waves");
        double health = scale(floor, "health_scale", 1);
        double damage = scale(floor, "damage_scale", 1);
        int delay = integer(floor, "wave_delay", 5, 0, 300) * 20;
        JsonObject loot = floor.has("rewards") ? object(floor.get("rewards"), "rewards") : new JsonObject();
        List<FloorDefinition.RewardEntry> entries = new ArrayList<>();
        if (loot.has("loot_tables")) {
            for (JsonElement entry : array(loot.get("loot_tables"), "loot_tables"))
                entries.add(new FloorDefinition.RewardEntry(id(entry), false, 1, 1, 1, 1));
        }
        if (loot.has("entries")) {
            for (JsonElement entry : array(loot.get("entries"), "entries")) {
                JsonObject spec = object(entry, "reward entry");
                require(spec.has("item") != spec.has("loot_table"), "reward needs exactly one of item or loot_table");
                boolean item = spec.has("item");
                ResourceLocation rewardId = id(spec.get(item ? "item" : "loot_table"));
                if (item) require(ForgeRegistries.ITEMS.containsKey(rewardId)
                        && !rewardId.equals(new ResourceLocation("minecraft", "air")), "unknown item: " + rewardId);
                int min = integer(spec, "min_count", integer(spec, "count", 1, 1, 4096), 1, 4096);
                int max = integer(spec, "max_count", min, min, 4096);
                int weight = integer(spec, "weight", 1, 1, 10000);
                double chance = 1;
                if (spec.has("chance")) {
                    JsonElement value = spec.get("chance");
                    require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "chance must be a number");
                    chance = value.getAsDouble();
                    require(Double.isFinite(chance) && chance >= 0 && chance <= 1, "chance must be 0..1");
                }
                entries.add(new FloorDefinition.RewardEntry(rewardId, item, min, max, weight, chance));
            }
        }
        require(entries.size() <= 64, "at most 64 reward entries are allowed");
        int rolls = integer(loot, "rolls", entries.isEmpty() ? 0 : 1, 0, 100);
        require(rolls == 0 || !entries.isEmpty(), "reward rolls require entries or loot_tables");
        int experience = integer(loot, "experience", 0, 0, 100000);
        List<FloorDefinition.Wave> parsed = new ArrayList<>();
        for (var entry : waves.entrySet()) {
            require(entry.getKey() == parsed.size() + 1, "wave files must be consecutive, starting at wave_1.json");
            JsonObject wave = entry.getValue();
            require(wave.has("total_time"), "wave_" + entry.getKey() + " needs total_time (seconds)");
            int ticks = integer(wave, "total_time", 0, 1, 86400) * 20;
            List<FloorDefinition.Enemy> enemies = new ArrayList<>();
            require(wave.has("bosses"), "wave needs a bosses array");
            int count = 0;
            for (JsonElement boss : array(wave.get("bosses"), "bosses")) {
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
                        && !mobId.equals(new ResourceLocation("spiral_abyss", "reward_chest")),
                        "the key cannot be a wave enemy");
                enemies.add(new FloorDefinition.Enemy(mobId, amount));
                count += amount;
            }
            require(count > 0 && count <= 128, "each wave needs 1..128 mobs");
            parsed.add(new FloorDefinition.Wave(ticks, scale(wave, "health_scale", health),
                    scale(wave, "damage_scale", damage), enemies));
        }
        return new FloorDefinition(number, delay, new FloorDefinition.Rewards(entries, rolls, experience), parsed);
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
        require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), key + " must be an integer");
        double number = value.getAsDouble();
        require(Double.isFinite(number) && number == Math.rint(number) && number >= min && number <= max,
                key + " must be an integer in " + min + ".." + max);
        return (int) number;
    }

    private static ResourceLocation id(JsonElement element) {
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
