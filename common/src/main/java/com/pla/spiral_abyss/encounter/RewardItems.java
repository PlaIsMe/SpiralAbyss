package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;
import net.minecraft.resources.ResourceKey;

/** Shared component-based construction keeps reward previews and saved payouts identical. */
public final class RewardItems {
    private RewardItems() {}
    public static ItemStack stack(HolderLookup.Provider registries, FloorDefinition.RewardEntry entry, int count) {
        if (!entry.item() || !BuiltInRegistries.ITEM.containsKey(entry.id()) || count <= 0) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(entry.id());
        if (item == Items.AIR) return ItemStack.EMPTY;
        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        boolean enchanted = entry.enchantments().keySet().stream().anyMatch(id ->
                enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, id)).isPresent());
        ItemStack stack = new ItemStack(item == Items.BOOK && enchanted ? Items.ENCHANTED_BOOK : item, count);
        entry.enchantments().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(configured ->
                enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, configured.getKey()))
                        .ifPresent(enchantment -> stack.enchant(enchantment, configured.getValue())));
        return stack;
    }
}
