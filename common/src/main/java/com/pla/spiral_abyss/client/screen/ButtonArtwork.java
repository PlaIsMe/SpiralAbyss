package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Visible UV bounds of the original exports; the source PNGs stay untouched.
 * Each state uses the union of its family's alpha bounds to prevent hover/selection jitter. */
record ButtonArtwork(ResourceLocation normal, ResourceLocation hover, ResourceLocation chosen,
                     int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
    static final ButtonArtwork BACK = new ButtonArtwork(texture("back_button"), texture("back_button_hover"),
            texture("back_button"), 111, 0, 277, 500, 500, 500);
    static final ButtonArtwork FIGHT = new ButtonArtwork(texture("fight_button"), texture("fight_button_hover"),
            texture("fight_button"), 0, 0, 497, 189, 500, 200);
    static final ButtonArtwork WAVE = new ButtonArtwork(texture("wave_button_not_chosen"), texture("wave_button_hover"),
            texture("wave_button_chosen"), 0, 0, 750, 150, 750, 150);

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(SpiralAbyss.MOD_ID, "textures/gui/buttons/" + name + ".png");
    }
    int fittedWidth(int maxWidth, int maxHeight) {
        return Math.max(1, Math.min(maxWidth, maxHeight * sourceWidth / sourceHeight));
    }
    int fittedHeight(int width) { return Math.max(1, Math.round((float) width * sourceHeight / sourceWidth)); }

    void render(GuiGraphics graphics, int x, int y, int width, int height, boolean highlighted, boolean selected, boolean enabled) {
        ResourceLocation image = selected ? chosen : highlighted && enabled ? hover : normal;
        RenderSystem.enableBlend();
        graphics.setColor(enabled || selected ? 1F : 0.45F, enabled || selected ? 1F : 0.45F,
                enabled || selected ? 1F : 0.45F, 1F);
        graphics.blit(image, x, y, width, height, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight);
        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}
