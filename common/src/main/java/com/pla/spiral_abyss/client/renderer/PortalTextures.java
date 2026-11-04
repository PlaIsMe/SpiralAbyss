package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Three shared 64x72 textures, updated at most ten times per second; never allocated per portal. */
final class PortalTextures {
    private static final Slot[] SLOTS = new Slot[3];
    private PortalTextures() {}

    static ResourceLocation texture(int state, int frame) {
        int mode = Math.max(0, Math.min(2, state));
        if (SLOTS[mode] == null) SLOTS[mode] = new Slot(mode);
        Slot slot = SLOTS[mode];
        int animationFrame = Math.floorMod(frame, PortalPixels.FRAMES);
        if (slot.frame != animationFrame) {
            NativeImage pixels = slot.texture.getPixels();
            if (pixels != null) {
                for (int y = 0; y < PortalPixels.HEIGHT; y++)
                    for (int x = 0; x < PortalPixels.WIDTH; x++)
                        pixels.setPixelRGBA(x, y, PortalPixels.pixel(x, y, mode, animationFrame));
                slot.texture.upload();
                slot.texture.setFilter(false, false);
            }
            slot.frame = animationFrame;
        }
        return slot.location;
    }

    private static final class Slot {
        private final ResourceLocation location;
        private final DynamicTexture texture;
        private int frame = -1;

        private Slot(int mode) {
            location = new ResourceLocation("spiral_abyss", "dynamic/portal_" + mode);
            texture = new DynamicTexture(new NativeImage(PortalPixels.WIDTH, PortalPixels.HEIGHT, false));
            texture.setFilter(false, false);
            Minecraft.getInstance().getTextureManager().register(location, texture);
        }
    }
}
