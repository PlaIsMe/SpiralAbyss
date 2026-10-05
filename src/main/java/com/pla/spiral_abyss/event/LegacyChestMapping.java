package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Old furniture is replaced by the session; opened bundles belong to saved profiles. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class LegacyChestMapping {
    @SubscribeEvent public static void missing(MissingMappingsEvent event) {
        event.getMappings(Registries.ENTITY_TYPE, SpiralAbyss.MOD_ID).stream()
                .filter(mapping -> mapping.getKey().getPath().equals("reward_chest"))
                .forEach(MissingMappingsEvent.Mapping::ignore);
    }
}
