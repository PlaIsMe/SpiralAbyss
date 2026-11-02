package com.pla.spiral_abyss.client.handler;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.screen.AbyssJournalScreen;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.client.screen.JournalNavigation;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class JournalClientHandler {
    private static final Map<Screen, Button> INVENTORY_BUTTONS = new WeakHashMap<>();
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
        var screen = event.getScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
        Button button = JournalNavigation.icon(0, 0, 22, 22,
                JournalNavigation.MENU_ICON, 256, "screen.spiral_abyss.floors", false, AbyssNetwork::requestJournal);
        positionButton(screen, button);
        INVENTORY_BUTTONS.put(screen, button);
        event.addListener(button);
    }
    @SubscribeEvent public static void renderInventory(ScreenEvent.Render.Pre event) {
        Button button = INVENTORY_BUTTONS.get(event.getScreen());
        if (button != null) positionButton(event.getScreen(), button);
    }
    private static void positionButton(Screen screen, Button button) {
        // Move the existing widget, so its rendering and click target stay together on hot reload.
        button.setX(Math.min(AbyssClientConfig.INVENTORY_BUTTON_X.get(), Math.max(0, screen.width - button.getWidth())));
        button.setY(Math.min(AbyssClientConfig.INVENTORY_BUTTON_Y.get(), Math.max(0, screen.height - button.getHeight())));
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
