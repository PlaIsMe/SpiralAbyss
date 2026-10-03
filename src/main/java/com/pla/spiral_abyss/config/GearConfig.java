package com.pla.spiral_abyss.config;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Editable local JSON, read once when an attempt starts. Existing files are never overwritten. */
public final class GearConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private GearConfig() {}
    public record Rule(double chance, int level, List<ResourceLocation> entries) {
        public Rule { entries = List.copyOf(entries); }
    }
    public record Floor(double armorChance, List<List<ResourceLocation>> armorSets, double weaponChance,
                        List<ResourceLocation> weapons, Rule armor, Rule melee, Rule bow, Rule crossbow,
                        Rule trident, int loyaltyLevel) {
        public Floor { armorSets = armorSets.stream().map(List::copyOf).toList(); weapons = List.copyOf(weapons); }
    }
    public static void prepare() {
        prepare(FMLPaths.CONFIGDIR.get());
    }
    public static void prepare(Path configRoot) {
        Path directory = configRoot.resolve("spiral_abyss");
        try {
            Files.createDirectories(directory.resolve("gear"));
            Path old = configRoot.resolve("spiral_abyss-common.toml");
            Path current = directory.resolve("spiral_abyss-common.toml");
            if (!Files.exists(current) && Files.isRegularFile(old)) Files.copy(old, current);
            for (int floor = 1; floor <= FloorParser.MAX_FLOORS; floor++) {
                Path file = directory.resolve("gear/floor_" + floor + ".json");
                if (!Files.exists(file)) Files.writeString(file, JSON.toJson(GearDefaults.forFloor(floor)) + "\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException ex) { throw new IllegalStateException("Cannot prepare Spiral Abyss gear configuration", ex); }
    }
    public static Floor load(int floor) {
        return load(FMLPaths.CONFIGDIR.get(), floor);
    }
    public static Floor load(Path configRoot, int floor) {
        if (floor < 1 || floor > FloorParser.MAX_FLOORS) throw new IllegalArgumentException("Gear floor must be from 1 to 6");
        Path file = configRoot.resolve("spiral_abyss/gear/floor_" + floor + ".json");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException ex) {
            LogUtils.getLogger().error("Invalid gear file {}; using code-defined defaults for this attempt", file, ex);
            return parse(GearDefaults.forFloor(floor));
        }
    }
    public static Floor parse(JsonObject json) {
        JsonObject armor = json.getAsJsonObject("armor"), equipment = json.getAsJsonObject("equipment");
        List<List<ResourceLocation>> sets = new ArrayList<>();
        for (JsonElement value : armor.getAsJsonArray("sets")) {
            var set = ids(value.getAsJsonArray());
            if (set.size() != 4) throw new IllegalArgumentException("Each armor set needs helmet, chestplate, leggings, boots, in that order");
            sets.add(set);
        }
        JsonObject enchant = json.getAsJsonObject("enchantments");
        return new Floor(chance(armor), sets, chance(equipment), ids(equipment.getAsJsonArray("items")),
                rule(enchant.getAsJsonObject("armor")), rule(enchant.getAsJsonObject("melee")),
                rule(enchant.getAsJsonObject("bow")), rule(enchant.getAsJsonObject("crossbow")),
                rule(enchant.getAsJsonObject("trident")), integer(enchant.getAsJsonObject("trident"), "loyalty_level", 1, 3));
    }
    private static Rule rule(JsonObject json) {
        return new Rule(chance(json), integer(json, "level", 1, 255), ids(json.getAsJsonArray("entries")));
    }
    private static int integer(JsonObject json, String key, int min, int max) {
        double value = json.get(key).getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < min || value > max)
            throw new IllegalArgumentException(key + " must be an integer from " + min + " to " + max);
        return (int) value;
    }
    private static double chance(JsonObject json) {
        double value = json.get("chance").getAsDouble();
        if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("chance must be from 0 to 1");
        return value;
    }
    private static List<ResourceLocation> ids(JsonArray array) {
        List<ResourceLocation> result = new ArrayList<>();
        for (JsonElement entry : array) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id == null) throw new IllegalArgumentException("Invalid registry ID: " + entry);
            result.add(id);
        }
        return List.copyOf(result);
    }
}
