package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.*;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** One continuous, double-sided portal surface, clipped around the gate frame and controller. */
public final class AbyssPortalRenderer implements BlockEntityRenderer<AbyssPortalBlockEntity> {
    @Override public boolean shouldRenderOffScreen(com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity portal) { return dev.architectury.platform.Platform.isFabric(); }

    private final Font font;
    public AbyssPortalRenderer(BlockEntityRendererProvider.Context context) { font = context.getFont(); }
    public AABB getRenderBoundingBox(AbyssPortalBlockEntity entity) {
        return entity.getRenderBoundingBox();
    }
    @Override public void render(AbyssPortalBlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (!entity.getBlockState().getValue(AbyssGateActivationBlock.ACTIVE)) return;
        pose.pushPose();
        var offset = entity.frame().base().subtract(entity.getBlockPos());
        pose.translate(offset.getX(), offset.getY(), offset.getZ());
        boolean xAxis = entity.frame().axis() == Direction.Axis.X;
        long tick = entity.getLevel() == null ? 0 : entity.getLevel().getGameTime();
        float entry = PortalEntryAnimation.intensity(entity, partialTick);
        int state = entry > .55F ? 2 : entry > 0 || entity.displayedFloor() > 0 ? 1 : 0;
        ResourceLocation texture = PortalTextures.texture(state, (int) ((tick / 2) % PortalPixels.FRAMES));
        // Ordinary emissive entity shading preserves the cyan palette without directional diffuse dimming.
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture, false));
        texturedStrip(vertices, pose, xAxis, 0, 1, 0.5F, 1);
        texturedStrip(vertices, pose, xAxis, -1, 2, 1, 1.5F);
        texturedStrip(vertices, pose, xAxis, -1.5F, 2.5F, 1.5F, 2);
        texturedStrip(vertices, pose, xAxis, -1.5F, 2.5F, 2, 4);
        texturedStrip(vertices, pose, xAxis, -1.5F, 2.5F, 4, 4.5F);
        texturedStrip(vertices, pose, xAxis, -1, 2, 4.5F, 5);
        var client = Minecraft.getInstance();
        double distance = client.gameRenderer.getMainCamera().getPosition()
                .distanceTo(Vec3.atCenterOf(entity.frame().base()).add(0, 2.3, 0));
        int detail = switch (client.options.particles().get()) {
            case ALL -> 2;
            case DECREASED -> 1;
            case MINIMAL -> 0;
        };
        float fade = (float) Math.max(0, Math.min(1, (48 - distance) / 16));
        if (detail > 0 && fade > 0)
            PortalAura.render(pose, buffers.getBuffer(LootGlow.TYPE), xAxis, (tick % 120000) + partialTick,
                    entity.getBlockPos().hashCode(), detail, fade);
        renderFloor(entity, pose, buffers, xAxis);
        PortalFrameGlow.render(entity, pose, buffers);
        pose.popPose();
    }
    private void renderFloor(AbyssPortalBlockEntity entity, PoseStack pose, MultiBufferSource buffers, boolean xAxis) {
        if (entity.displayedFloor() == 0) return;
        // Vanilla enchantments use these translated Roman numeral labels (I through VI).
        Component label = entity.displayedFloor() == 7 ? Component.literal("\u221e")
                : Component.translatable("enchantment.level." + entity.displayedFloor());
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
    private static void texturedStrip(VertexConsumer vertices, PoseStack pose, boolean xAxis,
                                      float left, float right, float low, float high) {
        float normal = xAxis ? 1 : -1;
        texturedVertex(vertices, pose, xAxis, left, low, normal);
        texturedVertex(vertices, pose, xAxis, right, low, normal);
        texturedVertex(vertices, pose, xAxis, right, high, normal);
        texturedVertex(vertices, pose, xAxis, left, high, normal);
        // The emissive layer has NO_CULL; duplicate reverse faces would blend translucent idle sheets twice.
    }
    private static void texturedVertex(VertexConsumer vertices, PoseStack pose, boolean xAxis,
                                       float across, float y, float normal) {
        vertices.addVertex(pose.last().pose(), xAxis ? across : 0.5F, y, xAxis ? 0.5F : across)
                .setColor(255, 255, 255, 255).setUv((across + 1.5F) / 4F, (5F - y) / 4.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), xAxis ? 0 : normal, 0, xAxis ? normal : 0);
    }
    @Override public int getViewDistance() { return 128; }
}
