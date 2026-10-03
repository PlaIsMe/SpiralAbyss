package com.pla.spiral_abyss.config;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Mob pools are captured per global cycle; equipment settings are captured per attempt. */
public final class AbyssConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue MIX_MOBS;
    public static final ForgeConfigSpec.BooleanValue CAN_EQUIP_GEAR;
    public static final ForgeConfigSpec.IntValue RESET_DAYS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARMOR_MOBS, MELEE_MOBS, BOW_MOBS, CROSSBOW_MOBS, TRIDENT_MOBS;
    private static final Map<MobCategory, ForgeConfigSpec.ConfigValue<List<? extends String>>> POOLS = new EnumMap<>(MobCategory.class);
    static {
        var builder = new ForgeConfigSpec.Builder();
        RESET_DAYS = builder.comment("Server-wide reset interval in real-world days, including server downtime.",
                "0 disables automatic resets. Abyss Keys never change this schedule or the cycle's mob choices.")
                .defineInRange("reset_days", 20, 0, 36500);
        MIX_MOBS = builder.comment("False: pick one entity ID per category per wave and repeat it for the count.",
                "True: pick an entity ID independently for every spawn, including across mods.")
                .define("mix_mobs", false);
        builder.push("categories");
        for (MobCategory category : MobCategory.values()) {
            POOLS.put(category, builder.comment("Entity registry IDs for " + category.key() + ". Installed modded Mob entities are supported.")
                    .defineListAllowEmpty(List.of(category.key()), category.defaults(),
                            value -> value instanceof String id && ResourceLocation.tryParse(id) != null));
        }
        builder.pop();
        builder.push("equipment");
        CAN_EQUIP_GEAR = builder.comment("Enable floor gear and enchantments. Gear files are in config/spiral_abyss/gear.")
                .define("can_equip_gear", true);
        ARMOR_MOBS = mobList(builder, "armor_mobs", "zombie", "husk", "zombie_villager", "drowned", "skeleton", "stray", "piglin", "piglin_brute", "wither_skeleton");
        MELEE_MOBS = mobList(builder, "melee_mobs", "zombie", "husk", "zombie_villager", "piglin", "piglin_brute", "wither_skeleton", "vindicator");
        BOW_MOBS = mobList(builder, "bow_users", "skeleton", "stray");
        CROSSBOW_MOBS = mobList(builder, "crossbow_users", "pillager");
        TRIDENT_MOBS = mobList(builder, "trident_users", "drowned");
        builder.pop();
        SPEC = builder.build();
    }
    private AbyssConfig() {}

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> mobList(ForgeConfigSpec.Builder builder, String key, String... defaults) {
        return builder.comment("Entity IDs allowed to use this equipment; modded IDs are supported.")
                .defineListAllowEmpty(List.of(key), Arrays.stream(defaults).map(id -> "minecraft:" + id).toList(),
                        value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
    }
    public record Equipment(boolean enabled, Set<ResourceLocation> armor, Set<ResourceLocation> melee,
                            Set<ResourceLocation> bows, Set<ResourceLocation> crossbows, Set<ResourceLocation> tridents) {
        public Equipment {
            armor = Set.copyOf(armor); melee = Set.copyOf(melee); bows = Set.copyOf(bows);
            crossbows = Set.copyOf(crossbows); tridents = Set.copyOf(tridents);
        }
    }
    private static Set<ResourceLocation> ids(ForgeConfigSpec.ConfigValue<List<? extends String>> config) {
        return config.get().stream().map(ResourceLocation::new).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public static Equipment equipment() {
        return new Equipment(CAN_EQUIP_GEAR.get(), ids(ARMOR_MOBS), ids(MELEE_MOBS), ids(BOW_MOBS), ids(CROSSBOW_MOBS), ids(TRIDENT_MOBS));
    }

    public record Pools(boolean mixMobs, Map<MobCategory, List<ResourceLocation>> entries) {
        public Pools {
            var copy = new EnumMap<MobCategory, List<ResourceLocation>>(MobCategory.class);
            entries.forEach((category, ids) -> copy.put(category, List.copyOf(ids)));
            entries = Collections.unmodifiableMap(copy);
        }
        public Pools available(java.util.function.Predicate<ResourceLocation> installed) {
            Map<MobCategory, List<ResourceLocation>> valid = new EnumMap<>(MobCategory.class);
            entries.forEach((category, ids) -> valid.put(category, ids.stream().filter(installed).toList()));
            return new Pools(mixMobs, valid);
        }
    }
    public static Pools snapshot() {
        Map<MobCategory, List<ResourceLocation>> result = new EnumMap<>(MobCategory.class);
        POOLS.forEach((category, config) -> {
            List<ResourceLocation> valid = new ArrayList<>();
            for (String value : config.get()) {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)
                        || id.toString().equals("minecraft:player") || id.toString().equals("spiral_abyss:key_entity")
                        || id.toString().equals("spiral_abyss:reward_chest") || id.toString().equals("spiral_abyss:scroll_entity")) {
                    LogUtils.getLogger().warn("Ignoring unavailable or forbidden Abyss {} entry: {}", category.key(), value);
                    continue;
                }
                if (!valid.contains(id)) valid.add(id);
            }
            result.put(category, valid);
        });
        return new Pools(MIX_MOBS.get(), result);
    }
    /** Saved cycles may outlive optional mods. Filter again without changing their persisted pool order. */
    public static boolean availableMob(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id)
                && !id.toString().equals("minecraft:player") && !id.toString().equals("spiral_abyss:key_entity")
                && !id.toString().equals("spiral_abyss:reward_chest") && !id.toString().equals("spiral_abyss:scroll_entity");
    }
}
