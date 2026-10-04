package com.pla.spiral_abyss.init;

import com.mojang.serialization.Codec;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.loot.AbyssActivationLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.*;

public final class LootInit {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SpiralAbyss.MOD_ID);
    public static final RegistryObject<Codec<AbyssActivationLootModifier>> ABYSS_ACTIVATION = MODIFIERS.register("abyss_activation", () -> AbyssActivationLootModifier.CODEC);
    private LootInit() {}
}
