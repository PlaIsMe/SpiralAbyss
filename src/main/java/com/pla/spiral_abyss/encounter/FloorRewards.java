package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Roll once on victory and save the result; chest opening never rerolls rewards. */
public final class FloorRewards {
    private FloorRewards() {}
    public static double partyScale(int players) { return 1 + 0.5 * (Math.max(1, Math.min(4, players)) - 1); }
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
    public static Bundle roll(ServerLevel level, Vec3 position, FloorDefinition.Rewards rewards, double scale) {
        List<ItemStack> items = new ArrayList<>();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position)
                .create(LootContextParamSets.CHEST);
        double scaledRolls = rewards.rolls() * scale;
        int rolls = (int) scaledRolls + (level.random.nextDouble() < scaledRolls % 1 ? 1 : 0);
        var tables = rewards.entries().stream().filter(entry -> !entry.item()).toList();
        var bonuses = rewards.entries().stream().filter(FloorDefinition.RewardEntry::item).toList();
        int totalWeight = tables.stream().mapToInt(FloorDefinition.RewardEntry::weight).sum();
        for (int i = 0; i < rolls; i++) {
            // Choose a chest table, then independently try every item bonus on this same roll.
            if (totalWeight > 0) {
                int choice = level.random.nextInt(totalWeight);
                for (var entry : tables) {
                    choice -= entry.weight();
                    if (choice >= 0) continue;
                    if (level.random.nextDouble() < entry.chance())
                        items.addAll(level.getServer().getLootData().getLootTable(entry.id()).getRandomItems(params));
                    break;
                }
            }
            for (var entry : bonuses) {
                if (level.random.nextDouble() >= entry.chance()) continue;
                var item = ForgeRegistries.ITEMS.getValue(entry.id());
                if (item == null) continue;
                int count = entry.minCount() + level.random.nextInt(entry.maxCount() - entry.minCount() + 1);
                while (count > 0) {
                    ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxStackSize()));
                    items.add(stack); count -= stack.getCount();
                }
            }
        }
        return new Bundle(items, (int) Math.round(rewards.experience() * scale));
    }
    public static void release(ServerLevel level, Vec3 position, Bundle bundle) {
        for (ItemStack stack : bundle.items()) {
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(level, position.x, position.y + 0.8, position.z, stack.copy());
            item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.6, 0.45 + level.random.nextDouble() * 0.2,
                    (level.random.nextDouble() - 0.5) * 0.6);
            item.setDefaultPickUpDelay(); level.addFreshEntity(item);
        }
        if (bundle.experience() > 0) ExperienceOrb.award(level, position, bundle.experience());
    }
}
