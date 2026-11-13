package com.pla.spiral_abyss.boss_enviroment;

import com.google.gson.*;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import com.pla.spiral_abyss.encounter.MobSpawnData;

/** Bounded explicit placements; IDs are resolved at use so optional mods may be absent. */
public final class EnvironmentPlacements {
    public record Entry(ResourceLocation id, EnvironmentPosition position, int count, Vec3 offset, String spawnNbt) {
        public Vec3 anchor(Vec3 center, double halfWidth) {
            return offset == null ? position.resolve(center, halfWidth) : center.add(offset);
        }
    }
    private EnvironmentPlacements() {}
    public static List<Entry> parse(JsonObject json, String field, String idField) {
        if (!json.has(field)) return List.of();
        if (!json.get(field).isJsonArray()) throw new JsonParseException(field + " must be a list");
        JsonArray list = json.getAsJsonArray(field);
        if (list.size() > 64) throw new JsonParseException(field + " supports at most 64 entries");
        List<Entry> entries = new ArrayList<>();
        int total = 0;
        for (JsonElement element : list) {
            JsonObject spec = element.getAsJsonObject();
            for (String key : spec.keySet()) if (!Set.of(idField, "position", "count", "offset", "spawn_nbt").contains(key))
                throw new JsonParseException("Unknown " + field + " field " + key);
            ResourceLocation id = FloorParser.id(spec.get(idField));
            EnvironmentPosition position = EnvironmentPosition.parse(spec.has("position") ? spec.get("position").getAsString() : "middle");
            int count = BossEnvironmentCatalog.integer(spec, "count", 1, 1, 64);
            total += count;
            if (total > 64) throw new JsonParseException(field + " supports at most 64 total placements");
            Vec3 offset = null;
            if (spec.has("offset")) {
                if (count != 1) throw new JsonParseException("Exact offsets require count 1");
                if (spec.has("position")) throw new JsonParseException("Use position or offset, not both");
                JsonArray xyz = spec.getAsJsonArray("offset");
                if (xyz.size() != 3) throw new JsonParseException("offset needs [x, y, z]");
                double x = xyz.get(0).getAsDouble(), y = xyz.get(1).getAsDouble(), z = xyz.get(2).getAsDouble();
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                        || Math.abs(x) > 46 || Math.abs(z) > 46 || y < 0 || y > 40)
                    throw new JsonParseException("offset exceeds the bounded room");
                offset = new Vec3(x, y, z);
            }
            String nbt = spec.has("spawn_nbt") ? spec.get("spawn_nbt").getAsString() : "{}";
            if (idField.equals("block") && !nbt.equals("{}")) throw new JsonParseException("Blocks do not support spawn_nbt");
            MobSpawnData.parse(nbt);
            entries.add(new Entry(id, position, count, offset, nbt));
        }
        return List.copyOf(entries);
    }
}
