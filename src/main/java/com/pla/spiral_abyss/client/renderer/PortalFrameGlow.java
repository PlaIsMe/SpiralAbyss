package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Cyan-only overlays on the actual baked frame. The activation emblem keeps its original appearance. */
final class PortalFrameGlow {
    private static final Map<BakedModel, Map<BlockState, List<Face>>> CACHE = new WeakHashMap<>();
    private record Skin(ResourceLocation core, ResourceLocation halo) {}
    private record Face(float[] vertices, Vector3f normal, Skin skin) {}
    private static final Map<ResourceLocation, Skin> SKINS = Map.of(id("block/abyss_gate_1"), skin("abyss_gate_1"));

    private PortalFrameGlow() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("spiral_abyss", path); }
    private static Skin skin(String name) {
        return new Skin(id("textures/block/glow/" + name + "_emissive.png"),
                id("textures/block/glow/" + name + "_halo.png"));
    }

    /** Pose is at the gate base, not its elevated controller. Never runs for an incomplete/inactive frame. */
    static void render(AbyssPortalBlockEntity portal, PoseStack pose, MultiBufferSource buffers) {
        var level = portal.getLevel();
        var frame = portal.frame();
        if (level == null || !frame.active(level)) return;
        for (int y = 0; y <= 5; y++) for (int across = -2; across <= 2; across++) {
            if (frame.expectedFrameState(across, y) == null) continue;
            BlockPos pos = frame.at(across, y);
            BlockState state = level.getBlockState(pos);
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            List<Face> faces = CACHE.computeIfAbsent(model, ignored -> new HashMap<>())
                    .computeIfAbsent(state, ignored -> bake(model, state));
            pose.pushPose();
            BlockPos offset = pos.subtract(frame.base());
            pose.translate(offset.getX(), offset.getY(), offset.getZ());
            for (Face face : faces) {
                draw(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(face.skin.core, false)), face, .002F);
                draw(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(face.skin.halo, false)), face, .003F);
            }
            pose.popPose();
        }
    }

    private static List<Face> bake(BakedModel model, BlockState state) {
        List<Face> result = new ArrayList<>();
        append(result, model.getQuads(state, null, RandomSource.create(0)));
        for (Direction side : Direction.values()) append(result, model.getQuads(state, side, RandomSource.create(0)));
        return List.copyOf(result);
    }

    private static void append(List<Face> result, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            var sprite = quad.getSprite();
            Skin skin = SKINS.get(sprite.contents().name());
            if (skin == null) continue;
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            float[] vertices = new float[20];
            for (int v = 0; v < 4; v++) {
                int source = v * stride, target = v * 5;
                vertices[target] = Float.intBitsToFloat(data[source]);
                vertices[target + 1] = Float.intBitsToFloat(data[source + 1]);
                vertices[target + 2] = Float.intBitsToFloat(data[source + 2]);
                vertices[target + 3] = (Float.intBitsToFloat(data[source + 4]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
                vertices[target + 4] = (Float.intBitsToFloat(data[source + 5]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
            }
            Vector3f edgeA = new Vector3f(vertices[5] - vertices[0], vertices[6] - vertices[1], vertices[7] - vertices[2]);
            Vector3f edgeB = new Vector3f(vertices[10] - vertices[0], vertices[11] - vertices[1], vertices[12] - vertices[2]);
            Vector3f normal = edgeA.cross(edgeB);
            if (normal.lengthSquared() < 1.0E-8F) continue;
            result.add(new Face(vertices, normal.normalize(), skin));
        }
    }

    private static void draw(PoseStack pose, VertexConsumer out, Face face, float offset) {
        for (int v = 0; v < 4; v++) {
            int i = v * 5;
            out.vertex(pose.last().pose(), face.vertices[i] + face.normal.x * offset,
                            face.vertices[i + 1] + face.normal.y * offset, face.vertices[i + 2] + face.normal.z * offset)
                    .color(255, 255, 255, 255).uv(face.vertices[i + 3], face.vertices[i + 4])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.last().normal(), face.normal.x, face.normal.y, face.normal.z).endVertex();
        }
    }
}
