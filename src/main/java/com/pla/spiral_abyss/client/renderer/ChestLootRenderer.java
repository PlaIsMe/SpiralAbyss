package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.spiral_abyss.entity.ChestLootEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

/** Chest-only light trail, using the reference's camera-facing ribbon/bright-core approach. */
public final class ChestLootRenderer extends ItemEntityRenderer {
    public ChestLootRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(ItemEntity item, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        super.render(item, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
        if (!(item instanceof ChestLootEntity loot)) return;
        Vec3 origin = item.getPosition(partialTick);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(origin);
        var vertices = buffers.getBuffer(LootGlow.TYPE);
        int color = loot.rare() ? LootGlow.GOLD : LootGlow.CYAN;
        if (!loot.onGround()) {
            var points = new ArrayList<>(loot.trail());
            points.add(origin.add(0, 0.15, 0));
            for (int index = 1; index < points.size(); index++) {
                Vec3 from = points.get(index - 1).subtract(origin), to = points.get(index).subtract(origin);
                float a = (index - 1f) / points.size(), b = index / (float) points.size();
                LootGlow.ribbon(pose, vertices, from, to, camera, 0.16f * a, 0.16f * b, color, 0.6f * a, 0.6f * b);
                LootGlow.ribbon(pose, vertices, from, to, camera, 0.035f * a, 0.035f * b, 0xFFFFFF, 0.9f * a, 0.9f * b);
            }
            return;
        }
        // Grounded loot gets a beam; airborne loot renders only its motion trail.
        Vec3 base = new Vec3(0, 0.16, 0), top = base.add(0, 1.75, 0);
        LootGlow.ribbon(pose, vertices, base, top, camera, 0.18f, 0.1f, color, 0.38f, 0);
        LootGlow.ribbon(pose, vertices, base, top, camera, 0.065f, 0.035f, color, 0.85f, 0);
        LootGlow.ribbon(pose, vertices, base, top, camera, 0.018f, 0.01f, 0xFFFFFF, 0.85f, 0);
    }
}
