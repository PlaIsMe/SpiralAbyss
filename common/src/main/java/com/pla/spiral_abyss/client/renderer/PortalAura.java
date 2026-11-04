package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Local deterministic light, with a fixed geometry budget and no particle entities or server state. */
final class PortalAura {
    private PortalAura() {}

    static void render(PoseStack pose, VertexConsumer out, boolean xAxis, float ticks,
                       int seed, int detail, float fade) {
        if (detail <= 0 || fade <= 0) return;
        int motes = detail == 1 ? 12 : 28;
        int rays = detail == 1 ? 4 : 8;
        float time = ticks + Math.floorMod(seed, 719);
        for (int i = 0; i < motes; i++) {
            float progress = fraction(time * (.0035F + (i % 3) * .0006F) + i * .618034F);
            float side = (i & 1) == 0 ? -1 : 1;
            float x = .5F + side * (1.55F + fraction(i * .371F) * .82F)
                    + (float) Math.sin(time * .025F + i * 2.1F) * .10F;
            if (i % 4 == 0) x = .5F + (fraction(i * .419F + .15F) - .5F) * 3.8F;
            float y = .3F + progress * 5.25F;
            float depth = .5F + ((i & 2) == 0 ? -1 : 1) * (.22F + fraction(i * .271F) * .65F);
            if (i % 4 == 0) depth = .5F + ((i & 4) == 0 ? -1 : 1) * 1.05F;
            float alpha = (float) Math.sin(progress * Math.PI) * fade * .68F;
            float size = .018F + (i % 3) * .009F;
            quad(pose, out, xAxis, x - size, y - size, x + size, y + size, depth, 0x77EFFF, alpha, alpha);
            if (i % 5 == 0) {
                // Tiny angular corner glints echo the reference glyphs without importing symbols.
                quad(pose, out, xAxis, x + size, y - size, x + size * 3, y, depth, 0x39B9EC, alpha * .6F, alpha * .6F);
                quad(pose, out, xAxis, x + size * 2, y, x + size * 3, y + size * 3, depth, 0x39B9EC, alpha * .6F, alpha * .6F);
            }
        }
        for (int i = 0; i < rays; i++) {
            float progress = fraction(time * .0028F + i * .618034F);
            float x = .5F + ((i & 1) == 0 ? -1 : 1) * (1.85F + fraction(i * .419F) * .62F);
            if (i % 4 == 0) x = .5F + (i == 0 ? -.85F : .85F);
            float y = .25F + progress * 4.7F;
            float depth = .5F + ((i & 2) == 0 ? -1 : 1) * (.18F + fraction(i * .317F) * .65F);
            float length = .45F + fraction(i * .713F) * .90F;
            float alpha = (float) Math.sin(progress * Math.PI) * fade * .50F;
            // Exactly vertical, thin cyan shafts fade upward; dim wider skirts give restrained glow.
            quad(pose, out, xAxis, x - .025F, y, x + .025F, y + length, depth, 0x258FD3, alpha * .25F, 0);
            quad(pose, out, xAxis, x - .006F, y, x + .006F, y + length, depth, 0x69E3FF, alpha, 0);
        }
    }

    private static float fraction(float value) { return value - (float) Math.floor(value); }

    private static void quad(PoseStack pose, VertexConsumer out, boolean xAxis, float left, float bottom,
                              float right, float top, float depth, int rgb, float lowAlpha, float highAlpha) {
        point(pose, out, xAxis, left, bottom, depth, rgb, lowAlpha);
        point(pose, out, xAxis, right, bottom, depth, rgb, lowAlpha);
        point(pose, out, xAxis, right, top, depth, rgb, highAlpha);
        point(pose, out, xAxis, left, top, depth, rgb, highAlpha);
    }

    private static void point(PoseStack pose, VertexConsumer out, boolean xAxis,
                               float x, float y, float depth, int rgb, float alpha) {
        out.addVertex(pose.last().pose(), xAxis ? x : depth, y, xAxis ? depth : x)
                .setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha);
    }
}
