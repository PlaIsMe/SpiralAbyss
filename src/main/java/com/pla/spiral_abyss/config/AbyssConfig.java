package com.pla.spiral_abyss.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server policy only; mob pools and equipment eligibility are datapack definitions. */
public final class AbyssConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue MIX_MOBS;
    public static final ForgeConfigSpec.IntValue RESET_DAYS;
    static {
        var builder = new ForgeConfigSpec.Builder();
        RESET_DAYS = builder.comment("Server-wide reset interval in real-world days, including server downtime.",
                "0 disables automatic resets. Personal resets never change this schedule or the cycle's mob choices.")
                .defineInRange("reset_days", 20, 0, 36500);
        MIX_MOBS = builder.comment("False: pick one entity ID per category per wave and repeat it for the count.",
                "True: pick an entity ID independently for every spawn, including across mods.")
                .define("mix_mobs", false);
        SPEC = builder.build();
    }
    private AbyssConfig() {}

}
