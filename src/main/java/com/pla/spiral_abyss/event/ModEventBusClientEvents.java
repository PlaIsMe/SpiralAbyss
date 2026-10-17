package com.pla.spiral_abyss.event;


import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.renderer.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("encounter", AbyssEncounterOverlay.HUD);
        event.registerAboveAll("rare_reward", RareRewardOverlay.HUD);
    }
    @SubscribeEvent
    public static void registerDimensionEffects(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(AbyssSkyEffects.ID,
                new AbyssSkyEffects());
    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.KEY_ENTITY_LAYER, KeyEntityModel::createBodyLayer);
        event.registerLayerDefinition(CopperChestModel.LAYER_LOCATION,
                CopperChestModel::createBodyLayer);
        event.registerLayerDefinition(IronChestModel.LAYER_LOCATION,
                IronChestModel::createBodyLayer);
        event.registerLayerDefinition(GoldenChestModel.LAYER_LOCATION,
                GoldenChestModel::createBodyLayer);
    }
}
