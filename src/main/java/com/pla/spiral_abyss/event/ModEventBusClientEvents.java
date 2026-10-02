package com.pla.spiral_abyss.event;


import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.renderer.KeyEntityModel;
import com.pla.spiral_abyss.client.renderer.ModModelLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.KEY_ENTITY_LAYER, KeyEntityModel::createBodyLayer);
        event.registerLayerDefinition(com.pla.spiral_abyss.client.renderer.RewardChestModel.LAYER_LOCATION,
                com.pla.spiral_abyss.client.renderer.RewardChestModel::createBodyLayer);
    }
}
