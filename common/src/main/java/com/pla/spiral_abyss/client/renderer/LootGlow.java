package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/** Additive, depth-tested light ribbons with a soft halo and bright center. */
public final class LootGlow extends RenderType {
    public static final int SILVER = 0xCCDFFF, CYAN = 0x36EDFF, GOLD = 0xFFCD42;
    public static final RenderType TYPE = create("spiral_abyss_loot_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private LootGlow(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
                     boolean sorting, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorting, setup, clear);
    }
    public static void ribbon(PoseStack pose, VertexConsumer out, Vec3 from, Vec3 to, Vec3 camera,
                              float startWidth, float endWidth, int color, float startAlpha, float endAlpha) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-8) return;
        Vec3 side = direction.cross(camera.subtract(from)).normalize();
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(1, 0, 0);
        // The outer two strips fade to zero alpha, avoiding a hard rectangular halo.
        float[] edges = {-1, -0.3f, 0.3f, 1};
        float[] opacity = {0, 1, 1, 0};
        for (int i = 0; i < 3; i++) {
            vertex(pose, out, from.add(side.scale(startWidth * edges[i])), color, startAlpha * opacity[i]);
            vertex(pose, out, from.add(side.scale(startWidth * edges[i + 1])), color, startAlpha * opacity[i + 1]);
            vertex(pose, out, to.add(side.scale(endWidth * edges[i + 1])), color, endAlpha * opacity[i + 1]);
            vertex(pose, out, to.add(side.scale(endWidth * edges[i])), color, endAlpha * opacity[i]);
        }
    }
    private static void vertex(PoseStack pose, VertexConsumer out, Vec3 point, int color, float alpha) {
        out.addVertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha);
    }
}
