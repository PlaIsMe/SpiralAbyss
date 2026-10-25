package com.pla.spiral_abyss.encounter;

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
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;
import java.util.*;

/** Roll once when claiming a chest for its currently eligible roster, then persist the payout. */
public final class FloorRewards {
    private static final Set<net.minecraft.resources.ResourceLocation> missingTables = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private FloorRewards() {}
    static void resetMissingTableLogs() { missingTables.clear(); }
    static LootTable table(MinecraftServer server, ResourceLocation id) {
        LootTable table = server.getLootData().getElement(new LootDataId<>(LootDataType.TABLE, id));
        if (table == null && (id.getNamespace().equals("minecraft") || ModList.get().isLoaded(id.getNamespace()))
                && missingTables.add(id)) com.mojang.logging.LogUtils.getLogger().warn(
                "[Spiral Abyss] Ignoring unavailable reward loot table {} (missing or failed to load). Check the configured ID and data/{}/loot_tables/{}.json; other available rewards remain usable.",
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
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        public Bundle(List<ItemStack> items, int experience) { this(items, experience, Set.of()); }
        public boolean rare(int index) { return rareItems.contains(index); }
        public boolean hasRareItems() { return !rareItems.isEmpty(); }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag stacks = new ListTag();
            items.forEach(stack -> stacks.add(stack.save(new CompoundTag())));
            tag.put("Items", stacks); tag.putInt("Experience", experience);
            tag.putIntArray("RareItems", rareItems.stream().sorted().mapToInt(Integer::intValue).toArray());
            return tag;
        }
        public static Bundle load(CompoundTag tag) {
            List<ItemStack> stacks = new ArrayList<>();
            tag.getList("Items", Tag.TAG_COMPOUND).forEach(value -> stacks.add(ItemStack.of((CompoundTag) value)));
            Set<Integer> rare = new HashSet<>();
            for (int index : tag.getIntArray("RareItems")) rare.add(index);
            return new Bundle(stacks, tag.getInt("Experience"), rare);
        }
    }
    /** Each encountered type contributes once per floor, regardless of counts or repeated waves. */
    public static FloorDefinition.Rewards withMobRewards(FloorDefinition.Rewards base,
            Set<net.minecraft.resources.ResourceLocation> encountered,
            Map<net.minecraft.resources.ResourceLocation, List<FloorDefinition.RewardEntry>> linked) {
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
            Set<net.minecraft.resources.ResourceLocation> attempted = new HashSet<>();
            // Failed chances and valid empty tables try other actual rewards, at most once per ID.
            // The hard cap also bounds large pools assembled from optional mob-linked definitions.
            for (int attempt = 0; attempt < 64; attempt++) {
                var table = pool.table(level.random, attempted);
                if (table == null) break;
                attempted.add(table.id());
                if (level.random.nextDouble() >= table.chance()) continue;
                var rolled = level.getServer().getLootData().getLootTable(table.id()).getRandomItems(params)
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
                var item = ForgeRegistries.ITEMS.getValue(rare.id());
                if (item != null && item != Items.AIR) {
                    rareItems.add(items.size());
                    items.add(RewardItems.stack(rare, RewardPool.RARE_COUNT));
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
            var item = ForgeRegistries.ITEMS.getValue(entry.id());
            if (item == null || item == Items.AIR) continue;
            int count = entry.minCount() + level.random.nextInt(entry.maxCount() - entry.minCount() + 1);
            while (count > 0) {
                ItemStack stack = RewardItems.stack(entry, 1);
                stack.setCount(Math.min(count, stack.getMaxStackSize()));
                items.add(stack); count -= stack.getCount();
            }
        }
    }
    private static boolean availableItem(FloorDefinition.RewardEntry entry) {
        return ForgeRegistries.ITEMS.containsKey(entry.id()) && ForgeRegistries.ITEMS.getValue(entry.id()) != Items.AIR;
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
