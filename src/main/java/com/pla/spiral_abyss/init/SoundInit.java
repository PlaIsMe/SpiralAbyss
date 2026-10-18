package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoundInit {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SpiralAbyss.MOD_ID);
    public static final RegistryObject<SoundEvent> PORTAL_ENTRY = SOUNDS.register("portal.entry",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "portal.entry")));

    private SoundInit() {}
}
