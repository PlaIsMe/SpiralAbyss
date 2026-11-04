package com.pla.spiral_abyss.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local presentation preferences; never synchronized from the server. */
public final class AbyssClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue INVENTORY_BUTTON_X;
    public static final ModConfigSpec.IntValue INVENTORY_BUTTON_Y;

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
        SPEC = builder.build();
    }

    private AbyssClientConfig() {}
}
