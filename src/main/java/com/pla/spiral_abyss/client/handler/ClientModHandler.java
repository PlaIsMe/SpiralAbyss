package com.pla.spiral_abyss.client.handler;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.renderer.*;
import com.pla.spiral_abyss.client.screen.AbyssClientConfigScreen;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.init.EntityInit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class ClientModHandler {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(SpiralAbyss.MOD_ID).orElseThrow()
                .registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new AbyssClientConfigScreen(parent));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(JournalClientHandler.OPEN);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.KEY_ENTITY.get(), KeyEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityInit.ABYSS_PORTAL.get(), AbyssPortalRenderer::new);
        event.registerEntityRenderer(EntityInit.COPPER_CHEST.get(), LootChestRenderer::copper);
        event.registerEntityRenderer(EntityInit.IRON_CHEST.get(), LootChestRenderer::iron);
        event.registerEntityRenderer(EntityInit.GOLDEN_CHEST.get(), LootChestRenderer::golden);
        event.registerEntityRenderer(EntityInit.CHEST_LOOT.get(), ChestLootRenderer::new);
        event.registerEntityRenderer(EntityInit.EXPERIENCE_CHEST.get(), ExperienceChestRenderer::new);
    }
}
