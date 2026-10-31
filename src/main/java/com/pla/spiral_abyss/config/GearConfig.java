package com.pla.spiral_abyss.config;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.FloorParser;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
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
    public record Assignments(List<String> floors, List<String> endless) {
        public Assignments { floors = List.copyOf(floors); endless = List.copyOf(endless); }
        public String floor(int floor) {
            if (floor < 1 || floor > FloorParser.MAX_FLOORS) throw new IllegalArgumentException("Gear floor must be from 1 to 6");
            return floors.get(floor - 1);
        }
        public String endless(int wave) {
            if (wave < 1) throw new IllegalArgumentException("Endless wave must be positive");
            return endless.get((wave - 1) % 10);
        }
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
            Path assignments = directory.resolve("gear/assignments.json");
            if (!Files.exists(assignments)) Files.writeString(assignments, JSON.toJson(GearDefaults.assignments()) + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) { throw new IllegalStateException("Cannot prepare Spiral Abyss gear configuration", ex); }
    }
    public static Floor load(int floor) {
        return load(FMLPaths.CONFIGDIR.get(), floor);
    }
    public static Floor load(Path configRoot, int floor) {
        if (floor < 1 || floor > FloorParser.MAX_FLOORS) throw new IllegalArgumentException("Gear floor must be from 1 to 6");
        return loadProfile(configRoot, assignments(configRoot).floor(floor), floor);
    }
    public static Floor loadEndless(int wave) {
        return loadEndless(FMLPaths.CONFIGDIR.get(), wave);
    }
    public static Floor loadEndless(Path configRoot, int wave) {
        if (wave < 1) throw new IllegalArgumentException("Endless wave must be positive");
        int defaultFloor = Integer.parseInt(parseAssignments(GearDefaults.assignments()).endless(wave).substring(6));
        return loadProfile(configRoot, assignments(configRoot).endless(wave), defaultFloor);
    }
    private static Floor loadProfile(Path configRoot, String profile, int defaultFloor) {
        Path file = configRoot.resolve("spiral_abyss/gear/" + profile + ".json");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException ex) {
            LogUtils.getLogger().error("Invalid gear file {}; using code-defined defaults for this attempt", file, ex);
            return parse(GearDefaults.forFloor(defaultFloor));
        }
    }
    private static Assignments assignments(Path configRoot) {
        Path file = configRoot.resolve("spiral_abyss/gear/assignments.json");
        if (!Files.exists(file)) return parseAssignments(GearDefaults.assignments());
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parseAssignments(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException ex) {
            LogUtils.getLogger().error("Invalid gear assignments {}; using code-defined assignments for this attempt", file, ex);
            return parseAssignments(GearDefaults.assignments());
        }
    }
    public static Assignments parseAssignments(JsonObject json) {
        JsonObject floors = json.getAsJsonObject("floors");
        List<String> floorProfiles = new ArrayList<>();
        for (int floor = 1; floor <= FloorParser.MAX_FLOORS; floor++)
            floorProfiles.add(profile(floors.get(Integer.toString(floor))));
        JsonArray endless = json.getAsJsonArray("endless");
        if (endless.size() != 10) throw new IllegalArgumentException("Endless gear assignments need exactly ten profile names");
        List<String> endlessProfiles = new ArrayList<>();
        for (JsonElement value : endless) endlessProfiles.add(profile(value));
        return new Assignments(floorProfiles, endlessProfiles);
    }
    private static String profile(JsonElement value) {
        String name = value.getAsString();
        if (!name.matches("[a-z0-9][a-z0-9_-]{0,63}"))
            throw new IllegalArgumentException("Gear profile must be a simple lowercase name, without a path or .json suffix");
        return name;
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
