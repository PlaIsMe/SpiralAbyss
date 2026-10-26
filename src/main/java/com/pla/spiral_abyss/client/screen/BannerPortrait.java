package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL13;


/** Shared bounded, outlined portraits for banner entities and item models. */
final class BannerPortrait {
    record Crop(int x, int y, int width, int height) {}
    static void draw(GuiGraphics graphics, TextureTarget portrait, Crop crop, int x, int y, int width, int height) {
        graphics.flush();
        RenderState previous = new RenderState();
        try {
            float scale = Math.min((float) width / crop.width, (float) height / crop.height);
            float drawWidth = crop.width * scale;
            float drawHeight = crop.height * scale;
            float left = x + (width - drawWidth) / 2;
            float top = y + (height - drawHeight) / 2;
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, portrait.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            Matrix4f matrix = graphics.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            // Framebuffer textures have their origin at the lower left.
            float u0 = (float) crop.x / portrait.width, u1 = (float) (crop.x + crop.width) / portrait.width;
            float v0 = 1F - (float) (crop.y + crop.height) / portrait.height, v1 = 1F - (float) crop.y / portrait.height;
            buffer.vertex(matrix, left, top + drawHeight, 0).uv(u0, v0).endVertex();
            buffer.vertex(matrix, left + drawWidth, top + drawHeight, 0).uv(u1, v0).endVertex();
            buffer.vertex(matrix, left + drawWidth, top, 0).uv(u1, v1).endVertex();
            buffer.vertex(matrix, left, top, 0).uv(u0, v1).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            previous.restore();
        }
    }

    /** Remove transparent margins once so large/small species occupy the same intended artwork area. */
    static Crop finish(TextureTarget target) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try (var pixels = new NativeImage(target.width, target.height, false)) {
            RenderSystem.bindTexture(target.getColorTextureId());
            pixels.downloadTexture(0, false);
            pixels.flipY();
            // One-time Manhattan distance field produces a crisp two-tone silhouette.
            // It follows the rendered alpha, including modded wings, weapons and armor.
            int[] distance = new int[target.width * target.height];
            for (int py = 0; py < target.height; py++) for (int px = 0; px < target.width; px++) {
                int i = py * target.width + px;
                distance[i] = (pixels.getPixelRGBA(px, py) >>> 24) >= 24 ? 0 : 10000;
                if (px > 0) distance[i] = Math.min(distance[i], distance[i - 1] + 1);
                if (py > 0) distance[i] = Math.min(distance[i], distance[i - target.width] + 1);
            }
            for (int py = target.height - 1; py >= 0; py--) for (int px = target.width - 1; px >= 0; px--) {
                int i = py * target.width + px;
                if (px + 1 < target.width) distance[i] = Math.min(distance[i], distance[i + 1] + 1);
                if (py + 1 < target.height) distance[i] = Math.min(distance[i], distance[i + target.width] + 1);
                if (distance[i] > 0 && distance[i] <= 6)
                    pixels.setPixelRGBA(px, py, distance[i] <= 3 ? 0xFF604735 : 0xFFF5EAD5);
            }
            int left = target.width, top = target.height, right = -1, bottom = -1;
            for (int py = 0; py < target.height; py++) for (int px = 0; px < target.width; px++) {
                if ((pixels.getPixelRGBA(px, py) >>> 24) < 24) continue;
                left = Math.min(left, px); right = Math.max(right, px);
                top = Math.min(top, py); bottom = Math.max(bottom, py);
            }
            pixels.flipY();
            pixels.upload(0, 0, 0, false);
            if (right < left) return new Crop(0, 0, target.width, target.height);
            left = Math.max(0, left - 4); top = Math.max(0, top - 4);
            right = Math.min(target.width, right + 5); bottom = Math.min(target.height, bottom + 5);
            return new Crop(left, top, right - left, bottom - top);
        } finally {
            RenderSystem.bindTexture(bound);
            RenderSystem.activeTexture(active);
        }
    }

    /** Preserve the caller's framebuffer, clip, transforms and common GL render state. */
    static final class RenderState {
        private final int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int[] viewport = new int[4];
        private final int[] scissor = new int[4];
        private final boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final int blendSource = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int blendDestination = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int alphaSource = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int alphaDestination = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        private final Matrix4f textureMatrix = new Matrix4f(RenderSystem.getTextureMatrix());
        private final VertexSorting sorting = RenderSystem.getVertexSorting();
        private final ShaderInstance shader = RenderSystem.getShader();
        private final float[] color = RenderSystem.getShaderColor().clone();
        private final float[] clearColor = new float[4];
        private final double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        private final int[] textures = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1),
                RenderSystem.getShaderTexture(2)};

        RenderState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        }

        void restore() {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setTextureMatrix(textureMatrix);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            RenderSystem.clearDepth(clearDepth);
            RenderSystem.setShader(() -> shader);
            for (int i = 0; i < textures.length; i++) RenderSystem.setShaderTexture(i, textures[i]);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            if (scissorEnabled) RenderSystem.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
            else RenderSystem.disableScissor();
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthWrite);
            RenderSystem.depthFunc(depthFunction);
            RenderSystem.blendFuncSeparate(blendSource, blendDestination, alphaSource, alphaDestination);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        }
    }

}
