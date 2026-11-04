package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Captured actual item models, fitted by visible alpha rather than mod-dependent FIXED transforms. */
final class BannerRewardRenderer {
    private record Key(String item, String tag, float tilt) {}
    private record Portrait(TextureTarget target, BannerPortrait.Crop crop) {}
    private final Map<Key, Portrait> portraits = new HashMap<>();
    private final Set<Key> unavailable = new HashSet<>();

    int fittedWidth(GuiGraphics graphics, ItemStack stack, int width, int height, float tilt) {
        Portrait portrait = portrait(graphics, stack, tilt);
        if (portrait == null) return width;
        var crop = portrait.crop();
        float scale = Math.min((float) width / crop.width(), (float) height / crop.height());
        return Math.max(1, Math.round(crop.width() * scale));
    }

    void render(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height, float tilt) {
        Portrait portrait = portrait(graphics, stack, tilt);
        if (portrait != null) BannerPortrait.draw(graphics, portrait.target(), portrait.crop(), x, y, width, height);
    }

    private Portrait portrait(GuiGraphics graphics, ItemStack stack, float tilt) {
        Key key = new Key(String.valueOf(AbyssRegistries.ITEMS.getKey(stack.getItem())), String.valueOf(stack.getTag()), tilt);
        if (unavailable.contains(key)) return null;
        try {
            Portrait portrait = portraits.get(key);
            if (portrait == null) {
                graphics.flush();
                portrait = capture(graphics, stack, tilt);
                portraits.put(key, portrait);
            }
            return portrait;
        } catch (RuntimeException | LinkageError exception) {
            unavailable.add(key);
            LogUtils.getLogger().warn("Cannot render Abyss reward model {}", key.item(), exception);
            return null;
        }
    }

    private Portrait capture(GuiGraphics graphics, ItemStack stack, float tilt) {
        Minecraft client = Minecraft.getInstance();
        BannerPortrait.RenderState previous = new BannerPortrait.RenderState();
        TextureTarget target = null;
        boolean success = false;
        try {
            target = new TextureTarget(512, 512, true, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
            target.setClearColor(0, 0, 0, 0);
            RenderSystem.disableScissor();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 512, 512, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().pushPose();
            try {
                RenderSystem.getModelViewStack().setIdentity();
                RenderSystem.applyModelViewMatrix();
                BannerPortrait.Crop crop = null;
                for (int attempt = 0; attempt < 4; attempt++) {
                    RenderSystem.depthMask(true);
                    target.clear(Minecraft.ON_OSX);
                    target.bindWrite(true);
                    RenderSystem.enableDepthTest();
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    Lighting.setupForFlatItems();
                    PoseStack pose = new PoseStack();
                    pose.translate(256, 256, -11000);
                    float size = 230F * (float) Math.pow(.5, attempt);
                    pose.scale(size, -size, size);
                    pose.mulPose(Axis.ZP.rotationDegrees(tilt));
                    pose.mulPose(Axis.YP.rotationDegrees(-18));
                    client.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose,
                            graphics.bufferSource(), client.level, 0);
                    graphics.flush();
                    crop = BannerPortrait.finish(target);
                    if (crop.x() > 0 && crop.y() > 0 && crop.x() + crop.width() < 512 && crop.y() + crop.height() < 512) break;
                }
                success = true;
                return new Portrait(target, crop);
            } finally {
                try { if (!success) graphics.flush(); }
                finally {
                    RenderSystem.getModelViewStack().popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        } finally {
            try { if (!success && target != null) target.destroyBuffers(); }
            finally { Lighting.setupFor3DItems(); previous.restore(); }
        }
    }

    void clear() {
        int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try { portraits.values().forEach(p -> p.target().destroyBuffers()); }
        finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        }
        portraits.clear();
        unavailable.clear();
    }
}
