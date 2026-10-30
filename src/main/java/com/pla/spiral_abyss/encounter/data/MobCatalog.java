package com.pla.spiral_abyss.encounter.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.RewardPool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;
import java.io.Reader;
import java.util.*;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import java.util.function.Predicate;

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
        public Pools available(Predicate<ResourceLocation> installed) {
            Map<MobCategory, List<ResourceLocation>> valid = new EnumMap<>(MobCategory.class);
            entries.forEach((category, ids) -> valid.put(category, ids.stream().filter(installed).toList()));
            return new Pools(mixMobs, valid);
        }
    }
    public record Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                       Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks,
                       Map<ResourceLocation, MobScaling.Plan> scaling,
                       MobScaling.Plan defaultScaling, Map<ResourceLocation, BossOptions> bossOptions) {
        public Pool {
            mobs = List.copyOf(mobs); rewards = immutableRewards(rewards);
            suppressedSpawns = Set.copyOf(suppressedSpawns);
            suppressedFallingBlocks = immutableSets(suppressedFallingBlocks);
            scaling = Map.copyOf(scaling);
            bossOptions = Map.copyOf(bossOptions);
        }
        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                    Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks,
                    Map<ResourceLocation, MobScaling.Plan> scaling, MobScaling.Plan defaultScaling) {
            this(mobs, rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, Map.of());
        }
        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
            this(mobs, rewards, Set.of(), Map.of(), Map.of(), MobScaling.Plan.NORMAL_PLAN);
        }
        public Pool(List<ResourceLocation> mobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                    Set<ResourceLocation> suppressedSpawns) {
            this(mobs, rewards, suppressedSpawns, Map.of(), Map.of(), MobScaling.Plan.NORMAL_PLAN);
        }
    }
    public record Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                              Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                              Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks,
                              Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> scaling,
                              Map<MobCategory, MobScaling.Plan> defaultScaling, Map<ResourceLocation, BossOptions> bossOptions) {
        public Definitions {
            pools = new Pools(false, pools).entries(); ghostMobs = Set.copyOf(ghostMobs);
            rewards = immutableRewards(rewards); suppressedSpawns = Set.copyOf(suppressedSpawns);
            suppressedFallingBlocks = immutableSets(suppressedFallingBlocks);
            var copy = new EnumMap<MobCategory, Map<ResourceLocation, MobScaling.Plan>>(MobCategory.class);
            scaling.forEach((category, plans) -> copy.put(category, Map.copyOf(plans)));
            scaling = Collections.unmodifiableMap(copy);
            defaultScaling = Map.copyOf(defaultScaling);
            bossOptions = Map.copyOf(bossOptions);
        }
        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                           Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                           Set<ResourceLocation> suppressedSpawns, Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks,
                           Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> scaling,
                           Map<MobCategory, MobScaling.Plan> defaultScaling) {
            this(pools, equipment, ghostMobs, rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, Map.of());
        }
        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                           Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
            this(pools, equipment, ghostMobs, rewards, Set.of(), Map.of(), Map.of(), Map.of());
        }
        public Definitions(Map<MobCategory, List<ResourceLocation>> pools, Equipment equipment,
                           Set<ResourceLocation> ghostMobs, Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards,
                           Set<ResourceLocation> suppressedSpawns) {
            this(pools, equipment, ghostMobs, rewards, suppressedSpawns, Map.of(), Map.of(), Map.of());
        }
    }
    public record ScalingSnapshot(Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> plans,
                                  Map<MobCategory, MobScaling.Plan> defaults) {
        public static final ScalingSnapshot EMPTY = new ScalingSnapshot(Map.of(), Map.of());
        public MobScaling.Floor at(MobCategory category, ResourceLocation mob, int floor) {
            if (category == null) return MobScaling.Plan.NORMAL;
            MobScaling.Plan fallback = defaults.getOrDefault(category, MobScaling.Plan.NORMAL_PLAN);
            return plans.getOrDefault(category, Map.of()).getOrDefault(mob, fallback).at(floor);
        }
    }
    private static volatile Definitions current = new Definitions(Map.of(),
            new Equipment(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), Set.of(), Map.of());
    public static Equipment equipment() { return current.equipment(); }
    public static BossOptions bossOptions(ResourceLocation id) { return current.bossOptions().getOrDefault(id, BossOptions.DEFAULT); }
    public static boolean isBoss(ResourceLocation id) {
        return id != null && current.pools().getOrDefault(MobCategory.BOSS, List.of()).contains(id);
    }
    public static boolean isGhostMob(ResourceLocation id) { return current.ghostMobs().contains(id); }
    public static boolean suppressesSpawn(ResourceLocation id) { return id != null && current.suppressedSpawns().contains(id); }
    public static Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks() { return current.suppressedFallingBlocks(); }
    public static Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards() { return current.rewards(); }
    public static ScalingSnapshot scalingSnapshot() {
        Definitions definitions = current;
        return new ScalingSnapshot(definitions.scaling(), definitions.defaultScaling());
    }
    public static Pools snapshot() { return new Pools(AbyssConfig.MIX_MOBS.get(), current.pools()).available(MobCatalog::selectableMob); }

    private static Map<ResourceLocation, List<FloorDefinition.RewardEntry>> immutableRewards(
            Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards) {
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> copy = new LinkedHashMap<>();
        rewards.forEach((id, entries) -> copy.put(id, List.copyOf(entries)));
        return Collections.unmodifiableMap(copy);
    }
    private static Map<ResourceLocation, Set<ResourceLocation>> immutableSets(Map<ResourceLocation, Set<ResourceLocation>> values) {
        Map<ResourceLocation, Set<ResourceLocation>> copy = new LinkedHashMap<>();
        values.forEach((id, entries) -> copy.put(id, Set.copyOf(entries)));
        return Collections.unmodifiableMap(copy);
    }
    /** Server encounter policy is separate from registry checks used for metadata and missing-ID warnings. */
    public static boolean selectableMob(ResourceLocation id) {
        return availableMob(id) && !AbyssConfig.MOB_BLACKLIST.get().contains(id.toString());
    }
    public static boolean availableMob(ResourceLocation id) {
        return ForgeRegistries.ENTITY_TYPES.containsKey(id)
                && !id.toString().equals("minecraft:player") && !id.toString().equals("spiral_abyss:key_entity")
                && !id.toString().equals("spiral_abyss:copper_chest") && !id.toString().equals("spiral_abyss:iron_chest")
                && !id.toString().equals("spiral_abyss:golden_chest")
                && !id.toString().equals("spiral_abyss:experience_chest")
                && !id.toString().equals("spiral_abyss:reward_chest");
    }

    public static Pool parsePool(JsonArray array) {
        return parsePool(array, ForgeRegistries.ITEMS::containsKey);
    }
    public static Pool parsePool(JsonArray array, Predicate<ResourceLocation> isItem) {
        return parsePool(array, isItem, false);
    }
    public static Pool parseBossPool(JsonArray array, Predicate<ResourceLocation> isItem) {
        return parsePool(array, isItem, true);
    }
    private static Pool parsePool(JsonArray array, Predicate<ResourceLocation> isItem,
                                  boolean bossPool) {
        Set<ResourceLocation> mobs = new LinkedHashSet<>();
        Set<ResourceLocation> configuredEnvironments = new HashSet<>();
        Set<ResourceLocation> suppressedSpawns = new LinkedHashSet<>();
        Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks = new LinkedHashMap<>();
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<>();
        MobScaling.Plan defaultScaling = MobScaling.Plan.NORMAL_PLAN;
        boolean hasDefaultScaling = false;
        for (JsonElement value : array) if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.size() != 1 || !object.has("scaling") || hasDefaultScaling)
                throw new JsonParseException("pool-level object must be one scaling definition");
            defaultScaling = MobScaling.parse(object.get("scaling"), MobScaling.Plan.NORMAL_PLAN);
            hasDefaultScaling = true;
        }
        Map<ResourceLocation, MobScaling.Plan> scaling = new LinkedHashMap<>();
        Map<ResourceLocation, BossOptions> bossOptions = new LinkedHashMap<>();
        for (JsonElement value : array) {
            if (value.isJsonObject()) continue;
            JsonArray spec = value.isJsonArray() ? value.getAsJsonArray() : null;
            if (spec != null && (spec.size() == 0 || spec.size() > 65))
                throw new JsonParseException("A mob array needs an entity ID and at most 64 rewards");
            ResourceLocation mob = FloorParser.id(spec == null ? value : spec.get(0));
            mobs.add(mob);
            MobScaling.Plan mobScale = defaultScaling;
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
                    if (BossOptions.isMetadata(object)) {
                        // Death rewards can place blocks for ordinary mobs/minibosses too.
                        // Other options still belong exclusively to the boss pool.
                        if (!bossPool && !object.keySet().equals(Set.of("suppress_death_blocks")))
                            throw new JsonParseException("only suppress_death_blocks is supported outside mob_pools/boss.json");
                        if (bossOptions.putIfAbsent(mob, BossOptions.parse(object)) != null)
                            throw new JsonParseException("duplicate boss options for " + mob);
                        continue;
                    }
                    if (object.has("scaling")) {
                        if (object.size() != 1 || scaling.containsKey(mob))
                            throw new JsonParseException("mob scaling must be a separate, unique metadata object for " + mob);
                        mobScale = MobScaling.parse(object.get("scaling"), defaultScaling);
                        scaling.put(mob, mobScale);
                        continue;
                    }
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
                    if (object.has("suppress_falling_blocks")) {
                        if (object.size() != 1) throw new JsonParseException("suppress_falling_blocks must be separate from reward/environment metadata");
                        JsonElement targets = object.get("suppress_falling_blocks");
                        if (!targets.isJsonArray() || targets.getAsJsonArray().isEmpty() || targets.getAsJsonArray().size() > 64)
                            throw new JsonParseException("suppress_falling_blocks needs 1-64 block IDs");
                        Set<ResourceLocation> blocks = suppressedFallingBlocks.computeIfAbsent(mob, unused -> new LinkedHashSet<>());
                        for (JsonElement target : targets.getAsJsonArray()) blocks.add(FloorParser.id(target));
                        continue;
                    }
                    // Boss terrain metadata shares an entry with reward links, but is parsed
                    // by BossEnvironmentCatalog rather than counted as a reward.
                    if (BossEnvironmentCatalog.isMetadata(object)) {
                        if (!bossPool) throw new JsonParseException("environment metadata is only supported in mob_pools/boss.json");
                        if (!configuredEnvironments.add(mob)) throw new JsonParseException("duplicate environment for " + mob);
                        for (String key : object.keySet()) if (!Set.of("environment", "decoration_environment",
                                "decoration_chance", "decoration_placement", "entity_place", "block_place").contains(key))
                            throw new JsonParseException("unknown boss environment metadata field " + key);
                        continue;
                    }
                    var entry = FloorParser.rewardEntry(object, object.has("item") ? RewardPool.RARE_CHANCE : 0.25);
                    bonuses.add(entry.item() ? RewardPool.rare(entry) : entry);
                }
            }
            scaling.putIfAbsent(mob, mobScale);
        }
        for (ResourceLocation mob : mobs) scaling.putIfAbsent(mob, defaultScaling);
        return new Pool(List.copyOf(mobs), rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, bossOptions);
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
        for (JsonElement value : read(manager, "mob_types/" + key)) {
            ResourceLocation id = FloorParser.id(value);
            if (availableMob(id)) ids.add(id);
            else warnUnavailableMob(id, "data/spiral_abyss/mob_types/" + key + ".json");
        }
        return ids;
    }
    static void warnUnavailableMob(ResourceLocation id, String source) {
        if (id.getNamespace().equals("minecraft") || ModList.get().isLoaded(id.getNamespace()))
            LogUtils.getLogger().warn("[Spiral Abyss] Ignoring unavailable or forbidden mob {} in {}; check the configured entity ID.", id, source);
    }
    @Override protected Definitions prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<MobCategory, List<ResourceLocation>> pools = new EnumMap<>(MobCategory.class);
        Map<ResourceLocation, List<FloorDefinition.RewardEntry>> rewards = new LinkedHashMap<>();
        Set<ResourceLocation> suppressedSpawns = new LinkedHashSet<>();
        Map<ResourceLocation, Set<ResourceLocation>> suppressedFallingBlocks = new LinkedHashMap<>();
        Map<MobCategory, Map<ResourceLocation, MobScaling.Plan>> scaling = new EnumMap<>(MobCategory.class);
        Map<MobCategory, MobScaling.Plan> defaultScaling = new EnumMap<>(MobCategory.class);
        Map<ResourceLocation, BossOptions> bossOptions = new LinkedHashMap<>();
        for (MobCategory category : MobCategory.values()) {
            JsonArray poolEntries = read(manager, "mob_pools/" + category.key());
            Pool pool = category == MobCategory.BOSS
                    ? parseBossPool(poolEntries, ForgeRegistries.ITEMS::containsKey)
                    : parsePool(poolEntries, ForgeRegistries.ITEMS::containsKey);
            pools.put(category, pool.mobs());
            scaling.put(category, pool.scaling());
            defaultScaling.put(category, pool.defaultScaling());
            bossOptions.putAll(pool.bossOptions());
            suppressedSpawns.addAll(pool.suppressedSpawns());
            pool.suppressedFallingBlocks().forEach((id, blocks) ->
                    suppressedFallingBlocks.computeIfAbsent(id, unused -> new LinkedHashSet<>()).addAll(blocks));
            pool.rewards().forEach((id, entries) -> rewards.computeIfAbsent(id, unused -> new ArrayList<>()).addAll(entries));
        }
        return new Definitions(pools, new Equipment(mobType(manager, "armor_mobs"), mobType(manager, "melee_mobs"),
                mobType(manager, "bow_users"), mobType(manager, "crossbow_users"), mobType(manager, "trident_users"), mobType(manager, "ranged_mobs")),
                mobType(manager, "ghost_mobs"), rewards, suppressedSpawns, suppressedFallingBlocks, scaling, defaultScaling, bossOptions);
    }
    @Override protected void apply(Definitions definitions, ResourceManager manager, ProfilerFiller profiler) {
        current = definitions;
        definitions.pools().forEach((category, ids) -> ids.stream().filter(id -> !availableMob(id))
                .forEach(id -> warnUnavailableMob(id, "data/spiral_abyss/mob_pools/" + category.key() + ".json")));
    }
}
