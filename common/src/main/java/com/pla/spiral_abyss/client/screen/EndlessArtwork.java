package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.stream.IntStream;
import net.minecraft.Util;

/** All 300 reference GIF frames, retaining its repeating 30/40/30 ms timing. */
final class EndlessArtwork {
    private static final ResourceLocation[] PAGES = IntStream.range(0, 5)
            .mapToObj(i -> new ResourceLocation("spiral_abyss", "textures/gui/floors/endless_" + i + ".png"))
            .toArray(ResourceLocation[]::new);
    static void render(GuiGraphics graphics, int x, int y, int width, int height) {
        long time = Util.getMillis() % 10000;
        int remainder = (int) (time % 100);
        int frame = (int) (time / 100) * 3 + (remainder < 30 ? 0 : remainder < 70 ? 1 : 2);
        int tile = frame % 60;
        RenderSystem.enableBlend();
        graphics.blit(PAGES[frame / 60], x, y, width, height, (tile % 10) * 128, (tile / 10) * 228,
                128, 228, 1280, 1368);
        RenderSystem.disableBlend();
    }
}
