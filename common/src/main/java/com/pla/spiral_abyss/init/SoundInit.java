package com.pla.spiral_abyss.init;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundInit {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(SpiralAbyss.MOD_ID, Registries.SOUND_EVENT);
    public static final RegistrySupplier<SoundEvent> PORTAL_ENTRY = SOUNDS.register("portal.entry",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "portal.entry")));

    private SoundInit() {}
}
