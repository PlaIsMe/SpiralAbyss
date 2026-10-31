package com.pla.spiral_abyss.init;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SoundInit {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, SpiralAbyss.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_ENTRY = SOUNDS.register("portal.entry",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "portal.entry")));

    private SoundInit() {}
}
