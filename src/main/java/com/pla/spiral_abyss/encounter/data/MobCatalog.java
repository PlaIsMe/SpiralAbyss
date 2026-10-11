package com.pla.spiral_abyss.encounter.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.RewardPool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.Reader;
import java.util.*;

/** Normal datapack priority replaces each pool/eligibility file as a JSON array. */
public final class MobCatalog extends SimplePreparableReloadListener<MobCatalog.Definitions> {
    public record Equipment(Set<ResourceLocation> armor, Set<ResourceLocation> melee,
                            Set<ResourceLocation> bows, Set<ResourceLocation> crossbows, Set<ResourceLocation> tridents, Set<ResourceLocation> ranged) {
        public Equipment {
            armor = Set.copyOf(armor); melee = Set.copyOf(melee); bows = Set.copyOf(bows);
            crossbows = Set.copyOf(crossbows); tridents = Set.copyOf(tridents); ranged = Set.copyOf(ranged);
        }
        public Equipment(Set<ResourceLocation> armor, Set<ResourceLocation> melee, Set<ResourceLocation> bows, Set<ResourceLocation> crossbows, Set<ResourceLocation> tridents) { this(armor, melee, bows, crossbows, tridents, Set.of()); }
        public CombatRole role(ResourceLocation id) {
            // A trident item alone does not install projectile AI. Native ranged mobs are data-defined.
            return bows.contains(id) || crossbows.contains(id) || ranged.contains(id) ? CombatRole.RANGED : CombatRole.MELEE;
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
    public record Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                       Set<ResourceLocation> suppressedSpawns) {
        public Pool { mobs = List.copyOf(mobs); rewards = immutableRewards(rewards); suppressedSpawns = Set.copyOf(suppressedSpawns); }
        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
            this(mobs, rewards, Set.of());
        }
    }
    public record Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                              Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                              Set<ResourceLocation> suppressedSpawns) {
        public Definitions { pools = new Pools(false, pools).entries(); ghostMobs = Set.copyOf(ghostMobs); rewards = immutableRewards(rewards); suppressedSpawns = Set.copyOf(suppressedSpawns); }
        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                           Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
            this(pools, equipment, ghostMobs, rewards, Set.of());
        }
    }
    private static volatile Definitions current = new Definitions(Map.of(),
            new Equipment(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), Set.of(), Map.of());
    public static Equipment equipment() { return current.equipment(); }
    public static boolean isGhostMob(ResourceLocation id) { return current.ghostMobs().contains(id); }
    public static boolean suppressesSpawn(ResourceLocation id) { return id != null && current.suppressedSpawns().contains(id); }
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
                && !id.toString().equals("spiral_abyss:copper_chest") && !id.toString().equals("spiral_abyss:iron_chest")
                && !id.toString().equals("spiral_abyss:golden_chest")
                && !id.toString().equals("spiral_abyss:reward_chest") && !id.toString().equals("spiral_abyss:scroll_entity");
    }

    public static Pool parsePool(JsonArray array) {
        return parsePool(array, ForgeRegistries.ITEMS::containsKey);
    }
    public static Pool parsePool(JsonArray array, java.util.function.Predicate<ResourceLocation> isItem) {
        return parsePool(array, isItem, false);
    }
    public static Pool parseBossPool(JsonArray array, java.util.function.Predicate<ResourceLocation> isItem) {
        return parsePool(array, isItem, true);
    }
    private static Pool parsePool(JsonArray array, java.util.function.Predicate<ResourceLocation> isItem,
                                  boolean bossPool) {
        Set<ResourceLocation> mobs = new LinkedHashSet<>();
        Set<ResourceLocation> configuredEnvironments = new HashSet<>();
        Set<ResourceLocation> suppressedSpawns = new LinkedHashSet<>();
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
                    boolean item = isItem.test(id);
                    var entry = new FloorDefinition.RewardEntry(id, item, 1, 1, 1, item ? RewardPool.RARE_CHANCE : 0.25);
                    bonuses.add(item ? RewardPool.rare(entry) : entry);
                } else {
                    JsonObject object = reward.getAsJsonObject();
                    // Explicit auxiliary entity IDs use Forge's generic join hook; these are
                    // not arbitrary method names or runtime-generated mixin targets.
                    if (object.has("suppress_spawned_entities")) {
                        if (object.size() != 1) throw new JsonParseException("suppress_spawned_entities must be separate from reward/environment metadata");
                        JsonElement targets = object.get("suppress_spawned_entities");
                        if (!targets.isJsonArray() || targets.getAsJsonArray().isEmpty() || targets.getAsJsonArray().size() > 64)
                            throw new JsonParseException("suppress_spawned_entities needs 1-64 entity IDs");
                        for (JsonElement target : targets.getAsJsonArray()) suppressedSpawns.add(FloorParser.id(target));
                        continue;
                    }
                    // Boss terrain metadata shares an entry with reward links, but is parsed
                    // by BossEnvironmentCatalog rather than counted as a reward.
                    if (object.has("environment")) {
                        if (!bossPool) throw new JsonParseException("environment metadata is only supported in mob_pools/boss.json");
                        if (!configuredEnvironments.add(mob)) throw new JsonParseException("duplicate environment for " + mob);
                        for (String key : object.keySet()) if (!Set.of("environment", "decoration_environment",
                                "decoration_chance", "decoration_placement").contains(key))
                            throw new JsonParseException("unknown boss environment metadata field " + key);
                        continue;
                    }
                    var entry = FloorParser.rewardEntry(object, object.has("item") ? RewardPool.RARE_CHANCE : 0.25);
                    bonuses.add(entry.item() ? RewardPool.rare(entry) : entry);
                }
            }
        }
        return new Pool(List.copyOf(mobs), rewards, suppressedSpawns);
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
    private static Set<ResourceLocation> mobType(ResourceManager manager, String key) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (JsonElement value : read(manager, "mob_types/" + key)) ids.add(FloorParser.id(value));
        return ids;
    }
    @Override protected Definitions prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<MobCategory, List<ResourceLocation>> pools = new EnumMap<>(MobCategory.class);
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<>();
        Set<ResourceLocation> suppressedSpawns = new LinkedHashSet<>();
        for (MobCategory category : MobCategory.values()) {
            JsonArray poolEntries = read(manager, "mob_pools/" + category.key());
            Pool pool = category == MobCategory.BOSS
                    ? parseBossPool(poolEntries, ForgeRegistries.ITEMS::containsKey)
                    : parsePool(poolEntries, ForgeRegistries.ITEMS::containsKey);
            pools.put(category, pool.mobs());
            suppressedSpawns.addAll(pool.suppressedSpawns());
            pool.rewards().forEach((id, entries) -> rewards.computeIfAbsent(id, unused -> new ArrayList<>()).addAll(entries));
        }
        return new Definitions(pools, new Equipment(mobType(manager, "armor_mobs"), mobType(manager, "melee_mobs"),
                mobType(manager, "bow_users"), mobType(manager, "crossbow_users"), mobType(manager, "trident_users"), mobType(manager, "ranged_mobs")),
                mobType(manager, "ghost_mobs"), rewards, suppressedSpawns);
    }
    @Override protected void apply(Definitions definitions, ResourceManager manager, ProfilerFiller profiler) {
        current = definitions;
        definitions.pools().forEach((category, ids) -> ids.stream().filter(id -> !availableMob(id))
                .forEach(id -> LogUtils.getLogger().warn("Ignoring unavailable or forbidden Abyss {} mob: {}", category.key(), id)));
    }
}
