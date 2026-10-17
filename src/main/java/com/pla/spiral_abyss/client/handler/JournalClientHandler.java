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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class JournalClientHandler {
    public static final KeyMapping OPEN = new KeyMapping("key.spiral_abyss.menu", KeyConflictContext.IN_GAME,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.spiral_abyss");
    private JournalClientHandler() {}
    public static void open(AbyssNetwork.OpenJournal message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !client.player.level().dimension()
                .equals(com.pla.spiral_abyss.world.AbyssDimension.LEVEL))
            client.setScreen(new AbyssJournalScreen(message));
    }
    @Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof CreativeModeInventoryScreen)) return;
        event.addListener(JournalNavigation.icon(8, 8, 22, 22,
                JournalNavigation.MENU_ICON, 256, "screen.spiral_abyss.floors", false, AbyssNetwork::requestJournal));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
