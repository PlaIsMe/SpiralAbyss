package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class KeyEntityRenderer extends MobRenderer<KeyEntity, KeyEntityModel<KeyEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "textures/entity/key_entity.png");
    private static final ResourceLocation CUBE_TEXTURE = ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "textures/block/spinning_block.png");
    // The old FIXED block-item render was half a block wide. Keep that size here
    // without consulting item display transforms, baked item models or item renderers.
    private static final float HALF_CUBE = 0.25F;
    private static final float[][][] CUBE_FACES = {
            {{-HALF_CUBE, -HALF_CUBE, HALF_CUBE}, {HALF_CUBE, -HALF_CUBE, HALF_CUBE}, {HALF_CUBE, HALF_CUBE, HALF_CUBE}, {-HALF_CUBE, HALF_CUBE, HALF_CUBE}},
            {{HALF_CUBE, -HALF_CUBE, -HALF_CUBE}, {-HALF_CUBE, -HALF_CUBE, -HALF_CUBE}, {-HALF_CUBE, HALF_CUBE, -HALF_CUBE}, {HALF_CUBE, HALF_CUBE, -HALF_CUBE}},
            {{-HALF_CUBE, -HALF_CUBE, -HALF_CUBE}, {-HALF_CUBE, -HALF_CUBE, HALF_CUBE}, {-HALF_CUBE, HALF_CUBE, HALF_CUBE}, {-HALF_CUBE, HALF_CUBE, -HALF_CUBE}},
            {{HALF_CUBE, -HALF_CUBE, HALF_CUBE}, {HALF_CUBE, -HALF_CUBE, -HALF_CUBE}, {HALF_CUBE, HALF_CUBE, -HALF_CUBE}, {HALF_CUBE, HALF_CUBE, HALF_CUBE}},
            {{-HALF_CUBE, HALF_CUBE, HALF_CUBE}, {HALF_CUBE, HALF_CUBE, HALF_CUBE}, {HALF_CUBE, HALF_CUBE, -HALF_CUBE}, {-HALF_CUBE, HALF_CUBE, -HALF_CUBE}},
            {{-HALF_CUBE, -HALF_CUBE, -HALF_CUBE}, {HALF_CUBE, -HALF_CUBE, -HALF_CUBE}, {HALF_CUBE, -HALF_CUBE, HALF_CUBE}, {-HALF_CUBE, -HALF_CUBE, HALF_CUBE}}
    };
    private static final float[][] FACE_NORMALS = {
            {0, 0, 1}, {0, 0, -1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}
    };
    private static final float[] FACE_U = {0, 1, 1, 0};
    private static final float[] FACE_V = {1, 1, 0, 0};

    public KeyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new KeyEntityModel<>(context.bakeLayer(ModModelLayers.KEY_ENTITY_LAYER)), 0);
    }

    @Override public ResourceLocation getTextureLocation(KeyEntity entity) { return TEXTURE; }
    @Override protected int getBlockLightLevel(KeyEntity entity, BlockPos pos) { return 15; }

    @Override
    public void render(KeyEntity entity, float yaw, float partialTicks, PoseStack poses,
                       MultiBufferSource buffer, int light) {
        super.render(entity, yaw, partialTicks, poses, buffer, light);
        double time = entity.level().getGameTime() + partialTicks;
        poses.pushPose();
        poses.translate(0, 3.5 + Math.sin(time / 10) / 20, 0);
        poses.mulPose(Axis.ZP.rotationDegrees(45));
        poses.mulPose(Axis.XP.rotationDegrees((float) (time * 4 % 360)));
        renderCube(poses, buffer);
        poses.popPose();
    }

    private static void renderCube(PoseStack poses, MultiBufferSource buffer) {
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(CUBE_TEXTURE));
        PoseStack.Pose pose = poses.last();
        for (int face = 0; face < CUBE_FACES.length; face++) {
            float[] normal = FACE_NORMALS[face];
            for (int corner = 0; corner < 4; corner++) {
                float[] point = CUBE_FACES[face][corner];
                vertices.vertex(pose.pose(), point[0], point[1], point[2])
                        .color(255, 255, 255, 255).uv(FACE_U[corner], FACE_V[corner])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.normal(), normal[0], normal[1], normal[2]).endVertex();
            }
        }
    }
}
