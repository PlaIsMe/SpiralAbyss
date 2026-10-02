package com.pla.spiral_abyss.client.handler;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.renderer.KeyEntityRenderer;
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
        event.registerEntityRenderer(EntityInit.REWARD_CHEST.get(), com.pla.spiral_abyss.client.renderer.RewardChestRenderer::new);
    }
}
