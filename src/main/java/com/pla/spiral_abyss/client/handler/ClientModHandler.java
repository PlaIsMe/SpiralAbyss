package com.pla.spiral_abyss.client.handler;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.renderer.*;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.init.EntityInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModHandler {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.KEY_ENTITY.get(), KeyEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityInit.ABYSS_PORTAL.get(), AbyssPortalRenderer::new);
        event.registerEntityRenderer(EntityInit.COPPER_CHEST.get(), LootChestRenderer::copper);
        event.registerEntityRenderer(EntityInit.IRON_CHEST.get(), LootChestRenderer::iron);
        event.registerEntityRenderer(EntityInit.GOLDEN_CHEST.get(), LootChestRenderer::golden);
        event.registerEntityRenderer(EntityInit.CHEST_LOOT.get(), ChestLootRenderer::new);
    }
}
