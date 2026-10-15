package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Map;

/** Shared stack construction keeps direct reward previews and saved payouts identical. */
public final class RewardItems {
    private RewardItems() {}

    public static ItemStack stack(FloorDefinition.RewardEntry entry, int count) {
        if (!entry.item() || !ForgeRegistries.ITEMS.containsKey(entry.id())) return ItemStack.EMPTY;
        var item = ForgeRegistries.ITEMS.getValue(entry.id());
        if (item == null || item == Items.AIR || count <= 0) return ItemStack.EMPTY;
        ListTag enchantments = new ListTag();
        entry.enchantments().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(configured -> {
            // Optional mods may be absent. Their enchantments do not invalidate the item or other enchantments.
            if (!ForgeRegistries.ENCHANTMENTS.containsKey(configured.getKey())) return;
            // Preserve the exact namespaced registry ID from the datapack in Minecraft's enchantment NBT.
            enchantments.add(EnchantmentHelper.storeEnchantment(configured.getKey(), configured.getValue()));
        });
        if (item == Items.BOOK && !enchantments.isEmpty()) item = Items.ENCHANTED_BOOK;
        ItemStack stack = new ItemStack(item, count);
        // Explicit datapack enchantments may exceed normal levels or applicability/compatibility rules.
        if (!enchantments.isEmpty()) stack.addTagElement(item == Items.ENCHANTED_BOOK ? "StoredEnchantments" : "Enchantments", enchantments);
        return stack;
    }
}
