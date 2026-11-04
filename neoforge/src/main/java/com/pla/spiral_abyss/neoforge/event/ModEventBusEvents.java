package com.pla.spiral_abyss.neoforge.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.init.EntityInit;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.KEY_ENTITY.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.COPPER_CHEST.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.IRON_CHEST.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.GOLDEN_CHEST.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.EXPERIENCE_CHEST.get(), KeyEntity.createAttributes().build());
    }
}
