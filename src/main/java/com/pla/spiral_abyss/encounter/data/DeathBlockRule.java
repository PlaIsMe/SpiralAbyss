package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashSet;
import java.util.Set;

/** A bounded, short-lived footprint for block rewards from a dying encounter mob. */
public record DeathBlockRule(Set<ResourceLocation> blocks, int radius, int height, int ticks) {
    public static final DeathBlockRule NONE = new DeathBlockRule(Set.of(), 0, 0, 0);

    public DeathBlockRule {
        blocks = Set.copyOf(blocks);
        if (blocks.size() > 64 || radius < 0 || radius > 16 || height < 0 || height > 64
                || ticks < (blocks.isEmpty() ? 0 : 1) || ticks > 1200)
            throw new IllegalArgumentException("Invalid death block suppression bounds");
    }

    public boolean configured() { return !blocks.isEmpty(); }

    public static DeathBlockRule parse(JsonObject object) {
        if (!object.keySet().equals(Set.of("blocks", "radius", "height", "ticks"))
                || !object.get("blocks").isJsonArray())
            throw new JsonParseException("suppress_death_blocks requires blocks, radius, height and ticks");
        var entries = object.getAsJsonArray("blocks");
        if (entries.isEmpty() || entries.size() > 64)
            throw new JsonParseException("suppress_death_blocks needs 1..64 block IDs");
        Set<ResourceLocation> blocks = new LinkedHashSet<>();
        for (var entry : entries) blocks.add(FloorParser.id(entry));
        return new DeathBlockRule(blocks, integer(object, "radius"), integer(object, "height"), integer(object, "ticks"));
    }

    private static int integer(JsonObject object, String key) {
        var value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new JsonParseException(key + " must be an integer");
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException exception) { throw new JsonParseException(key + " must be an integer", exception); }
    }
}
