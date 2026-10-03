package com.pla.spiral_abyss.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/** Code-defined defaults for files in config/spiral_abyss/gear; no resource-pack dependency. */
public final class GearDefaults {
    private GearDefaults() {}

    public static JsonObject forFloor(int floor) {
        List<String> materials = switch (floor) {
            case 1 -> List.of();
            case 2 -> List.of("leather");
            case 3 -> List.of("golden", "chainmail");
            case 4 -> List.of("iron");
            case 5 -> List.of("diamond");
            case 6 -> List.of("netherite");
            default -> throw new IllegalArgumentException("Gear floor must be from 1 to 6");
        };
        String weaponMaterial = floor == 2 ? "wooden" : materials.isEmpty() ? null : materials.get(0);
        double chance = (floor - 1) / 5.0;
        int level = Math.max(1, floor - 1);
        double rangedChance = floor >= 4 ? chance : 0;
        int rangedLevel = Math.max(1, floor - 3);

        JsonArray sets = new JsonArray();
        for (String material : materials) sets.add(items(material, "helmet", "chestplate", "leggings", "boots"));
        JsonObject armor = new JsonObject();
        armor.addProperty("chance", chance); armor.add("sets", sets);
        JsonObject equipment = new JsonObject();
        equipment.addProperty("chance", chance);
        equipment.add("items", weaponMaterial == null ? new JsonArray()
                : items(weaponMaterial, "sword", "pickaxe", "axe", "shovel", "hoe"));

        JsonObject enchantments = new JsonObject();
        enchantments.add("armor", rule(chance, level,
                "protection", "fire_protection", "blast_protection", "projectile_protection", "unbreaking"));
        enchantments.add("melee", rule(chance, level, "sharpness", "fire_aspect", "knockback", "unbreaking"));
        enchantments.add("bow", rule(rangedChance, rangedLevel, "power", "flame"));
        enchantments.add("crossbow", rule(rangedChance, rangedLevel, "quick_charge", "multishot", "piercing"));
        JsonObject trident = rule(chance, level, "impaling", "unbreaking");
        trident.addProperty("loyalty_level", rangedLevel);
        enchantments.add("trident", trident);

        JsonObject result = new JsonObject();
        result.add("armor", armor); result.add("equipment", equipment); result.add("enchantments", enchantments);
        return result;
    }

    private static JsonArray items(String material, String... slots) {
        JsonArray result = new JsonArray();
        for (String slot : slots) result.add("minecraft:" + material + "_" + slot);
        return result;
    }
    private static JsonObject rule(double chance, int level, String... enchantments) {
        JsonObject rule = new JsonObject();
        rule.addProperty("chance", chance); rule.addProperty("level", level);
        JsonArray entries = new JsonArray();
        for (String enchantment : enchantments) entries.add("minecraft:" + enchantment);
        rule.add("entries", entries);
        return rule;
    }
}
