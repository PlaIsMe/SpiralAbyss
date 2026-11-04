package com.pla.spiral_abyss.forge.client;

import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;

import net.minecraftforge.client.event.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "spiral_abyss",
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ForgeClientEvents {
    @SubscribeEvent
    public static void JournalClientHandler_inventory(ScreenEvent.Init.Post event) {
        var shared =
                new AbyssClientEvents.ScreenInit(
                        event.getScreen(), widget -> event.addListener(widget));
        JournalClientHandler.inventory(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_renderInventory(ScreenEvent.Render.Pre event) {
        var shared = new AbyssClientEvents.ScreenEvent.Render.Pre(event.getScreen());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        JournalClientHandler.renderInventory(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_tick(TickEvent.ClientTickEvent event) {
        var shared =
                new AbyssClientEvents.TickEvent.ClientTickEvent(
                        AbyssClientEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        JournalClientHandler.tick(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_key(ScreenEvent.KeyPressed.Pre event) {
        var shared =
                new AbyssClientEvents.ScreenEvent.KeyPressed.Pre(
                        event.getScreen(), event.getKeyCode(), event.getScanCode());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        JournalClientHandler.key(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounterOverlay_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        AbyssEncounterOverlay.logout(shared);
    }

    @SubscribeEvent
    public static void RareRewardOverlay_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        RareRewardOverlay.logout(shared);
    }

    @SubscribeEvent
    public static void RareRewardOverlay_tick(TickEvent.ClientTickEvent event) {
        var shared =
                new AbyssClientEvents.TickEvent.ClientTickEvent(
                        AbyssClientEvents.TickEvent.Phase.valueOf(event.phase.name()));
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        RareRewardOverlay.tick(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        PortalEntryAnimation.logout(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_hud(RenderGuiEvent.Pre event) {
        var shared = new AbyssClientEvents.RenderGuiEvent.Pre();
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        PortalEntryAnimation.hud(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_hand(RenderHandEvent event) {
        var shared = new AbyssClientEvents.RenderHandEvent();
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        PortalEntryAnimation.hand(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_outline(RenderHighlightEvent.Block event) {
        var shared = new AbyssClientEvents.RenderHighlightEvent.Block(event.getTarget());
        if (event.isCancelable()) shared.bindCancellation(event::isCanceled, event::setCanceled);
        PortalEntryAnimation.outline(shared);
    }
}
