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
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("encounter", com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay.HUD);
    }
    @SubscribeEvent
    public static void registerDimensionEffects(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(com.pla.spiral_abyss.client.renderer.AbyssSkyEffects.ID,
                new com.pla.spiral_abyss.client.renderer.AbyssSkyEffects());
    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.KEY_ENTITY_LAYER, KeyEntityModel::createBodyLayer);
        event.registerLayerDefinition(com.pla.spiral_abyss.client.renderer.ScrollEntityModel.LAYER_LOCATION,
                com.pla.spiral_abyss.client.renderer.ScrollEntityModel::createBodyLayer);
        event.registerLayerDefinition(com.pla.spiral_abyss.client.renderer.RewardChestModel.LAYER_LOCATION,
                com.pla.spiral_abyss.client.renderer.RewardChestModel::createBodyLayer);
    }
}
