package com.pla.spiral_abyss.event;


import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.init.EntityInit;
import com.pla.spiral_abyss.init.ItemInit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(ItemInit.ABYSS_DRAFT);
        }
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.KEY_ENTITY.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.REWARD_CHEST.get(), KeyEntity.createAttributes().build());
    }
}
