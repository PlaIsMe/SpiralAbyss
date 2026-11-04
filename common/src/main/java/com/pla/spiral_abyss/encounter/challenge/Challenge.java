package com.pla.spiral_abyss.encounter.challenge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import java.util.*;

/** A concrete cycle roll, shared by every wave of one floor. */
public record Challenge(Type type, Difficulty difficulty, int target, double percentage, List<ResourceLocation> ids,
                        List<EquipmentSlot> forbiddenSlots) {
    public enum Type {
        TIME, NO_ARMOR, TAKE_DAMAGE, ARMOR_ENCHANTMENT, WEAPON_ENCHANTMENT, WEAPON, PROJECTILE,
        PLAYER_EFFECT, MOB_EFFECT, EXPLOSION, PLAYER_BURNING, MOB_BURNING,
        FISHING, TOTEM, TRICKSHOT, ENDER_PEARL_HIT, SHIELD_BLOCK;
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }
    public enum Difficulty {
        EASY, MEDIUM, HARD;
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }
    public Challenge {
        Objects.requireNonNull(type); Objects.requireNonNull(difficulty);
        if (target < 1) throw new IllegalArgumentException("Challenge target must be positive");
        if (!Double.isFinite(percentage) || percentage < 0 || percentage > 100 || counted(type) && percentage == 0)
            throw new IllegalArgumentException("Counted challenge percentage must be greater than zero and at most 100");
        ids = List.copyOf(ids); forbiddenSlots = List.copyOf(forbiddenSlots);
    }
    public Challenge(Type type, Difficulty difficulty, int target, List<ResourceLocation> ids, List<EquipmentSlot> slots) {
        this(type, difficulty, target, counted(type) ? defaultPercentage(difficulty) : 0, ids, slots);
    }
    public static boolean counted(Type type) { return type != Type.TIME && type != Type.NO_ARMOR && type != Type.ARMOR_ENCHANTMENT
            && type != Type.TOTEM; }
    public static double defaultPercentage(Difficulty difficulty) { return switch (difficulty) { case EASY -> 20; case MEDIUM -> 25; case HARD -> 100.0 / 3; }; }
    public int requiredCount(int waveEnemies) {
        return counted(type) ? Math.max(1, (int) Math.ceil(Math.max(0, waveEnemies) * percentage / 100.0 - 1.0E-9)) : target;
    }
    public Component description() { return description(-1); }
    public static int requiredEquipmentPlayers(int participants) { return Math.max(1, (participants + 2) / 3); }
    public Component description(int waveEnemies) {
        var detail = Component.empty();
        for (ResourceLocation id : ids) {
            if (!detail.getSiblings().isEmpty()) detail.append(", ");
            detail.append(registryName(id));
        }
        for (EquipmentSlot slot : forbiddenSlots) {
            if (!detail.getSiblings().isEmpty()) detail.append(", ");
            detail.append(Component.translatable("challenge.spiral_abyss.slot." + slot.getName()));
        }
        Object amount = type == Type.TIME ? (target % 60 == 0 ? Integer.toString(target / 60) : String.format(Locale.ROOT, "%.2f", target / 60.0))
                : counted(type) ? waveEnemies >= 0 ? requiredCount(waveEnemies) : String.format(Locale.ROOT, "%.2f%%", percentage) : target;
        boolean singular = counted(type) && waveEnemies >= 0 && requiredCount(waveEnemies) == 1;
        String suffix = singular || type == Type.TIME && target == 60 ? ".one" : "";
        return Component.translatable("challenge.spiral_abyss." + type.key() + suffix, amount, detail);
    }
    private Component registryName(ResourceLocation id) {
        return switch (type) {
            case ARMOR_ENCHANTMENT, WEAPON_ENCHANTMENT -> ChallengeCatalog.enchantment(id) != null
                    ? ChallengeCatalog.enchantment(id).value().description() : Component.literal(id.toString());
            case WEAPON -> BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id).getDescription() : Component.literal(id.toString());
            case PLAYER_EFFECT, MOB_EFFECT -> BuiltInRegistries.MOB_EFFECT.containsKey(id)
                    ? BuiltInRegistries.MOB_EFFECT.get(id).getDisplayName() : Component.literal(id.toString());
            case PROJECTILE -> BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                    ? BuiltInRegistries.ENTITY_TYPE.get(id).getDescription() : Component.literal(id.toString());
            default -> Component.literal(id.toString());
        };
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name()); tag.putString("Difficulty", difficulty.name()); tag.putInt("Target", target);
        tag.putDouble("Percentage", percentage);
        ListTag entries = new ListTag(); ids.forEach(id -> entries.add(StringTag.valueOf(id.toString()))); tag.put("Ids", entries);
        ListTag slots = new ListTag(); forbiddenSlots.forEach(slot -> slots.add(StringTag.valueOf(slot.getName()))); tag.put("Slots", slots);
        return tag;
    }
    public static Challenge load(CompoundTag tag) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Tag id : tag.getList("Ids", Tag.TAG_STRING)) {
            var parsed = ResourceLocation.tryParse(id.getAsString());
            if (parsed == null) throw new IllegalArgumentException("Invalid saved challenge ID");
            ids.add(parsed);
        }
        List<EquipmentSlot> slots = new ArrayList<>();
        for (Tag slot : tag.getList("Slots", Tag.TAG_STRING)) slots.add(EquipmentSlot.byName(slot.getAsString()));
        Type type = Type.valueOf(tag.getString("Type")); Difficulty difficulty = Difficulty.valueOf(tag.getString("Difficulty"));
        return new Challenge(type, difficulty, tag.getInt("Target"), tag.contains("Percentage") ? tag.getDouble("Percentage")
                : counted(type) ? defaultPercentage(difficulty) : 0, ids, slots);
    }
}
