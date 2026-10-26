package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

/** Optional boss settings; death-block suppression also applies to other category pools. */
public record BossOptions(double spawnYOffset, PreviewAnimation previewAnimation, DeathBlockRule deathBlocks,
                          boolean hideBossBar, Set<ResourceLocation> phaseMobs,
                          Map<String, Integer> spawnIntegers) {
    public static final BossOptions DEFAULT = new BossOptions(0, PreviewAnimation.DEFAULT);
    public BossOptions(double spawnYOffset, PreviewAnimation previewAnimation) {
        this(spawnYOffset, previewAnimation, DeathBlockRule.NONE, false, Set.of(), Map.of());
    }

    public record PreviewAnimation(String name, double timeSeconds) {
        public static final PreviewAnimation DEFAULT = new PreviewAnimation("", 0);
        public PreviewAnimation {
            if (name == null || name.length() > 128 || !Double.isFinite(timeSeconds)
                    || timeSeconds < 0 || timeSeconds > 300)
                throw new IllegalArgumentException("Invalid boss preview animation");
        }
        public boolean configured() { return !name.isEmpty(); }
    }

    public BossOptions {
        if (!Double.isFinite(spawnYOffset) || spawnYOffset < 0 || spawnYOffset > 16)
            throw new IllegalArgumentException("spawn_y_offset must be 0..16");
        Objects.requireNonNull(previewAnimation);
        Objects.requireNonNull(deathBlocks);
        phaseMobs = Set.copyOf(phaseMobs);
        if (phaseMobs.size() > 32) throw new IllegalArgumentException("At most 32 phase_mobs are allowed");
        spawnIntegers = Map.copyOf(spawnIntegers);
        if (spawnIntegers.size() > 16) throw new IllegalArgumentException("At most 16 spawn_integers are allowed");
    }

    public static boolean isMetadata(JsonObject object) {
        return object.has("spawn_y_offset") || object.has("preview_animation")
                || object.has("suppress_death_blocks") || object.has("hide_boss_bar") || object.has("phase_mobs")
                || object.has("spawn_integers");
    }

    public static BossOptions parse(JsonObject object) {
        for (String key : object.keySet())
            if (!Set.of("spawn_y_offset", "preview_animation", "suppress_death_blocks", "hide_boss_bar", "phase_mobs", "spawn_integers").contains(key))
                throw new JsonParseException("unknown boss option " + key);
        double offset = object.has("spawn_y_offset") ? number(object, "spawn_y_offset") : 0;
        PreviewAnimation preview = PreviewAnimation.DEFAULT;
        if (object.has("preview_animation")) {
            JsonObject animation = object.getAsJsonObject("preview_animation");
            if (!animation.keySet().equals(Set.of("name", "time_seconds"))
                    || !animation.get("name").isJsonPrimitive()
                    || !animation.getAsJsonPrimitive("name").isString()
                    || animation.get("name").getAsString().isBlank())
                throw new JsonParseException("preview_animation requires name and time_seconds");
            preview = new PreviewAnimation(animation.get("name").getAsString(), number(animation, "time_seconds"));
        }
        DeathBlockRule deathBlocks = object.has("suppress_death_blocks")
                ? DeathBlockRule.parse(object.getAsJsonObject("suppress_death_blocks")) : DeathBlockRule.NONE;
        boolean hideBossBar = false;
        if (object.has("hide_boss_bar")) {
            var value = object.get("hide_boss_bar");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
                throw new JsonParseException("hide_boss_bar must be a boolean");
            hideBossBar = value.getAsBoolean();
        }
        Set<ResourceLocation> phases = new LinkedHashSet<>();
        if (object.has("phase_mobs")) {
            if (!object.get("phase_mobs").isJsonArray()) throw new JsonParseException("phase_mobs must be an array");
            for (var entry : object.getAsJsonArray("phase_mobs")) phases.add(FloorParser.id(entry));
        }
        Map<String, Integer> spawn = new LinkedHashMap<>();
        if (object.has("spawn_integers")) {
            if (!object.get("spawn_integers").isJsonObject()) throw new JsonParseException("spawn_integers must be an object");
            for (var entry : object.getAsJsonObject("spawn_integers").entrySet()) {
                if (!entry.getKey().matches("[A-Za-z][A-Za-z0-9_]{0,63}") || !entry.getValue().isJsonPrimitive()
                        || !entry.getValue().getAsJsonPrimitive().isNumber()) throw new JsonParseException("Invalid spawn integer");
                try { spawn.put(entry.getKey(), entry.getValue().getAsBigDecimal().intValueExact()); }
                catch (ArithmeticException ex) { throw new JsonParseException("spawn_integers requires integer values", ex); }
            }
        }
        return new BossOptions(offset, preview, deathBlocks, hideBossBar, phases, spawn);
    }

    private static double number(JsonObject object, String key) {
        var value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new JsonParseException(key + " must be a number");
        return value.getAsDouble();
    }
}
