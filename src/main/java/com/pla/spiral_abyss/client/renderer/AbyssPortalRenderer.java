package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.*;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

/** One continuous, double-sided End Portal surface, clipped around the slabs and stair treads. */
public final class AbyssPortalRenderer implements BlockEntityRenderer<AbyssPortalBlockEntity> {
    private final Font font;
    public AbyssPortalRenderer(BlockEntityRendererProvider.Context context) { font = context.getFont(); }
    @Override public void render(AbyssPortalBlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.endPortal());
        Matrix4f matrix = pose.last().pose();
        boolean xAxis = entity.frame().axis() == Direction.Axis.X;
        strip(vertices, matrix, xAxis, 0, 1, 0.5F, 1);
        strip(vertices, matrix, xAxis, -1, 2, 1, 1.5F);
        strip(vertices, matrix, xAxis, -1.5F, 2.5F, 1.5F, 2);
        strip(vertices, matrix, xAxis, -2, 3, 2, 5);
        strip(vertices, matrix, xAxis, -1.5F, 2.5F, 5, 5.5F);
        strip(vertices, matrix, xAxis, -1, 2, 5.5F, 6);
        strip(vertices, matrix, xAxis, 0, 1, 6, 6.5F);
        renderFloor(entity, pose, buffers, xAxis);
    }
    private void renderFloor(AbyssPortalBlockEntity entity, PoseStack pose, MultiBufferSource buffers, boolean xAxis) {
        if (entity.displayedFloor() == 0) return;
        // Vanilla enchantments use these translated Roman numeral labels (I through VI).
        Component label = Component.translatable("enchantment.level." + entity.displayedFloor());
        int color = entity.hostRunning() ? 0xFF555555 : 0xFFFFFFFF;
        for (int side : new int[] {0, 1}) {
            pose.pushPose();
            pose.translate(0.5, 3.5, 0.5);
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
    @Override public int getViewDistance() { return 128; }
}
