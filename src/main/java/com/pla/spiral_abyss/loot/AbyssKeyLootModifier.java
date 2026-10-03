package com.pla.spiral_abyss.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pla.spiral_abyss.init.ItemInit;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;

/** Adds a bonus without replacing End City loot or other mods' additions. */
public final class AbyssKeyLootModifier extends LootModifier {
    public static final Codec<AbyssKeyLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, AbyssKeyLootModifier::new));
    public AbyssKeyLootModifier(LootItemCondition[] conditions) { super(conditions); }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        loot.add(new ItemStack(ItemInit.ABYSS_KEY.get())); return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
