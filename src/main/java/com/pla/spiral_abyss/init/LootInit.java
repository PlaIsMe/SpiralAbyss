package com.pla.spiral_abyss.init;

import com.mojang.serialization.Codec;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.loot.AbyssKeyLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.*;

public final class LootInit {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SpiralAbyss.MOD_ID);
    public static final RegistryObject<Codec<AbyssKeyLootModifier>> ABYSS_KEY = MODIFIERS.register("abyss_key", () -> AbyssKeyLootModifier.CODEC);
    private LootInit() {}
}
