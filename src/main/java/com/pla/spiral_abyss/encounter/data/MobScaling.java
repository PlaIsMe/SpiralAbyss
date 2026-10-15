package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Per-floor spawn scaling loaded from mob_pools, with optional per-mob overrides. */
public final class MobScaling {
    public record Health(boolean absolute, double value) {}
    public record Floor(Health health, double damage) {}
    public record Plan(Map<Integer, Floor> floors) {
        public static final Floor NORMAL = new Floor(new Health(false, 1), 1);
        public static final Plan NORMAL_PLAN = new Plan(Map.of());
        public Plan { floors = Map.copyOf(floors); }
        public Floor at(int floor) { return floors.getOrDefault(floor, NORMAL); }
    }

    private MobScaling() {}

    public static Plan parse(JsonElement value, Plan base) {
        if (value == null || !value.isJsonObject()) throw new JsonParseException("scaling must be an object keyed by floor 1..6");
        Map<Integer, Floor> floors = new HashMap<>(base.floors());
        for (var entry : value.getAsJsonObject().entrySet()) {
            int floor;
            try { floor = Integer.parseInt(entry.getKey()); }
            catch (NumberFormatException ex) { throw new JsonParseException("scaling floor must be 1..6: " + entry.getKey(), ex); }
            if (floor < 1 || floor > 6 || !entry.getKey().equals(Integer.toString(floor)))
                throw new JsonParseException("scaling floor must be 1..6: " + entry.getKey());
            if (!entry.getValue().isJsonObject()) throw new JsonParseException("scaling floor " + floor + " must be an object");
            JsonObject spec = entry.getValue().getAsJsonObject();
            for (String key : spec.keySet()) if (!Set.of("health", "damage").contains(key))
                throw new JsonParseException("unknown scaling floor " + floor + " field: " + key);
            Floor inherited = floors.getOrDefault(floor, Plan.NORMAL);
            Health health = spec.has("health") ? health(spec.get("health")) : inherited.health();
            double damage = spec.has("damage") ? number(spec.get("damage"), "damage", 1000) : inherited.damage();
            floors.put(floor, new Floor(health, damage));
        }
        return new Plan(floors);
    }

    private static Health health(JsonElement value) {
        if (value.isJsonPrimitive()) return new Health(false, number(value, "health multiplier", 1000));
        if (!value.isJsonObject()) throw new JsonParseException("health must be a multiplier or {type,value}");
        JsonObject spec = value.getAsJsonObject();
        if (!spec.has("type") || !spec.has("value") || spec.size() != 2)
            throw new JsonParseException("health needs exactly type and value");
        String type = spec.get("type").getAsString();
        if (!type.equals("absolute") && !type.equals("multiplier"))
            throw new JsonParseException("health type must be absolute or multiplier");
        return new Health(type.equals("absolute"), number(spec.get("value"), "health value", type.equals("absolute") ? 1000000 : 1000));
    }

    private static double number(JsonElement value, String name, double maximum) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new JsonParseException(name + " must be a positive number");
        double parsed = value.getAsDouble();
        if (!Double.isFinite(parsed) || parsed <= 0 || parsed > maximum)
            throw new JsonParseException(name + " must be positive and at most " + maximum);
        return parsed;
    }
}
