package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import org.lwjgl.opengl.GL11;

/** Screen-owned blur of the current game frame, applied before either journal panel is drawn. */
final class JournalWorldBackdrop implements AutoCloseable {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            "spiral_abyss", "shaders/post/journal_blur.json");
    private PostChain blur;
    private RenderTarget source;
    private int pixelWidth, pixelHeight;
    private boolean unavailable;

    void render(GuiGraphics graphics, int width, int height, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && !unavailable) {
            graphics.flush();
            BannerPortrait.RenderState previous = new BannerPortrait.RenderState();
            try {
                RenderTarget main = client.getMainRenderTarget();
                if (source != main) close();
                if (blur == null) {
                    blur = new PostChain(client.getTextureManager(), client.getResourceManager(), main, EFFECT);
                    source = main;
                }
                if (pixelWidth != main.width || pixelHeight != main.height) {
                    blur.resize(main.width, main.height);
                    pixelWidth = main.width;
                    pixelHeight = main.height;
                }
                RenderSystem.disableScissor();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                blur.process(partialTick);
                // PostPass writes a fullscreen depth value. The following GUI needs a clean
                // depth plane, otherwise only our depth-independent portrait quads remain visible.
                main.bindWrite(false);
                RenderSystem.depthMask(true);
                RenderSystem.clearDepth(1.0);
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            } catch (IOException | RuntimeException exception) {
                close();
                unavailable = true;
                LogUtils.getLogger().warn("Cannot apply Abyss journal background blur", exception);
            } finally {
                previous.restore();
            }
        }
        // Keep the live world recognizable while separating it gently from the journal.
        graphics.fill(0, 0, width, height, client.level == null ? 0xFF101A2A : 0x3010192B);
    }

    @Override public void close() {
        BannerPortrait.RenderState previous = new BannerPortrait.RenderState();
        try {
            if (blur != null) blur.close();
        } finally {
            blur = null;
            source = null;
            pixelWidth = pixelHeight = 0;
            previous.restore();
        }
    }
}
