package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
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
    private FloorRewards() {}
    public static double partyScale(int players) { return players <= 0 ? 0 : 1 + 0.5 * (players - 1); }
    public record Bundle(List<ItemStack> items, int experience) {
        public Bundle { items = items.stream().map(ItemStack::copy).toList(); }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag stacks = new ListTag();
            items.forEach(stack -> stacks.add(stack.save(new CompoundTag())));
            tag.put("Items", stacks); tag.putInt("Experience", experience);
            return tag;
        }
        public static Bundle load(CompoundTag tag) {
            List<ItemStack> stacks = new ArrayList<>();
            tag.getList("Items", Tag.TAG_COMPOUND).forEach(value -> stacks.add(ItemStack.of((CompoundTag) value)));
            return new Bundle(stacks, tag.getInt("Experience"));
        }
    }
    /** Each encountered type contributes once per floor, regardless of counts or repeated waves. */
    public static FloorDefinition.Rewards withMobRewards(FloorDefinition.Rewards base,
            Set<net.minecraft.resources.ResourceLocation> encountered,
            Map<net.minecraft.resources.ResourceLocation, List<FloorDefinition.RewardEntry>> linked) {
        var entries = new ArrayList<>(base.entries());
        var additions = new LinkedHashSet<FloorDefinition.RewardEntry>();
        encountered.forEach(id -> additions.addAll(linked.getOrDefault(id, List.of())));
        additions.removeAll(base.entries());
        entries.addAll(additions);
        return new FloorDefinition.Rewards(List.copyOf(entries), base.rolls(), base.experience());
    }
    public static Bundle roll(ServerLevel level, Vec3 position, FloorDefinition.Rewards rewards, double scale) {
        List<ItemStack> items = new ArrayList<>();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position)
                .create(LootContextParamSets.CHEST);
        double scaledRolls = rewards.rolls() * scale;
        int rolls = (int) scaledRolls + (level.random.nextDouble() < scaledRolls % 1 ? 1 : 0);
        var pool = new RewardPool(rewards.entries(), entry -> entry.item() ? availableItem(entry)
                : level.getServer().getLootData().getElement(new LootDataId<>(LootDataType.TABLE, entry.id())) != null);
        var bonuses = rewards.entries().stream().filter(FloorDefinition.RewardEntry::item).toList();
        // Selecting from the available table pool is equivalent to rerolling missing tables,
        // without an unbounded retry when optional mods are absent.
        for (int i = 0; i < rolls; i++) {
            // Choose a chest table, then independently try every item bonus on this same roll.
            var table = pool.table(level.random);
            if (table != null && level.random.nextDouble() < table.chance())
                items.addAll(level.getServer().getLootData().getLootTable(table.id()).getRandomItems(params));
            for (var requested : bonuses) {
                var entry = pool.bonus(requested, level.random);
                if (entry == null) continue;
                if (level.random.nextDouble() >= entry.chance()) continue;
                var item = ForgeRegistries.ITEMS.getValue(entry.id());
                if (item == null || item == Items.AIR) continue;
                int count = entry.minCount() + level.random.nextInt(entry.maxCount() - entry.minCount() + 1);
                while (count > 0) {
                    ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxStackSize()));
                    items.add(stack); count -= stack.getCount();
                }
            }
        }
        return new Bundle(items, (int) Math.round(rewards.experience() * scale));
    }
    private static boolean availableItem(FloorDefinition.RewardEntry entry) {
        return ForgeRegistries.ITEMS.containsKey(entry.id()) && ForgeRegistries.ITEMS.getValue(entry.id()) != Items.AIR;
    }
    public static void releaseItem(ServerLevel level, Vec3 position, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, position.x, position.y + 0.8, position.z, stack.copy());
        item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.6, 0.45 + level.random.nextDouble() * 0.2,
                (level.random.nextDouble() - 0.5) * 0.6);
        item.setDefaultPickUpDelay(); level.addFreshEntity(item);
    }
    public static void releaseExperience(ServerLevel level, Vec3 position, int experience) {
        if (experience > 0) ExperienceOrb.award(level, position, experience);
    }
}
