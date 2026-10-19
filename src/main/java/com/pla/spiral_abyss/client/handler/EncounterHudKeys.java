package com.pla.spiral_abyss.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Local visibility only; encounter updates continue while the HUD is hidden. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class EncounterHudKeys {
    public static final KeyMapping TOGGLE = new KeyMapping("key.spiral_abyss.toggle_hud", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.spiral_abyss");
    private EncounterHudKeys() {}

    @Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(TOGGLE); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            if (client.screen == null && client.player != null && client.level != null
                    && client.level.dimension().equals(AbyssDimension.LEVEL)) AbyssEncounterOverlay.toggleVisibility();
        }
    }
}
