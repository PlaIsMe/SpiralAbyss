package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.spiral_abyss.entity.LootChestEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class LootChestRenderer extends MobRenderer<LootChestEntity, LootChestModel> {
    private final ResourceLocation texture;
    private LootChestRenderer(EntityRendererProvider.Context context, LootChestModel model, String kind) {
        super(context, model, 0.6f);
        texture = ResourceLocation.fromNamespaceAndPath("spiral_abyss", "textures/entity/" + kind + "_chest.png");
    }
    public static LootChestRenderer copper(EntityRendererProvider.Context context) {
        return new LootChestRenderer(context, new CopperChestModel(context.bakeLayer(CopperChestModel.LAYER_LOCATION)), "copper");
    }
    public static LootChestRenderer iron(EntityRendererProvider.Context context) {
        return new LootChestRenderer(context, new IronChestModel(context.bakeLayer(IronChestModel.LAYER_LOCATION)), "iron");
    }
    public static LootChestRenderer golden(EntityRendererProvider.Context context) {
        return new LootChestRenderer(context, new GoldenChestModel(context.bakeLayer(GoldenChestModel.LAYER_LOCATION)), "golden");
    }
    @Override public ResourceLocation getTextureLocation(LootChestEntity entity) { return texture; }
    @Override public void render(LootChestEntity entity, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        if (entity.phase() != LootChestEntity.DISPENSING) return;
        var vertices = buffers.getBuffer(LootGlow.TYPE);
        int color = entity.rarePayout() ? LootGlow.GOLD : LootGlow.SILVER;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(entity.position());
        float time = entity.tickCount + partialTick;
        float fade = Mth.clamp((entity.level().getGameTime() - entity.phaseStart() + partialTick) / 4f, 0, 1);
        // Match the chest's body orientation, including after it stops tracking the player.
        float rotation = 180 - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
        camera = camera.yRot((float) Math.toRadians(-rotation));
        int seed = entity.getUUID().hashCode() & 1023;
        ChestInteriorGlow.render(pose, vertices, time, seed, color, fade);
        // Seeded moving sources fill the whole bowl rather than tracing its perimeter.
        for (int ray = 0; ray < 16; ray++) {
            double phase = seed + ray * 2.39996;
            double x = Math.sin(phase * 3.1 + time * 0.018) * 0.48;
            double z = Math.sin(phase * 5.7 + time * 0.023) * 0.33;
            float pulse = 0.55f + 0.45f * (float) Math.sin(time * 0.09 + phase);
            Vec3 from = new Vec3(x, 0.57, z);
            Vec3 to = new Vec3(x * 1.7 + Math.sin(phase) * 0.15,
                    1.45 + 0.8 * pulse, z * 1.7 + Math.cos(phase) * 0.15);
            LootGlow.ribbon(pose, vertices, from, to, camera, 0.15f, 0.3f, color, (0.16f + 0.18f * pulse) * fade, 0);
            LootGlow.ribbon(pose, vertices, from, from.lerp(to, 0.65), camera,
                    0.045f, 0.09f, 0xFFFFFF, 0.28f * pulse * fade, 0);
        }
        // Slow rising glints remain bounded and deterministic; no particle flood each frame.
        for (int glint = 0; glint < 8; glint++) {
            float progress = (time * 0.017f + glint / 8f) % 1;
            double angle = glint * 2.4 + time * 0.01;
            Vec3 point = new Vec3(Math.cos(angle) * progress * 0.7, 0.55 + progress * 1.6,
                    Math.sin(angle) * progress * 0.7);
            float alpha = (float) Math.sin(progress * Math.PI) * fade;
            LootGlow.ribbon(pose, vertices, point, point.add(0, 0.09, 0), camera, 0.035f, 0, color, alpha, 0);
        }
        pose.popPose();
    }
}
