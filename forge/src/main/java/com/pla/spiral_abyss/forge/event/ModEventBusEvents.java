package com.pla.spiral_abyss.forge.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.init.EntityInit;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
