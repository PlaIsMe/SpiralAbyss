package com.pla.spiral_abyss.platform;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/** Keeps Forge's nullable lookup semantics for absent optional IDs. */
public final class AbyssRegistries {
    public static final Lookup<net.minecraft.world.item.Item> ITEMS =
            new Lookup<>(BuiltInRegistries.ITEM);
    public static final Lookup<net.minecraft.world.level.block.Block> BLOCKS =
            new Lookup<>(BuiltInRegistries.BLOCK);
    public static final Lookup<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES =
            new Lookup<>(BuiltInRegistries.ENTITY_TYPE);
    public static final Lookup<net.minecraft.world.effect.MobEffect> MOB_EFFECTS =
            new Lookup<>(BuiltInRegistries.MOB_EFFECT);
    public static final Lookup<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS =
            new Lookup<>(BuiltInRegistries.ENCHANTMENT);

    public record Lookup<T>(Registry<T> registry) {
        public boolean containsKey(ResourceLocation id) {
            return registry.containsKey(id);
        }

        public T getValue(ResourceLocation id) {
            return registry.containsKey(id) ? registry.get(id) : null;
        }

        public ResourceLocation getKey(T value) {
            return registry.getKey(value);
        }

        public Collection<T> getValues() {
            return registry.stream().toList();
        }
    }
}
