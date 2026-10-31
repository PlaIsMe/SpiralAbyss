package com.pla.spiral_abyss.encounter;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import java.util.*;
import com.mojang.logging.LogUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

/** Roll once when claiming a chest for its currently eligible roster, then persist the payout. */
public final class FloorRewards {
    private static final Set<ResourceLocation> missingTables = ConcurrentHashMap.newKeySet();
    private FloorRewards() {}
    static void resetMissingTableLogs() { missingTables.clear(); }
    static LootTable table(MinecraftServer server, ResourceLocation id) {
        LootTable table = server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).get(ResourceKey.create(Registries.LOOT_TABLE, id)).map(Holder::value).orElse(null);
        if (table == null && (id.getNamespace().equals("minecraft") || ModList.get().isLoaded(id.getNamespace()))
                && missingTables.add(id)) LogUtils.getLogger().warn(
                "[Spiral Abyss] Ignoring unavailable reward loot table {} (missing or failed to load). Check the configured ID and data/{}/loot_table/{}.json; other available rewards remain usable.",
                id, id.getNamespace(), id.getPath());
        return table;
    }
    static void validateTables(MinecraftServer server) {
        // Run after loot loading finishes, so a failed table is treated like a missing one.
        var entries = new ArrayList<FloorDefinition.RewardEntry>();
        FloorCatalog.snapshot().floors().values().forEach(floor -> entries.addAll(floor.rewards().entries()));
        MobCatalog.rewards().values().forEach(entries::addAll);
        entries.stream().filter(entry -> !entry.item()).map(FloorDefinition.RewardEntry::id)
                .distinct().forEach(id -> table(server, id));
    }
    public static double partyScale(int players) { return players <= 0 ? 0 : 1 + 0.5 * (players - 1); }
    public record Bundle(List<ItemStack> items, int experience, Set<Integer> rareItems) {
        public Bundle {
            items = items.stream().map(ItemStack::copy).toList();
            int count = items.size();
            rareItems = rareItems.stream().filter(index -> index >= 0 && index < count)
                    .collect(Collectors.toUnmodifiableSet());
        }
        public Bundle(List<ItemStack> items, int experience) { this(items, experience, Set.of()); }
        public boolean rare(int index) { return rareItems.contains(index); }
        public boolean hasRareItems() { return !rareItems.isEmpty(); }
        public CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            ListTag stacks = new ListTag();
            items.forEach(stack -> stacks.add(stack.saveOptional(registries)));
            tag.put("Items", stacks);
            tag.putInt("ItemDataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion()); tag.putInt("Experience", experience);
            tag.putIntArray("RareItems", rareItems.stream().sorted().mapToInt(Integer::intValue).toArray());
            return tag;
        }
        public static Bundle load(CompoundTag tag, HolderLookup.Provider registries) {
            List<ItemStack> stacks = new ArrayList<>();
            int version = tag.contains("ItemDataVersion") ? tag.getInt("ItemDataVersion") : 3465; // Minecraft 1.20.1
            int currentVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
            tag.getList("Items", Tag.TAG_COMPOUND).forEach(value -> {
                CompoundTag item = (CompoundTag) value;
                if (!item.isEmpty() && version < currentVersion) {
                    item = (CompoundTag) DataFixers.getDataFixer().update(
                            References.ITEM_STACK,
                            new Dynamic<>(NbtOps.INSTANCE, item), version, currentVersion).getValue();
                }
                stacks.add(ItemStack.parseOptional(registries, item));
            });
            Set<Integer> rare = new HashSet<>();
            for (int index : tag.getIntArray("RareItems")) rare.add(index);
            return new Bundle(stacks, tag.getInt("Experience"), rare);
        }
    }
    /** Each encountered type contributes once per floor, regardless of counts or repeated waves. */
    public static FloorDefinition.Rewards withMobRewards(FloorDefinition.Rewards base,
            Set<ResourceLocation> encountered,
            Map<ResourceLocation, List<FloorDefinition.RewardEntry>> linked) {
        var entries = new ArrayList<>(base.entries());
        var additions = new LinkedHashSet<FloorDefinition.RewardEntry>();
        encountered.forEach(id -> linked.getOrDefault(id, List.of()).forEach(entry -> additions.add(
                entry.item() ? RewardPool.rare(entry) : entry)));
        additions.removeAll(base.entries());
        entries.addAll(additions);
        return new FloorDefinition.Rewards(List.copyOf(entries), base.rolls(), base.experience());
    }
    public static Bundle roll(ServerLevel level, Vec3 position, FloorDefinition.Rewards rewards,
                              int tier, int eligiblePlayers) {
        List<ItemStack> items = new ArrayList<>();
        Set<Integer> rareItems = new HashSet<>();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position)
                .create(LootContextParamSets.CHEST);
        int rolls = ChallengeRewards.rollCount(tier, eligiblePlayers);
        var pool = new RewardPool(rewards.entries(), entry -> entry.item() ? availableItem(entry)
                : table(level.getServer(), entry.id()) != null);
        var bonuses = rewards.entries().stream().filter(entry -> entry.item() && !entry.rare()).toList();
        var rareBonuses = rewards.entries().stream().filter(entry -> entry.item() && entry.rare()).toList();
        // Keep optional-item replacements within their own rarity group.
        var normalPool = new RewardPool(bonuses, FloorRewards::availableItem);
        var rarePool = new RewardPool(rareBonuses, FloorRewards::availableItem);
        // Selecting from the available table pool is equivalent to rerolling missing tables,
        // without an unbounded retry when optional mods are absent.
        for (int i = 0; i < rolls; i++) {
            // Floor-authored item entries retain their independent per-table-roll behavior.
            Set<ResourceLocation> attempted = new HashSet<>();
            // Failed chances and valid empty tables try other actual rewards, at most once per ID.
            // The hard cap also bounds large pools assembled from optional mob-linked definitions.
            for (int attempt = 0; attempt < 64; attempt++) {
                var table = pool.table(level.random, attempted);
                if (table == null) break;
                attempted.add(table.id());
                if (level.random.nextDouble() >= table.chance()) continue;
                var rolled = table(level.getServer(), table.id()).getRandomItems(params)
                        .stream().filter(stack -> !stack.isEmpty()).toList();
                if (rolled.isEmpty()) continue;
                items.addAll(rolled);
                break;
            }
            rollBonuses(level, bonuses, normalPool, items);
        }
        // Extra participants improve rare chances, never the number of rare items in a chest.
        // All encountered bosses share this one-item limit, independent of their configured count.
        if (tier == 3) {
            var rare = rarePool.singleBonus(rareBonuses, level.random, eligiblePlayers);
            if (rare != null) {
                var item = BuiltInRegistries.ITEM.get(rare.id());
                if (item != null && item != Items.AIR) {
                    rareItems.add(items.size());
                    items.add(RewardItems.stack(level.registryAccess(), rare, RewardPool.RARE_COUNT));
                }
            }
        }
        return new Bundle(items, (int) Math.round(rewards.experience() * ChallengeRewards.scale(eligiblePlayers)), rareItems);
    }
    private static void rollBonuses(ServerLevel level, List<FloorDefinition.RewardEntry> bonuses, RewardPool pool,
                                    List<ItemStack> items) {
        for (var requested : bonuses) {
            var entry = pool.bonus(requested, level.random);
            if (entry == null || level.random.nextDouble() >= entry.chance()) continue;
            var item = BuiltInRegistries.ITEM.get(entry.id());
            if (item == null || item == Items.AIR) continue;
            int count = entry.minCount() + level.random.nextInt(entry.maxCount() - entry.minCount() + 1);
            while (count > 0) {
                ItemStack stack = RewardItems.stack(level.registryAccess(), entry, 1);
                stack.setCount(Math.min(count, stack.getMaxStackSize()));
                items.add(stack); count -= stack.getCount();
            }
        }
    }
    private static boolean availableItem(FloorDefinition.RewardEntry entry) {
        return BuiltInRegistries.ITEM.containsKey(entry.id()) && BuiltInRegistries.ITEM.get(entry.id()) != Items.AIR;
    }
    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack) {
        releaseItem(level, position, stack, false);
    }
    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack, boolean rare) {
        releaseItem(level, position, stack, rare, 0);
    }
    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack, boolean rare, int floor) {
        if (stack.isEmpty()) return;
        ChestLootEntity item = new ChestLootEntity(level, position.x, position.y + 0.8, position.z, stack.copy(), rare);
        RareRewardBroadcast.markDrop(item, floor);
        item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.6, 0.45 + level.random.nextDouble() * 0.2,
                (level.random.nextDouble() - 0.5) * 0.6);
        item.setDefaultPickUpDelay(); level.addFreshEntity(item);
    }
    public static void releaseExperience(ServerLevel level, Vec3 position, int experience) {
        if (experience > 0) ExperienceOrb.award(level, position, experience);
    }
}
