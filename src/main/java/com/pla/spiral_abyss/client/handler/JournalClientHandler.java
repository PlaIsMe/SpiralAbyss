package com.pla.spiral_abyss.client.handler;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.screen.AbyssJournalScreen;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.client.screen.JournalNavigation;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.pla.spiral_abyss.world.AbyssDimension;

@EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class JournalClientHandler {
    public static final KeyMapping OPEN = new KeyMapping("key.spiral_abyss.menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.spiral_abyss");
    private JournalClientHandler() {}
    public static void open(AbyssNetwork.OpenJournal message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !client.player.level().dimension()
                .equals(AbyssDimension.LEVEL))
            client.setScreen(new AbyssJournalScreen(message));
    }
    @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof CreativeModeInventoryScreen)) return;
        event.addListener(JournalNavigation.icon(8, 8, 22, 22,
                JournalNavigation.MENU_ICON, 256, "screen.spiral_abyss.floors", false, AbyssNetwork::requestJournal));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {

        Minecraft client = Minecraft.getInstance();
        while (OPEN.consumeClick()) if (client.screen == null && client.player != null) AbyssNetwork.requestJournal();
    }
    @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (!OPEN.matches(event.getKeyCode(), event.getScanCode())) return;
        var screen = event.getScreen();
        if (screen instanceof AbyssJournalScreen || screen instanceof FloorSelectionScreen) {
            Minecraft.getInstance().setScreen(null); event.setCanceled(true);
        } else if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            AbyssNetwork.requestJournal(); event.setCanceled(true);
        }
    }
}
