package com.pla.spiral_abyss.boss_enviroment;

import com.google.gson.*;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Bounded explicit placements; IDs are resolved at use so optional mods may be absent. */
public final class EnvironmentPlacements {
    public record Entry(ResourceLocation id, EnvironmentPosition position, int count) {}
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
            for (String key : spec.keySet()) if (!Set.of(idField, "position", "count").contains(key))
                throw new JsonParseException("Unknown " + field + " field " + key);
            ResourceLocation id = FloorParser.id(spec.get(idField));
            EnvironmentPosition position = EnvironmentPosition.parse(spec.has("position") ? spec.get("position").getAsString() : "middle");
            int count = BossEnvironmentCatalog.integer(spec, "count", 1, 1, 64);
            total += count;
            if (total > 64) throw new JsonParseException(field + " supports at most 64 total placements");
            entries.add(new Entry(id, position, count));
        }
        return List.copyOf(entries);
    }
}
