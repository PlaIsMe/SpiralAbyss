package com.pla.spiral_abyss.encounter.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;

/** Reads the requested data/spiral_abyss/floor_N layout using normal pack priority. */
public final class FloorCatalog extends SimplePreparableReloadListener<Map<Integer, FloorDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern FILE = Pattern.compile("floor_([1-9][0-9]*)/(floor|wave_([1-9][0-9]*))\\.json");
    private static volatile Snapshot current = new Snapshot(0, Map.of());
    private static volatile Map<ResourceLocation, FloorDefinition.WaveType> templates = Map.of();

    public record Snapshot(int revision, Map<Integer, FloorDefinition> floors) {
        public Snapshot { floors = Map.copyOf(floors); }
    }

    public static Snapshot snapshot() { return current; }
    public static Map<ResourceLocation, FloorDefinition.WaveType> waveTypes() { return templates; }

    @Override
    protected Map<Integer, FloorDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, FloorDefinition.WaveType> waveTypes = new HashMap<>();
        manager.listResources("wave_types", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring("wave_types/".length(), id.getPath().length() - ".json".length()));
            try (Reader reader = resource.openAsReader()) {
                waveTypes.put(name, FloorParser.waveType(name, JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception ex) { LOGGER.error("Invalid wave type {}: {}", id, ex.getMessage()); }
        });
        templates = Map.copyOf(waveTypes);
        Map<Integer, JsonObject> settings = new HashMap<>();
        Map<Integer, SortedMap<Integer, JsonObject>> waves = new HashMap<>();
        Set<Integer> invalid = new HashSet<>();
        // Forge pack resources reject an empty root path. Enumerate the six explicit folders.
        Map<ResourceLocation, Resource> resources = new HashMap<>();
        for (int number = 1; number <= FloorParser.MAX_FLOORS; number++) {
            resources.putAll(manager.listResources("floor_" + number,
                    id -> id.getNamespace().equals("spiral_abyss") && FILE.matcher(id.getPath()).matches()));
        }
        resources.forEach((id, resource) -> {
            var match = FILE.matcher(id.getPath());
            if (!match.matches()) return;
            int floor;
            try { floor = Integer.parseInt(match.group(1)); }
            catch (NumberFormatException ex) { LOGGER.error("Ignoring invalid floor path {}", id); return; }
            if (floor > FloorParser.MAX_FLOORS) {
                LOGGER.error("Ignoring {}: maximum floor is 6", id);
                return;
            }
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject()) throw new JsonParseException("file must contain an object");
                if (match.group(3) == null) settings.put(floor, json.getAsJsonObject());
                else waves.computeIfAbsent(floor, n -> new TreeMap<>())
                        .put(Integer.parseInt(match.group(3)), json.getAsJsonObject());
            } catch (Exception ex) {
                invalid.add(floor);
                LOGGER.error("Invalid floor resource {}: {}", id, ex.getMessage());
            }
        });
        Map<Integer, FloorDefinition> result = new HashMap<>();
        Set<Integer> numbers = new HashSet<>(settings.keySet());
        numbers.addAll(waves.keySet());
        for (int number : numbers) {
            if (invalid.contains(number)) continue;
            try {
                if (!settings.containsKey(number)) throw new JsonParseException("missing floor.json");
                result.put(number, FloorParser.parse(number, settings.get(number), waves.getOrDefault(number, new TreeMap<>()), waveTypes));
            } catch (Exception ex) {
                LOGGER.error("Floor {} disabled: {}", number, ex.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    @Override
    protected void apply(Map<Integer, FloorDefinition> floors, ResourceManager manager, ProfilerFiller profiler) {
        current = new Snapshot(current.revision() + 1, floors);
        LOGGER.info("Loaded SpiralAbyss floors: {}", new TreeSet<>(floors.keySet()));
    }
}
