package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.init.ItemInit;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Retire the old portal without leaving missing-registry prompts in existing worlds. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class LegacyPortalMappings {
    @SubscribeEvent
    public static void missingMappings(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, SpiralAbyss.MOD_ID)) {
            if (mapping.getKey().getPath().equals("abyss_portal_anchor")) mapping.remap(Blocks.POLISHED_ANDESITE_SLAB);
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, SpiralAbyss.MOD_ID)) {
            switch (mapping.getKey().getPath()) {
                case "abyss_key" -> mapping.remap(ItemInit.ABYSS_GATE_ACTIVATE_BLOCK_ITEM.get());
                case "abyss_draft" -> mapping.ignore();
            }
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, SpiralAbyss.MOD_ID)) {
            if (mapping.getKey().getPath().equals("abyss_portal")) mapping.ignore();
        }
    }
}
