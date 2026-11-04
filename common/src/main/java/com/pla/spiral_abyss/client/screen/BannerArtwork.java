package com.pla.spiral_abyss.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Resolution-independent pixel artwork: silver paper, an Abyss seal, and indigo constellations. */
final class BannerArtwork implements AutoCloseable {
    private record Pixel(int x, int y, int width, int height, int color) {}
    private static final List<Pixel> ART = build();
    private ResourceLocation texture;

    void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (texture == null) {
            NativeImage image = new NativeImage(800, 400, true);
            for (Pixel pixel : ART) {
                int argb = pixel.color;
                int abgr = (argb & 0xFF00FF00) | (argb >> 16 & 0xFF) | (argb & 0xFF) << 16;
                image.fillRect(pixel.x, pixel.y, pixel.width, pixel.height, abgr);
            }
            texture = Minecraft.getInstance().getTextureManager().register("abyss_banner", new DynamicTexture(image));
        }
        graphics.fill(x + width, y + 6, x + width + 4, y + height + 6, 0x66071628);
        graphics.fill(x + 4, y + height, x + width, y + height + 6, 0x66071628);
        graphics.blit(texture, x, y, width, height, 0, 0, 800, 400, 800, 400);
    }

    @Override public void close() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(texture);
        texture = null;
    }

    private static List<Pixel> build() {
        List<Pixel> art = new ArrayList<>();
        add(art, 0, 0, 800, 400, 0xFFE5EDF3);
        add(art, 442, 0, 358, 400, 0xFF303665);
        // Quiet woven diamonds in the reading area; hard pixel edges retain the Minecraft style.
        for (int y = 20; y < 400; y += 48) for (int x = 18; x < 442; x += 48) {
            diamond(art, x, y, 19, 0xFFDDE7EF);
            diamond(art, x, y, 15, 0xFFE5EDF3);
            add(art, x - 1, y - 7, 2, 14, 0xFFDFE9F0);
        }
        // Constellation trails on the indigo edge.
        ring(art, 546, 195, 228, 1, 0xFF485380);
        ring(art, 546, 195, 244, 1, 0xFF3E4977);
        for (int i = 0; i < 15; i++) {
            int x = 465 + (i * 83 % 309), y = 25 + (i * 59 % 350);
            star(art, x, y, i % 3 == 0 ? 5 : 2, 0xFF889FC0);
        }
        // A stepped circular seal recalls the reference medallion without its orange palette.
        disk(art, 534, 200, 184, 0xFFC4D5E2);
        ring(art, 534, 200, 184, 7, 0xFFF1F5F8);
        ring(art, 534, 200, 171, 3, 0xFF91ADCA);
        ring(art, 534, 200, 164, 2, 0xFFE6F0F5);
        ring(art, 534, 200, 136, 2, 0xFFB4CADC);
        diamond(art, 534, 200, 115, 0xFFB3CADB);
        diamond(art, 534, 200, 110, 0xFFC4D5E2);
        diamond(art, 534, 200, 76, 0xFFB9CFDF);
        diamond(art, 534, 200, 69, 0xFFC4D5E2);
        star(art, 534, 200, 40, 0xFFB1CADC);
        for (int[] point : new int[][]{{534, 25}, {534, 375}, {359, 200}, {709, 200}}) {
            diamond(art, point[0], point[1], 10, 0xFFF1F5F8);
            diamond(art, point[0], point[1], 5, 0xFF789BBB);
        }
        // Fine double border with clipped pixel corners.
        for (int inset : new int[]{4, 9}) {
            int color = inset == 4 ? 0xFF9AB2CB : 0xFFB8CBDD;
            add(art, 18, inset, 764, 1, color); add(art, 18, 399 - inset, 764, 1, color);
            add(art, inset, 18, 1, 364, color); add(art, 799 - inset, 18, 1, 364, color);
        }
        for (int x : new int[]{13, 786}) for (int y : new int[]{13, 386}) star(art, x, y, 5, 0xFF95AEC6);
        return List.copyOf(art);
    }
    private static void add(List<Pixel> art, int x, int y, int width, int height, int color) {
        int right = Math.min(800, x + width), bottom = Math.min(400, y + height);
        x = Math.max(0, x); y = Math.max(0, y);
        if (right > x && bottom > y) art.add(new Pixel(x, y, right - x, bottom - y, color));
    }
    private static void disk(List<Pixel> art, int cx, int cy, int radius, int color) {
        for (int y = -radius; y < radius; y += 4) {
            int half = (int) Math.sqrt(Math.max(0, radius * radius - (y + 2) * (y + 2))) / 4 * 4;
            add(art, cx - half, cy + y, half * 2, 4, color);
        }
    }
    private static void ring(List<Pixel> art, int cx, int cy, int radius, int thickness, int color) {
        for (int y = -radius; y < radius; y += 4) {
            int half = (int) Math.sqrt(Math.max(0, radius * radius - (y + 2) * (y + 2))) / 4 * 4;
            int inner = (int) Math.sqrt(Math.max(0, (radius - thickness) * (radius - thickness) - (y + 2) * (y + 2))) / 4 * 4;
            int line = Math.max(2, half - inner);
            add(art, cx - half, cy + y, line, 4, color); add(art, cx + half - line, cy + y, line, 4, color);
        }
    }
    private static void diamond(List<Pixel> art, int cx, int cy, int radius, int color) {
        for (int y = -radius; y < radius; y += 3) {
            int half = radius - Math.abs(y);
            add(art, cx - half, cy + y, half * 2, 3, color);
        }
    }
    private static void star(List<Pixel> art, int cx, int cy, int radius, int color) {
        add(art, cx - 1, cy - radius, 2, 2 * radius + 1, color);
        add(art, cx - radius, cy - 1, 2 * radius + 1, 2, color);
        diamond(art, cx, cy, Math.max(2, radius / 2), color);
    }
}
