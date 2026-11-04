package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.*;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.*;

/** A wave chooses one complete armor set; each eligible mob rolls whether to wear it. */
public final class WaveEquipment {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final MobCatalog.Equipment whitelist;
    private final GearConfig.Floor floor;
    private final List<Item> armor;
    private final List<Item> weapons;

    public WaveEquipment(MobCatalog.Equipment whitelist, GearConfig.Floor floor, RandomSource random) {
        this.whitelist = whitelist; this.floor = floor;
        List<List<Item>> sets = new ArrayList<>();
        for (var ids : floor.armorSets()) {
            List<Item> items = ids.stream().map(WaveEquipment::item).toList();
            boolean valid = items.size() == ARMOR.length;
            for (int i = 0; valid && i < ARMOR.length; i++)
                valid = items.get(i) != null && Mob.getEquipmentSlotForItem(new ItemStack(items.get(i))) == ARMOR[i];
            if (valid) sets.add(items);
            else LogUtils.getLogger().warn("Skipping unavailable or incorrectly ordered Abyss armor set: {}", ids);
        }
        armor = sets.isEmpty() ? List.of() : sets.get(random.nextInt(sets.size()));
        weapons = floor.weapons().stream().map(WaveEquipment::item).filter(Objects::nonNull).toList();
    }
    private static Item item(ResourceLocation id) {
        Item item = AbyssRegistries.ITEMS.containsKey(id) ? AbyssRegistries.ITEMS.getValue(id) : null;
        return item == Items.AIR ? null : item;
    }
    public void apply(Mob mob) {
        mob.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0);
        var id = AbyssRegistries.ENTITY_TYPES.getKey(mob.getType());
        RandomSource random = mob.getRandom();
        if (whitelist.armor().contains(id)) {
            // Remove vanilla random pieces first, so partial/mixed tiers cannot leak into a wave.
            for (EquipmentSlot slot : ARMOR) mob.setItemSlot(slot, ItemStack.EMPTY);
            if (!armor.isEmpty() && random.nextDouble() < floor.armorChance()) {
                boolean enchanted = random.nextDouble() < floor.armor().chance();
                for (int i = 0; i < ARMOR.length; i++) {
                    ItemStack stack = new ItemStack(armor.get(i));
                    if (enchanted) enchant(stack, floor.armor(), random, true);
                    mob.setItemSlot(ARMOR[i], stack);
                }
            }
        }
        // Ranged users take precedence if a custom config puts a mob in several lists.
        if (whitelist.tridents().contains(id)) {
            ItemStack stack = new ItemStack(Items.TRIDENT);
            stack.enchant(Enchantments.LOYALTY, floor.loyaltyLevel());
            maybeEnchant(stack, floor.trident(), random);
            weapon(mob, stack);
        } else if (whitelist.crossbows().contains(id)) {
            ItemStack stack = new ItemStack(Items.CROSSBOW);
            maybeEnchant(stack, floor.crossbow(), random); weapon(mob, stack);
        } else if (whitelist.bows().contains(id)) {
            ItemStack stack = new ItemStack(Items.BOW);
            maybeEnchant(stack, floor.bow(), random); weapon(mob, stack);
        } else if (whitelist.melee().contains(id)) {
            // Vindicators need an axe for their native attack/rendering behavior. Keep their
            // iron axe unless this floor actually supplies a stronger axe.
            if (mob instanceof Vindicator) {
                if (!weapons.isEmpty() && random.nextDouble() < floor.weaponChance()) {
                    Item nativeItem = mob.getMainHandItem().isEmpty() ? Items.IRON_AXE : mob.getMainHandItem().getItem();
                    List<AxeItem> upgrades = weapons.stream().filter(AxeItem.class::isInstance)
                            .map(AxeItem.class::cast).filter(axe -> nativeItem instanceof AxeItem current
                                    && axe.getTier().getLevel() > current.getTier().getLevel()).toList();
                    if (!upgrades.isEmpty()) {
                        ItemStack stack = new ItemStack(upgrades.get(random.nextInt(upgrades.size())));
                        maybeEnchant(stack, floor.melee(), random); weapon(mob, stack);
                    }
                }
                if (mob.getMainHandItem().isEmpty()) weapon(mob, new ItemStack(Items.IRON_AXE));
                return;
            }
            weapon(mob, ItemStack.EMPTY);
            if (!weapons.isEmpty() && random.nextDouble() < floor.weaponChance()) {
                ItemStack stack = new ItemStack(weapons.get(random.nextInt(weapons.size())));
                maybeEnchant(stack, floor.melee(), random); weapon(mob, stack);
            }
        }
    }
    private static void weapon(Mob mob, ItemStack stack) {
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.MAINHAND, stack);
    }
    private static void maybeEnchant(ItemStack stack, GearConfig.Rule rule, RandomSource random) {
        if (random.nextDouble() < rule.chance()) enchant(stack, rule, random, false);
    }
    private static void enchant(ItemStack stack, GearConfig.Rule rule, RandomSource random, boolean armor) {
        List<Enchantment> pool = new ArrayList<>();
        for (var id : rule.entries()) {
            Enchantment enchantment = AbyssRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment != null && !pool.contains(enchantment) && (!armor || enchantment.canEnchant(stack))) pool.add(enchantment);
        }
        int count = pool.isEmpty() ? 0 : 1 + random.nextInt(Math.min(3, pool.size()));
        var selected = new ArrayList<>(EnchantmentHelper.getEnchantments(stack).keySet());
        while (count > 0 && !pool.isEmpty()) {
            var next = pool.remove(random.nextInt(pool.size()));
            if (selected.stream().anyMatch(previous -> previous == next || !previous.isCompatibleWith(next)
                    || !next.isCompatibleWith(previous))) continue;
            // Keep Quick Charge in its supported range (V has zero charge time). Multishot has one useful level.
            int level = next == Enchantments.QUICK_CHARGE ? Math.min(3, rule.level())
                    : next == Enchantments.MULTISHOT ? 1 : rule.level();
            stack.enchant(next, level); selected.add(next); count--;
        }
    }
}
