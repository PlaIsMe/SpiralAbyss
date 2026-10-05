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
        // A fan of rays originates inside the bowl and widens above the open lid.
        for (int ray = 0; ray < 9; ray++) {
            double angle = ray * Math.PI * 2 / 9 + time * 0.008;
            double spread = 0.65 + 0.15 * Math.sin(time * 0.06 + ray);
            Vec3 from = new Vec3(Math.cos(angle) * 0.25, 0.48, Math.sin(angle) * 0.2);
            Vec3 to = new Vec3(Math.cos(angle) * spread, 1.9 + 0.3 * Math.sin(time * 0.04 + ray), Math.sin(angle) * spread);
            LootGlow.ribbon(pose, vertices, from, to, camera, 0.12f, 0.36f, color, 0.3f * fade, 0);
            LootGlow.ribbon(pose, vertices, from, to.scale(0.8).add(0, 0.1, 0), camera,
                    0.04f, 0.1f, 0xFFFFFF, 0.3f * fade, 0);
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
    }
}
