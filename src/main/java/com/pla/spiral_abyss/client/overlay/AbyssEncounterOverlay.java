package com.pla.spiral_abyss.client.overlay;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Display server snapshots without locally advancing the clock during wave delays or server pauses. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class AbyssEncounterOverlay {
    private static final float TIMER_SCALE = 1.5F;
    private static AbyssNetwork.EncounterStatus state = AbyssNetwork.EncounterStatus.HIDDEN;
    private AbyssEncounterOverlay() {}
    public static void update(AbyssNetwork.EncounterStatus status) { state = status; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = AbyssNetwork.EncounterStatus.HIDDEN;
    }
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft client = Minecraft.getInstance();
        if (!state.active() || client.options.hideGui || client.player == null || client.level == null
                || !client.level.dimension().equals(AbyssDimension.LEVEL)) return;
        var font = client.font;
        AbyssClockRenderer.combat(graphics, font, width / 2, 12, state.seconds(), TIMER_SCALE);
        int left = 12, top = Math.max(44, height / 4);
        graphics.drawString(font, Component.translatable("hud.spiral_abyss.defeated", state.defeated(), state.total()),
                left, top, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("hud.spiral_abyss.floor_wave", state.floor(), state.wave()),
                left, top + font.lineHeight + 6, 0xFFFFFFFF, true);
    };
}
