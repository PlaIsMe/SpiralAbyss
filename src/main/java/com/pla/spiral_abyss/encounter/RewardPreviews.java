package com.pla.spiral_abyss.encounter;

import com.google.gson.*;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.network.AbyssNetwork.RewardPreview;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Describes possible rewards without executing loot rolls or consuming the world's random stream. */
public final class RewardPreviews {
    public static final int MAX_REWARDS = 128, MAX_CONTENTS = 128;
    private final MinecraftServer server;
    private final Gson gson = Deserializers.createLootTableSerializer().create();
    private final Map<ResourceLocation, Contents> cache = new HashMap<>();
    private record Contents(List<ResourceLocation> items, boolean incomplete) {}
    private record PreviewKey(ResourceLocation id, boolean item, boolean rare, Map<ResourceLocation, Integer> enchantments) {}
    private static final class Scan {
        final Set<ResourceLocation> items = new TreeSet<>();
        final Set<ResourceLocation> visiting = new HashSet<>();
        boolean incomplete;
        int nodes;
    }
    public RewardPreviews(MinecraftServer server) { this.server = server; }

    public List<RewardPreview> floor(FloorDefinition.Rewards base, Set<ResourceLocation> enemies) {
        var rewards = FloorRewards.withMobRewards(base, enemies, MobCatalog.rewards());
        // Include zero-chance entries in the denominator: they can still be selected, then fail.
        // Retry outcomes and loot-table conditions are intentionally not simulated by the preview.
        Map<ResourceLocation, Double> tableChances = new HashMap<>();
        double tableWeight = 0;
        for (var entry : rewards.entries()) {
            if (entry.item() || table(entry.id()) == null) continue;
            tableWeight += entry.weight();
            tableChances.merge(entry.id(), entry.weight() * entry.chance(), Double::sum);
        }
        Map<PreviewKey, FloorDefinition.RewardEntry> unique = new LinkedHashMap<>();
        rewards.entries().stream().filter(entry -> entry.chance() > 0)
                .sorted(Comparator.comparingInt(entry -> !entry.item() ? 0 : entry.rare() ? 2 : 1))
                .forEach(entry -> unique.putIfAbsent(new PreviewKey(entry.id(), entry.item(), entry.rare(), entry.enchantments()), entry));
        List<RewardPreview> result = new ArrayList<>();
        for (var entry : unique.values()) {
            if (result.size() >= MAX_REWARDS) break;
            if (entry.item()) {
                if (available(entry.id())) result.add(new RewardPreview(entry.id(), false, entry.rare(), List.of(), false,
                        RewardItems.stack(entry, 1), entry.rare() ? RewardPool.RARE_CHANCE : 0));
            } else if (table(entry.id()) != null) {
                Contents contents = cache.computeIfAbsent(entry.id(), id -> {
                    Scan scan = new Scan(); scanTable(id, scan, 0);
                    return new Contents(List.copyOf(scan.items), scan.incomplete);
                });
                double chance = tableWeight > 0 ? tableChances.getOrDefault(entry.id(), 0.0) / tableWeight : 0;
                result.add(new RewardPreview(entry.id(), true, false, contents.items(), contents.incomplete(),
                        net.minecraft.world.item.ItemStack.EMPTY, Math.min(1, chance)));
            }
        }
        return List.copyOf(result);
    }
    private LootTable table(ResourceLocation id) {
        return server.getLootData().getElement(new LootDataId<>(LootDataType.TABLE, id));
    }
    private static boolean available(ResourceLocation id) {
        return ForgeRegistries.ITEMS.containsKey(id) && ForgeRegistries.ITEMS.getValue(id) != Items.AIR;
    }
    private void add(ResourceLocation id, Scan scan) {
        if (!available(id)) return;
        if (scan.items.size() < MAX_CONTENTS) scan.items.add(id);
        else if (!scan.items.contains(id)) scan.incomplete = true;
    }
    private void scanTable(ResourceLocation id, Scan scan, int depth) { scanTable(id, scan, depth, 0); }
    private void scanTable(ResourceLocation id, Scan scan, int depth, int transforms) {
        if (depth > 16 || !scan.visiting.add(id)) { scan.incomplete = true; return; }
        try {
            LootTable table = table(id);
            if (table == null) { scan.incomplete = true; return; }
            scan(gson.toJsonTree(table, LootTable.class), scan, depth + 1, transforms);
        } catch (RuntimeException ex) {
            // Custom entries may not support serialization. A partial preview is honest about that.
            scan.incomplete = true;
        } finally { scan.visiting.remove(id); }
    }
    private void scan(JsonElement value, Scan state, int depth, int transforms) {
        if (++state.nodes > 8192 || depth > 32) { state.incomplete = true; return; }
        if (value.isJsonArray()) { for (var child : value.getAsJsonArray()) scan(child, state, depth + 1, transforms); return; }
        if (!value.isJsonObject()) return;
        JsonObject object = value.getAsJsonObject();
        transforms |= transforms(object.get("functions"), state);
        if (object.has("type")) {
            String type = object.get("type").getAsString();
            if (!type.contains(":")) type = "minecraft:" + type;
            ResourceLocation id = object.has("name") && object.get("name").isJsonPrimitive()
                    ? ResourceLocation.tryParse(object.get("name").getAsString()) : null;
            switch (type) {
                case "minecraft:item" -> { if (id != null) addTransformed(id, state, transforms); }
                case "minecraft:loot_table" -> { if (id != null) scanTable(id, state, depth + 1, transforms); }
                case "minecraft:tag" -> {
                    final int appliedTransforms = transforms;
                    if (id != null) server.registryAccess().registryOrThrow(Registries.ITEM)
                            .getTag(TagKey.create(Registries.ITEM, id)).ifPresent(tag -> tag.forEach(item ->
                                    addTransformed(ForgeRegistries.ITEMS.getKey(item.value()), state, appliedTransforms)));
                }
                case "minecraft:empty", "minecraft:alternatives", "minecraft:sequence", "minecraft:group",
                        "minecraft:chest" -> { }
                default -> { if (!object.has("pools")) state.incomplete = true; }
            }
        }
        for (String key : List.of("pools", "entries", "children"))
            if (object.has(key)) scan(object.get(key), state, depth + 1, transforms);
    }
    private void addTransformed(ResourceLocation id, Scan state, int transforms) {
        if ((transforms & 1) != 0 && id.toString().equals("minecraft:book")) {
            add(ResourceLocation.fromNamespaceAndPath("minecraft", "enchanted_book"), state);
            if ((transforms & 4) == 0) return;
        }
        if ((transforms & 2) != 0 && id.toString().equals("minecraft:map"))
            add(ResourceLocation.fromNamespaceAndPath("minecraft", "filled_map"), state);
        add(id, state);
    }
    private static int transforms(JsonElement value, Scan state) {
        if (value == null || !value.isJsonArray()) return 0;
        int result = 0;
        for (var element : value.getAsJsonArray()) {
            if (!element.isJsonObject()) { state.incomplete = true; continue; }
            var function = element.getAsJsonObject();
            if (!function.has("function")) continue;
            String name = function.get("function").getAsString();
            if (!name.contains(":")) name = "minecraft:" + name;
            if (name.equals("minecraft:enchant_randomly") || name.equals("minecraft:enchant_with_levels")) {
                result |= 1;
                if (function.has("conditions") && !function.getAsJsonArray("conditions").isEmpty()) result |= 4;
            } else if (name.equals("minecraft:exploration_map")) result |= 2;
            else if (!name.startsWith("minecraft:") || name.equals("minecraft:furnace_smelt")) state.incomplete = true;
        }
        return result;
    }
}
