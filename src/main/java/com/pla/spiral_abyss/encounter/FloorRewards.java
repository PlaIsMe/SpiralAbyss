package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Roll once when claiming a chest for its currently eligible roster, then persist the payout. */
public final class FloorRewards {
    private static final Set<net.minecraft.resources.ResourceLocation> missingTables = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private FloorRewards() {}
    static void resetMissingTableLogs() { missingTables.clear(); }
    private static boolean availableTable(ServerLevel level, FloorDefinition.RewardEntry entry) {
        if (level.getServer().getLootData().getElement(new LootDataId<>(LootDataType.TABLE, entry.id())) != null) return true;
        if (missingTables.add(entry.id())) com.mojang.logging.LogUtils.getLogger().error(
                "Abyss reward loot table {} is unavailable (missing or failed to load); skipping it and rolling other available configured tables. Check data/{}/loot_tables/{}.json and the providing mod/datapack.",
                entry.id(), entry.id().getNamespace(), entry.id().getPath());
        return false;
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
                entry.item() ? new FloorDefinition.RewardEntry(entry.id(), true, entry.minCount(),
                        entry.maxCount(), entry.weight(), entry.chance(), true) : entry)));
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
                : availableTable(level, entry));
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
            rollBonuses(level, bonuses, normalPool, items, rareItems, false);
        }
        // Three golden table rolls do not triple the boss bonus probability.
        if (tier == 3) for (int player = 0; player < eligiblePlayers; player++)
            rollBonuses(level, rareBonuses, rarePool, items, rareItems, true);
        return new Bundle(items, (int) Math.round(rewards.experience() * ChallengeRewards.scale(eligiblePlayers)), rareItems);
    }
    private static void rollBonuses(ServerLevel level, List<FloorDefinition.RewardEntry> bonuses, RewardPool pool,
                                    List<ItemStack> items, Set<Integer> rareItems, boolean rare) {
        for (var requested : bonuses) {
            var entry = pool.bonus(requested, level.random);
            if (entry == null || level.random.nextDouble() >= entry.chance()) continue;
            var item = ForgeRegistries.ITEMS.getValue(entry.id());
            if (item == null || item == Items.AIR) continue;
            int count = entry.minCount() + level.random.nextInt(entry.maxCount() - entry.minCount() + 1);
            while (count > 0) {
                ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxStackSize()));
                if (rare) rareItems.add(items.size());
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
        if (stack.isEmpty()) return;
        ItemEntity item = new ChestLootEntity(level, position.x, position.y + 0.8, position.z, stack.copy(), rare);
        item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.6, 0.45 + level.random.nextDouble() * 0.2,
                (level.random.nextDouble() - 0.5) * 0.6);
        item.setDefaultPickUpDelay(); level.addFreshEntity(item);
    }
    public static void releaseExperience(ServerLevel level, Vec3 position, int experience) {
        if (experience > 0) ExperienceOrb.award(level, position, experience);
    }
}
