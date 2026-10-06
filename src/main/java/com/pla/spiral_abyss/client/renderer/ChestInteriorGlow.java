package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

/** Filled chest light geometry, independent of the render pipeline. */
public final class ChestInteriorGlow {
    private ChestInteriorGlow() {}
    /** A filled light field across the chest opening; overlapping slices give the light depth. */
    public static void render(PoseStack pose, VertexConsumer out, float time, int seed, int color, float fade) {
        int columns = 10, rows = 8;
        for (int layer = 0; layer < 4; layer++) {
            double y = 0.57 + layer * 0.09;
            float strength = fade * (layer == 0 ? 0.78f : 0.15f / layer);
            for (int x = 0; x < columns; x++) for (int z = 0; z < rows; z++) {
                for (int corner = 0; corner < 4; corner++) {
                    int gx = x + (corner == 1 || corner == 2 ? 1 : 0);
                    int gz = z + (corner >= 2 ? 1 : 0);
                    float nx = gx / (float) columns, nz = gz / (float) rows;
                    float edge = Math.min(1, Math.min(Math.min(nx, 1 - nx), Math.min(nz, 1 - nz)) * 10);
                    float shimmer = 0.7f + 0.3f * (float) Math.sin(time * 0.12 + gx * 1.7 + gz * 2.3 + seed);
                    vertex(pose, out, new Vec3((nx - 0.5) * 1.08, y, (nz - 0.5) * 0.8),
                            color, strength * edge * shimmer);
                }
            }
        }
    }
    private static void vertex(PoseStack pose, VertexConsumer out, Vec3 point, int color, float alpha) {
        out.vertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha).endVertex();
    }
}
