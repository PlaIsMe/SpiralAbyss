package com.pla.spiral_abyss.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared imported clock artwork and time formatting for combat and cycle reset displays. */
public final class AbyssClockRenderer {
    private static final ResourceLocation CLOCK = ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "textures/gui/clock.png");
    private AbyssClockRenderer() {}
    public static String twoDigits(long value) { return value < 10 ? "0" + value : Long.toString(value); }
    public static void combat(GuiGraphics graphics, Font font, int centerX, int y, int seconds, float scale) {
        int x = centerX - 33;
        RenderSystem.enableBlend();
        graphics.blit(CLOCK, x, y, 66, 22, 0, 0, 651, 215, 651, 215);
        RenderSystem.disableBlend();
        seconds = Math.max(0, seconds);
        Component time = Component.translatable("hud.spiral_abyss.time", twoDigits(seconds / 60), twoDigits(seconds % 60));
        graphics.pose().pushPose();
        graphics.pose().translate(x + 44, y + (22 - font.lineHeight * scale) / 2, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawCenteredString(font, time, 0, 0, 0xFFFFFFFF);
        graphics.pose().popPose();
    }
    public static void reset(GuiGraphics graphics, Font font, int centerX, int y, int availableWidth, long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        Component text = millis < 0 ? Component.translatable("screen.spiral_abyss.reset_disabled")
                : Component.translatable("screen.spiral_abyss.reset_remaining", seconds / 86400,
                twoDigits(seconds / 3600 % 24), twoDigits(seconds / 60 % 60), twoDigits(seconds % 60));
        int width = 20 + font.width(text);
        float scale = Math.min(1, (float) Math.max(1, availableWidth) / width);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - width * scale / 2, y, 0);
        graphics.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        graphics.blit(CLOCK, 0, 0, 16, 16, 0, 0, 215, 215, 651, 215);
        RenderSystem.disableBlend();
        graphics.drawString(font, text, 20, (16 - font.lineHeight) / 2, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }
}
