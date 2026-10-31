package com.pla.spiral_abyss.event;


import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.renderer.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "encounter"), AbyssEncounterOverlay.HUD);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "rare_reward"), RareRewardOverlay.HUD);
    }
    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(AbyssSkyEffects.ID,
                new AbyssSkyEffects());
    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.KEY_ENTITY_LAYER, KeyEntityModel::createBodyLayer);
        event.registerLayerDefinition(ExperienceChestModel.LAYER_LOCATION, ExperienceChestModel::createBodyLayer);
        event.registerLayerDefinition(CopperChestModel.LAYER_LOCATION,
                CopperChestModel::createBodyLayer);
        event.registerLayerDefinition(IronChestModel.LAYER_LOCATION,
                IronChestModel::createBodyLayer);
        event.registerLayerDefinition(GoldenChestModel.LAYER_LOCATION,
                GoldenChestModel::createBodyLayer);
    }
}
