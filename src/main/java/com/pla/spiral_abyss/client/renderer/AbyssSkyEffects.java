package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Renders the supplied 3x2 sky atlas directly; no OptiFine or resource-pack installation is needed. */
public final class AbyssSkyEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = new ResourceLocation(SpiralAbyss.MOD_ID, "spiral_abyss");
    private static final ResourceLocation TEXTURE = new ResourceLocation(SpiralAbyss.MOD_ID, "textures/environment/galaxy_blue.png");
    // Atlas order: bottom, top, south, west, north, east. Each quad faces the cube's interior.
    private static final float[][][] FACES = {
            {{-1,-1,-1}, {-1,-1,1}, {1,-1,1}, {1,-1,-1}},
            {{-1,1,1}, {-1,1,-1}, {1,1,-1}, {1,1,1}},
            {{1,1,1}, {1,-1,1}, {-1,-1,1}, {-1,1,1}},
            {{-1,1,1}, {-1,-1,1}, {-1,-1,-1}, {-1,1,-1}},
            {{-1,1,-1}, {-1,-1,-1}, {1,-1,-1}, {1,1,-1}},
            {{1,1,-1}, {1,-1,-1}, {1,-1,1}, {1,1,1}}
    };
    public AbyssSkyEffects() { super(Float.NaN, false, SkyType.NONE, true, false); }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) { return new Vec3(0.025, 0.045, 0.1); }
    @Override public boolean isFoggyAt(int x, int z) { return false; }
    @Override public float[] getSunriseColor(float time, float partialTick) { return null; }
    @Override public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack pose,
                                       Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog) {
        setupFog.run();
        if (foggy || camera.getFluidInCamera() != FogType.NONE
                || (camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))) return true;
        pose.pushPose();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        FogRenderer.setupNoFog();
        try {
            // The source specifies rotation about Y, one revolution per Minecraft day.
            pose.mulPose(Axis.YP.rotationDegrees(((level.getGameTime() % 24000) + partialTick) * 360F / 24000F));
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f matrix = pose.last().pose();
            for (int face = 0; face < FACES.length; face++) {
                float u0 = (face % 3) / 3F, u1 = u0 + 1F / 3F;
                float v0 = (face / 3) / 2F, v1 = v0 + 0.5F;
                for (int corner = 0; corner < 4; corner++) {
                    float[] xyz = FACES[face][corner];
                    buffer.vertex(matrix, xyz[0] * 100, xyz[1] * 100, xyz[2] * 100)
                            .uv(corner < 2 ? u0 : u1, corner == 0 || corner == 3 ? v0 : v1)
                            .color(255, 255, 255, 255).endVertex();
                }
            }
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            pose.popPose(); RenderSystem.depthMask(true); RenderSystem.setShaderColor(1, 1, 1, 1); setupFog.run();
        }
        return true;
    }
    @Override public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack pose,
                                          double x, double y, double z, Matrix4f projection) { return true; }
    @Override public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture light,
                                               double x, double y, double z) { return true; }
    @Override public boolean tickRain(ClientLevel level, int ticks, Camera camera) { return true; }
}
