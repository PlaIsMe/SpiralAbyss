package com.pla.spiral_abyss.encounter.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.AbyssConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.Reader;
import java.util.*;

/** Normal datapack priority replaces each pool/eligibility file as a JSON array. */
public final class MobCatalog extends SimplePreparableReloadListener<MobCatalog.Definitions> {
    public record Equipment(Set<ResourceLocation> armor, Set<ResourceLocation> melee,
                            Set<ResourceLocation> bows, Set<ResourceLocation> crossbows, Set<ResourceLocation> tridents) {
        public Equipment {
            armor = Set.copyOf(armor); melee = Set.copyOf(melee); bows = Set.copyOf(bows);
            crossbows = Set.copyOf(crossbows); tridents = Set.copyOf(tridents);
        }
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
    public record Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
        public Pool { mobs = List.copyOf(mobs); rewards = immutableRewards(rewards); }
    }
    public record Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                              Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
        public Definitions { pools = new Pools(false, pools).entries(); rewards = immutableRewards(rewards); }
    }
    private static volatile Definitions current = new Definitions(Map.of(),
            new Equipment(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), Map.of());
    public static Equipment equipment() { return current.equipment(); }
    public static Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards() { return current.rewards(); }
    public static Pools snapshot() { return new Pools(AbyssConfig.MIX_MOBS.get(), current.pools()).available(MobCatalog::availableMob); }

    private static Map<ResourceLocation, List<FloorDefinition.RewardEntry>> immutableRewards(
            Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> copy = new LinkedHashMap<>();
        rewards.forEach((id, entries) -> copy.put(id, List.copyOf(entries)));
        return Collections.unmodifiableMap(copy);
    }
    public static boolean availableMob(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id)
                && !id.toString().equals("minecraft:player") && !id.toString().equals("spiral_abyss:key_entity")
                && !id.toString().equals("spiral_abyss:reward_chest") && !id.toString().equals("spiral_abyss:scroll_entity");
    }

    public static Pool parsePool(JsonArray array) {
        return parsePool(array, ForgeRegistries.ITEMS::containsKey);
    }
    public static Pool parsePool(JsonArray array, java.util.function.Predicate<ResourceLocation> isItem) {
        Set<ResourceLocation> mobs = new LinkedHashSet<>();
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<>();
        for (JsonElement value : array) {
            JsonArray spec = value.isJsonArray() ? value.getAsJsonArray() : null;
            if (spec != null && (spec.size() == 0 || spec.size() > 65))
                throw new JsonParseException("A mob array needs an entity ID and at most 64 rewards");
            ResourceLocation mob = FloorParser.id(spec == null ? value : spec.get(0));
            mobs.add(mob);
            if (spec == null) continue;
            List<FloorDefinition.RewardEntry> bonuses = rewards.computeIfAbsent(mob, id -> new ArrayList<>());
            for (int index = 1; index < spec.size(); index++) {
                JsonElement reward = spec.get(index);
                // Strings resolve to installed items, otherwise chest table IDs. Objects remove ambiguity.
                if (reward.isJsonPrimitive()) {
                    ResourceLocation id = FloorParser.id(reward);
                    bonuses.add(new FloorDefinition.RewardEntry(id, isItem.test(id), 1, 1, 1, 0.25));
                } else bonuses.add(FloorParser.rewardEntry(reward.getAsJsonObject(), 0.25));
            }
        }
        return new Pool(List.copyOf(mobs), rewards);
    }
    private static JsonArray read(ResourceManager manager, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("spiral_abyss", path + ".json");
        var resource = manager.getResource(id);
        if (resource.isEmpty()) return new JsonArray();
        try (Reader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception ex) {
            throw new JsonParseException("Invalid " + id, ex);
        }
    }
    private static Set<ResourceLocation> equipment(ResourceManager manager, String key) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (JsonElement value : read(manager, "mob_equipment/" + key)) ids.add(FloorParser.id(value));
        return ids;
    }
    @Override protected Definitions prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<MobCategory, List<ResourceLocation>> pools = new EnumMap<>(MobCategory.class);
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<>();
        for (MobCategory category : MobCategory.values()) {
            Pool pool = parsePool(read(manager, "mob_pools/" + category.key()));
            pools.put(category, pool.mobs());
            pool.rewards().forEach((id, entries) -> rewards.computeIfAbsent(id, unused -> new ArrayList<>()).addAll(entries));
        }
        return new Definitions(pools, new Equipment(equipment(manager, "armor_mobs"), equipment(manager, "melee_mobs"),
                equipment(manager, "bow_users"), equipment(manager, "crossbow_users"), equipment(manager, "trident_users")), rewards);
    }
    @Override protected void apply(Definitions definitions, ResourceManager manager, ProfilerFiller profiler) {
        current = definitions;
        definitions.pools().forEach((category, ids) -> ids.stream().filter(id -> !availableMob(id))
                .forEach(id -> LogUtils.getLogger().warn("Ignoring unavailable or forbidden Abyss {} mob: {}", category.key(), id)));
    }
}
