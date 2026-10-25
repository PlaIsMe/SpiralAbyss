package com.pla.spiral_abyss.encounter.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Set;

/** Optional boss settings; death-block suppression also applies to other category pools. */
public record BossOptions(double spawnYOffset, PreviewAnimation previewAnimation, DeathBlockRule deathBlocks,
                          boolean hideBossBar) {
    public static final BossOptions DEFAULT = new BossOptions(0, PreviewAnimation.DEFAULT);
    public BossOptions(double spawnYOffset, PreviewAnimation previewAnimation) {
        this(spawnYOffset, previewAnimation, DeathBlockRule.NONE, false);
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
        java.util.Objects.requireNonNull(previewAnimation);
        java.util.Objects.requireNonNull(deathBlocks);
    }

    public static boolean isMetadata(JsonObject object) {
        return object.has("spawn_y_offset") || object.has("preview_animation")
                || object.has("suppress_death_blocks") || object.has("hide_boss_bar");
    }

    public static BossOptions parse(JsonObject object) {
        for (String key : object.keySet())
            if (!Set.of("spawn_y_offset", "preview_animation", "suppress_death_blocks", "hide_boss_bar").contains(key))
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
        return new BossOptions(offset, preview, deathBlocks, hideBossBar);
    }

    private static double number(JsonObject object, String key) {
        var value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new JsonParseException(key + " must be a number");
        return value.getAsDouble();
    }
}
