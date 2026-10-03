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
            event.accept(ItemInit.ABYSS_KEY);
        }
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            event.accept(ItemInit.ABYSS_BASE_BLOCK_ITEM);
            event.accept(ItemInit.ABYSS_PILLAR_ITEM);
            event.accept(ItemInit.ABYSS_RUNE_1_ITEM);
            event.accept(ItemInit.ABYSS_RUNE_2_ITEM);
            event.accept(ItemInit.ABYSS_RUNE_3_ITEM);
            event.accept(ItemInit.ABYSS_RUNE_4_ITEM);
            event.accept(ItemInit.ABYSS_BORDER_MIDDLE_ITEM);
            event.accept(ItemInit.ABYSS_BORDER_OUTSIDE_ITEM);
        }
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.KEY_ENTITY.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.REWARD_CHEST.get(), KeyEntity.createAttributes().build());
        event.put(EntityInit.SCROLL_ENTITY.get(), KeyEntity.createAttributes().build());
    }
}
