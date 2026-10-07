package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.*;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.lang.reflect.Method;

/** One continuous, double-sided portal surface, clipped around the gate frame and controller. */
public final class AbyssPortalRenderer implements BlockEntityRenderer<AbyssPortalBlockEntity> {
    private static final ResourceLocation PORTAL_TEXTURE = new ResourceLocation("minecraft", "textures/entity/end_portal.png");
    private static final Method IRIS_PACK_ACTIVE = irisPackActive();
    private final Font font;
    public AbyssPortalRenderer(BlockEntityRendererProvider.Context context) { font = context.getFont(); }
    @Override public void render(AbyssPortalBlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (!entity.getBlockState().getValue(com.pla.spiral_abyss.block.AbyssGateActivationBlock.ACTIVE)) return;
        pose.pushPose();
        var offset = entity.frame().base().subtract(entity.getBlockPos());
        pose.translate(offset.getX(), offset.getY(), offset.getZ());
        Matrix4f matrix = pose.last().pose();
        boolean xAxis = entity.frame().axis() == Direction.Axis.X;
        if (shaderPackActive()) {
            // End Portal's special shader is skipped by Oculus shader pipelines. An ordinary
            // full-bright entity layer lets the pack render the same star texture as geometry.
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(PORTAL_TEXTURE));
            texturedStrip(vertices, pose, xAxis, 0, 1, 0.5F, 1);
            texturedStrip(vertices, pose, xAxis, -1, 2, 1, 1.5F);
            texturedStrip(vertices, pose, xAxis, -1.5F, 2.5F, 1.5F, 2);
            texturedStrip(vertices, pose, xAxis, -1.5F, 2.5F, 2, 4);
            texturedStrip(vertices, pose, xAxis, -1.5F, 2.5F, 4, 4.5F);
            texturedStrip(vertices, pose, xAxis, -1, 2, 4.5F, 5);
        } else {
            VertexConsumer vertices = buffers.getBuffer(RenderType.endPortal());
            strip(vertices, matrix, xAxis, 0, 1, 0.5F, 1);
            strip(vertices, matrix, xAxis, -1, 2, 1, 1.5F);
            strip(vertices, matrix, xAxis, -1.5F, 2.5F, 1.5F, 2);
            strip(vertices, matrix, xAxis, -1.5F, 2.5F, 2, 4);
            strip(vertices, matrix, xAxis, -1.5F, 2.5F, 4, 4.5F);
            strip(vertices, matrix, xAxis, -1, 2, 4.5F, 5);
        }
        renderFloor(entity, pose, buffers, xAxis);
        pose.popPose();
    }
    private void renderFloor(AbyssPortalBlockEntity entity, PoseStack pose, MultiBufferSource buffers, boolean xAxis) {
        if (entity.displayedFloor() == 0) return;
        // Vanilla enchantments use these translated Roman numeral labels (I through VI).
        Component label = Component.translatable("enchantment.level." + entity.displayedFloor());
        int color = entity.hostRunning() ? 0xFF555555 : 0xFFFFFFFF;
        for (int side : new int[] {0, 1}) {
            pose.pushPose();
            pose.translate(0.5, 3, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees((xAxis ? 0 : 90) + side * 180));
            pose.translate(0, 0, 0.015);
            pose.scale(0.12F, -0.12F, 0.12F);
            font.drawInBatch(label, -font.width(label) / 2F, -font.lineHeight / 2F, color, false,
                    pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
    private static void strip(VertexConsumer vertices, Matrix4f matrix, boolean xAxis, float left, float right, float low, float high) {
        vertex(vertices, matrix, xAxis, left, low);
        vertex(vertices, matrix, xAxis, right, low);
        vertex(vertices, matrix, xAxis, right, high);
        vertex(vertices, matrix, xAxis, left, high);
        vertex(vertices, matrix, xAxis, left, high);
        vertex(vertices, matrix, xAxis, right, high);
        vertex(vertices, matrix, xAxis, right, low);
        vertex(vertices, matrix, xAxis, left, low);
    }
    private static void vertex(VertexConsumer vertices, Matrix4f matrix, boolean xAxis, float across, float y) {
        vertices.vertex(matrix, xAxis ? across : 0.5F, y, xAxis ? 0.5F : across).endVertex();
    }
    private static void texturedStrip(VertexConsumer vertices, PoseStack pose, boolean xAxis,
                                      float left, float right, float low, float high) {
        float normal = xAxis ? 1 : -1;
        texturedVertex(vertices, pose, xAxis, left, low, normal);
        texturedVertex(vertices, pose, xAxis, right, low, normal);
        texturedVertex(vertices, pose, xAxis, right, high, normal);
        texturedVertex(vertices, pose, xAxis, left, high, normal);
        texturedVertex(vertices, pose, xAxis, left, high, -normal);
        texturedVertex(vertices, pose, xAxis, right, high, -normal);
        texturedVertex(vertices, pose, xAxis, right, low, -normal);
        texturedVertex(vertices, pose, xAxis, left, low, -normal);
    }
    private static void texturedVertex(VertexConsumer vertices, PoseStack pose, boolean xAxis,
                                       float across, float y, float normal) {
        vertices.vertex(pose.last().pose(), xAxis ? across : 0.5F, y, xAxis ? 0.5F : across)
                .color(85, 185, 220, 255).uv((across + 1.5F) / 4F, (y - 0.5F) / 4.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.last().normal(), xAxis ? 0 : normal, 0, xAxis ? normal : 0).endVertex();
    }
    private static Method irisPackActive() {
        if (!ModList.get().isLoaded("oculus")) return null;
        try {
            return Class.forName("net.irisshaders.iris.Iris").getMethod("isPackInUseQuick");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Oculus shader state is unavailable", exception);
        }
    }
    private static boolean shaderPackActive() {
        if (IRIS_PACK_ACTIVE == null) return false;
        try {
            return (boolean) IRIS_PACK_ACTIVE.invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Oculus shader state could not be read", exception);
        }
    }
    @Override public int getViewDistance() { return 128; }
}
