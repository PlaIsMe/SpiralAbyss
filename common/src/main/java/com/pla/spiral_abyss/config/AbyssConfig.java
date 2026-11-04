package com.pla.spiral_abyss.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class AbyssConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue MIX_MOBS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_BLACKLIST;
    public static final ModConfigSpec.IntValue RESET_DAYS;
    public static final ModConfigSpec.BooleanValue BROADCAST_RARE_LOOT;
    public static final ModConfigSpec.DoubleValue RARE_REWARD_CHANCE;
    public static final ModConfigSpec.BooleanValue ABYSSAL_REJECTION;
    static {
        var builder = new ModConfigSpec.Builder();
        ABYSSAL_REJECTION = builder.comment("Reduce a player's damage to floor 5/6 bosses by 60% until they defeat that species outside the Abyss.",
                "Preserves normal boss progression. Disable for packs that intentionally allow Abyss-first boss encounters.")
                .define("abyssal_rejection", true);
        RESET_DAYS = builder.comment("Server-wide reset interval in real-world days, including server downtime.",
                "0 disables automatic resets. Personal resets never change this schedule or the cycle's mob choices.")
                .defineInRange("reset_days", 20, 0, 36500);
        MIX_MOBS = builder.comment("False: pick one entity ID per category per wave and repeat it for the count.",
                "True: pick an entity ID independently for every spawn, including across mods.")
                .define("mix_mobs", false);
        MOB_BLACKLIST = builder.comment("Entity IDs excluded from all Abyss wave selections, including bosses and mini-bosses in normal and Endless modes.",
                "Example: [\"minecraft:wither\", \"annoyingvillagers:herobrine_clone\"]. Empty allows all configured mobs.")
                .defineListAllowEmpty("mob_blacklist", List.of(), value -> value instanceof String id
                        && ResourceLocation.tryParse(id) != null && id.equals(ResourceLocation.tryParse(id).toString()));
        BROADCAST_RARE_LOOT = builder.comment("Announce successful pickups of rare gold-trail Abyss rewards to every online player.")
                .define("broadcast_rare_loot", true);
        RARE_REWARD_CHANCE = builder.comment("Base rare reward probability per eligible player and mob-linked item roll in a Golden Chest.")
                .defineInRange("rare_reward_chance", 0.05, 0.0, 1.0);
        SPEC = builder.build();
    }
    private AbyssConfig() {}

}
