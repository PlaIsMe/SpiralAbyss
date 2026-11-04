package com.pla.spiral_abyss.neoforge.client;

import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import com.pla.spiral_abyss.platform.event.AbyssEvents;

import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.*;

@EventBusSubscriber(modid = "spiral_abyss", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class NeoForgeClientEvents {
    private static void bindCancellation(AbyssEvents.Event shared, Object nativeEvent) {
        if (nativeEvent instanceof net.neoforged.bus.api.ICancellableEvent event)
            shared.bindCancellation(event::isCanceled, event::setCanceled);
    }

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
        bindCancellation(shared, event);
        JournalClientHandler.renderInventory(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_tick(ClientTickEvent.Post event) {
        var shared = new AbyssClientEvents.ClientTickEvent.Post();
        bindCancellation(shared, event);
        JournalClientHandler.tick(shared);
    }

    @SubscribeEvent
    public static void JournalClientHandler_key(ScreenEvent.KeyPressed.Pre event) {
        var shared =
                new AbyssClientEvents.ScreenEvent.KeyPressed.Pre(
                        event.getScreen(), event.getKeyCode(), event.getScanCode());
        bindCancellation(shared, event);
        JournalClientHandler.key(shared);
    }

    @SubscribeEvent
    public static void AbyssEncounterOverlay_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        bindCancellation(shared, event);
        AbyssEncounterOverlay.logout(shared);
    }

    @SubscribeEvent
    public static void RareRewardOverlay_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        bindCancellation(shared, event);
        RareRewardOverlay.logout(shared);
    }

    @SubscribeEvent
    public static void RareRewardOverlay_tick(ClientTickEvent.Post event) {
        var shared = new AbyssClientEvents.ClientTickEvent.Post();
        bindCancellation(shared, event);
        RareRewardOverlay.tick(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var shared = new AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut();
        bindCancellation(shared, event);
        PortalEntryAnimation.logout(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_hud(RenderGuiEvent.Pre event) {
        var shared = new AbyssClientEvents.RenderGuiEvent.Pre();
        bindCancellation(shared, event);
        PortalEntryAnimation.hud(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_hand(RenderHandEvent event) {
        var shared = new AbyssClientEvents.RenderHandEvent();
        bindCancellation(shared, event);
        PortalEntryAnimation.hand(shared);
    }

    @SubscribeEvent
    public static void PortalEntryAnimation_outline(RenderHighlightEvent.Block event) {
        var shared = new AbyssClientEvents.RenderHighlightEvent.Block(event.getTarget());
        bindCancellation(shared, event);
        PortalEntryAnimation.outline(shared);
    }
}
