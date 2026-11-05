package com.pla.spiral_abyss.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Locale;

/** Local presentation preferences; never synchronized from the server. */
public final class AbyssClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue INVENTORY_BUTTON_X;
    public static final ModConfigSpec.IntValue INVENTORY_BUTTON_Y;
    public static final double MIN_HUD_SCALE = 0.5D, MAX_HUD_SCALE = 2.5D;
    public enum HudGroup { TIMER, CHALLENGES, REJECTION }
    private static final EnumMap<HudGroup, HudSettings> HUD = new EnumMap<>(HudGroup.class);

    public static final class HudSettings {
        public final ModConfigSpec.DoubleValue x, y, scale;

        private HudSettings(ModConfigSpec.Builder builder) {
            x = builder.comment("Horizontal anchor: 0 is left, 1 is right, -1 retains the default placement.")
                    .defineInRange("x", -1D, -1D, 1D);
            y = builder.comment("Vertical anchor: 0 is top, 1 is bottom, -1 retains the default placement.")
                    .defineInRange("y", -1D, -1D, 1D);
            scale = builder.comment("Size multiplier relative to the original HUD.")
                    .defineInRange("scale", 1D, MIN_HUD_SCALE, MAX_HUD_SCALE);
        }

        public void reset() {
            x.set(x.getDefault());
            y.set(y.getDefault());
            scale.set(scale.getDefault());
        }
    }

    public static HudSettings hud(HudGroup group) { return HUD.get(group); }

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Position of the Abyss Menu button in survival and creative inventories.",
                "Coordinates use GUI-scaled pixels from the top-left corner of the screen.",
                "Changes apply immediately after the config reloads, including while the inventory is open.",
                "Positions are kept within the visible screen.")
                .push("inventory_button");
        INVENTORY_BUTTON_X = builder.comment("Horizontal position. Increase to move right.")
                .defineInRange("x", 8, 0, Integer.MAX_VALUE);
        INVENTORY_BUTTON_Y = builder.comment("Vertical position. Increase to move down.")
                .defineInRange("y", 8, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.comment("Local Abyss overlay positions and sizes; never sent to the server.",
                "Anchors use the free screen space after accounting for each group's size,",
                "so edge placements remain visible after resizing or changing GUI scale.")
                .push("abyss_overlay");
        for (HudGroup group : HudGroup.values()) {
            builder.push(group.name().toLowerCase(Locale.ROOT));
            HUD.put(group, new HudSettings(builder));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    private AbyssClientConfig() {}
}
