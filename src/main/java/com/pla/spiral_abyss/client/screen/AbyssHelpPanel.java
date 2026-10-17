package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Interactive gate diagram. Uses the actual frame states and baked block meshes. */
public final class AbyssHelpPanel {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("spiral_abyss", "textures/gui/journal/help_background.png");
    private static final int WIDTH = 720, HEIGHT = 405;
    private static final Quaternionf ROTATION = new Quaternionf().rotateX((float) Math.toRadians(12)).rotateY((float) Math.toRadians(-18));
    private final List<Part> parts = new ArrayList<>();

    public AbyssHelpPanel() {
        // Derive the diagram from the same geometry that authorizes a real portal.
        AbyssPortalFrame frame = new AbyssPortalFrame(BlockPos.ZERO, Direction.SOUTH);
        for (int y = 0; y <= AbyssPortalFrame.TOP; y++) {
            for (int x = -AbyssPortalFrame.RADIUS; x <= AbyssPortalFrame.RADIUS; x++) {
                BlockState state = frame.expectedFrameState(x, y);
                if (state != null) parts.add(new Part(state, x, y, 0, kind(state)));
            }
        }
        BlockPos controller = frame.anchor();
        parts.add(new Part(BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get().defaultBlockState()
                .setValue(AbyssGateActivationBlock.FACING, Direction.SOUTH),
                controller.getX(), controller.getY(), controller.getZ(), Kind.ACTIVATION));
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (width <= 0 || height <= 0) return;
        Minecraft client = Minecraft.getInstance();
        graphics.blit(BACKGROUND, x, y, width, height, 0, 0, 1280, 720, 1280, 720);
        float scale = Math.min(width / (float) WIDTH, height / (float) HEIGHT);
        float left = x + (width - WIDTH * scale) / 2, top = y + (height - HEIGHT * scale) / 2;
        float mx = (mouseX - left) / scale, my = (mouseY - top) / scale;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left, top, 0);
            graphics.pose().scale(scale, scale, scale);
            Kind hovered = null;
            for (Part part : parts) {
                part.prepare(client);
                if (part.contains(mx, my)) hovered = part.kind;
            }
            for (Kind kind : Kind.values()) {
                if (mx >= kind.labelX - 80 && mx <= kind.labelX + 80 && my >= kind.labelY - 5 && my <= kind.labelY + 19) hovered = kind;
            }
            for (Part part : parts) part.outline(graphics, hovered == part.kind);
            graphics.flush();
            Lighting.setupFor3DItems();
            graphics.pose().pushPose();
            try {
                gateTransform(graphics.pose());
                for (Part part : parts) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(part.x, part.y, part.z);
                    client.getBlockRenderer().renderSingleBlock(part.state, graphics.pose(), graphics.bufferSource(),
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    graphics.pose().popPose();
                }
                graphics.flush();
            } finally {
                graphics.pose().popPose();
                Lighting.setupFor3DItems();
            }
            // Label leaders and details sit in front of the 3D geometry.
            graphics.pose().translate(0, 0, 250);
            for (Kind kind : Kind.values()) label(graphics, kind, hovered == kind);
            details(graphics, hovered);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void gateTransform(PoseStack pose) {
        pose.translate(360, 325, 100);
        // GUI depth increases toward the viewer: +Z exposes the SOUTH-facing decorated
        // activation face. Invert only Y to keep the assembled frame upright.
        pose.scale(34, -34, 34);
        pose.mulPose(ROTATION);
        pose.translate(-0.5, 0, -0.5);
    }

    private static Kind kind(BlockState state) {
        if (state.is(BlockInit.ABYSS_GATE_HORIZONTAL.get())) return Kind.HORIZONTAL;
        if (state.is(BlockInit.ABYSS_GATE_CURVE.get())) return Kind.CURVE;
        return Kind.VERTICAL;
    }

    private static void label(GuiGraphics graphics, Kind kind, boolean hovered) {
        var font = Minecraft.getInstance().font;
        int color = hovered ? kind.color : 0xFFECE5FA;
        graphics.pose().pushPose();
        graphics.pose().translate(kind.labelX, kind.labelY, 0);
        graphics.pose().scale(1.35F, 1.35F, 1);
        graphics.drawCenteredString(font, Component.translatable("ui.spiral_abyss.help." + kind.key), 0, 0, color);
        graphics.pose().popPose();
        int elbow = kind.labelY + 34;
        dashed(graphics, kind.labelX, kind.labelY + 19, kind.labelX, elbow, color);
        dashed(graphics, kind.labelX, elbow, kind.targetX, elbow, color);
        dashed(graphics, kind.targetX, elbow, kind.targetX, kind.targetY, color);
        graphics.fill(kind.labelX - 2, kind.labelY + 17, kind.labelX + 3, kind.labelY + 22, color);
        graphics.fill(kind.targetX - 2, kind.targetY - 2, kind.targetX + 3, kind.targetY + 3, kind.color);
    }

    private static void dashed(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int length = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (length == 0) return;
        for (int i = 0; i < length; i += 7) {
            int end = Math.min(i + 4, length);
            int ax = x1 + (x2 - x1) * i / length, ay = y1 + (y2 - y1) * i / length;
            int bx = x1 + (x2 - x1) * end / length, by = y1 + (y2 - y1) * end / length;
            graphics.fill(Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx) + 2, Math.max(ay, by) + 2, color);
        }
    }

    private static void details(GuiGraphics graphics, Kind hovered) {
        var font = Minecraft.getInstance().font;
        graphics.fill(24, 342, 696, 397, 0xBA171027);
        graphics.fill(24, 342, 27, 397, hovered == null ? 0xFFBFA5D5 : hovered.color);
        String key = hovered == null ? "overview" : hovered.key;
        Component title = Component.translatable("ui.spiral_abyss.help." + key);
        Component description = Component.translatable("ui.spiral_abyss.help." + key + ".description");
        float titleScale = Math.min(1.25F, 644F / Math.max(1, font.width(title)));
        graphics.pose().pushPose();
        graphics.pose().translate(38, 350, 0);
        graphics.pose().scale(titleScale, titleScale, 1);
        graphics.drawString(font, title, 0, 0, 0xFFF3E4C5, false);
        graphics.pose().popPose();
        // Find the largest size that fits every translated line into the card.
        // Wrapping is recalculated at each size; no suffix or reset instruction is discarded.
        float low = 0, high = 1.25F;
        int spacing = font.lineHeight + 2;
        for (int i = 0; i < 16; i++) {
            float candidate = (low + high) / 2;
            int lines = font.split(description, Math.max(1, (int) (644 / candidate))).size();
            if (lines * spacing * candidate <= 27) low = candidate;
            else high = candidate;
        }
        float bodyScale = Math.max(0.00001F, low);
        graphics.pose().pushPose();
        graphics.pose().translate(38, 366, 0);
        graphics.pose().scale(bodyScale, bodyScale, 1);
        int lineY = 0;
        for (var line : font.split(description, Math.max(1, (int) (644 / bodyScale)))) {
            graphics.drawString(font, line, 0, lineY, 0xFFE1D8ED, false);
            lineY += spacing;
        }
        graphics.pose().popPose();
    }

    private enum Kind {
        HORIZONTAL("horizontal", 0xFF91D969, 168, 72, 315, 144),
        ACTIVATION("activation", 0xFF8DE4E0, 528, 45, 367, 117),
        VERTICAL("vertical", 0xFF9B8CFF, 559, 191, 438, 234),
        CURVE("curve", 0xFFF08196, 155, 237, 286, 286);
        final String key;
        final int color, labelX, labelY, targetX, targetY;
        Kind(String key, int color, int labelX, int labelY, int targetX, int targetY) {
            this.key = key; this.color = color; this.labelX = labelX; this.labelY = labelY;
            this.targetX = targetX; this.targetY = targetY;
        }
    }

    private static final class Part {
        final BlockState state;
        final int x, y, z;
        final Kind kind;
        BakedModel cachedModel;
        int minX, minY, width, height;
        boolean[] mask;
        final List<int[]> outline = new ArrayList<>();

        Part(BlockState state, int x, int y, int z, Kind kind) {
            this.state = state; this.x = x; this.y = y; this.z = z; this.kind = kind;
        }

        void prepare(Minecraft client) {
            BakedModel model = client.getBlockRenderer().getBlockModel(state);
            if (cachedModel == model) return;
            cachedModel = model;
            outline.clear();
            PoseStack pose = new PoseStack();
            gateTransform(pose);
            pose.translate(x, y, z);
            Matrix4f matrix = pose.last().pose();
            List<float[]> polygons = new ArrayList<>();
            float lowX = WIDTH, lowY = HEIGHT, highX = 0, highY = 0;
            for (int side = 0; side < 7; side++) {
                Direction direction = side == 6 ? null : Direction.values()[side];
                for (BakedQuad quad : model.getQuads(state, direction, RandomSource.create(42))) {
                    int[] data = quad.getVertices();
                    int stride = data.length / 4;
                    float[] polygon = new float[8];
                    for (int v = 0; v < 4; v++) {
                        Vector3f point = new Vector3f(Float.intBitsToFloat(data[v * stride]),
                                Float.intBitsToFloat(data[v * stride + 1]), Float.intBitsToFloat(data[v * stride + 2]));
                        matrix.transformPosition(point);
                        polygon[v * 2] = point.x; polygon[v * 2 + 1] = point.y;
                        lowX = Math.min(lowX, point.x); lowY = Math.min(lowY, point.y);
                        highX = Math.max(highX, point.x); highY = Math.max(highY, point.y);
                    }
                    polygons.add(polygon);
                }
            }
            if (polygons.isEmpty()) { mask = new boolean[0]; width = height = 0; return; }
            minX = (int) Math.floor(lowX) - 3; minY = (int) Math.floor(lowY) - 3;
            width = (int) Math.ceil(highX) - minX + 4; height = (int) Math.ceil(highY) - minY + 4;
            mask = new boolean[width * height];
            for (int py = 0; py < height; py++) for (int px = 0; px < width; px++) {
                for (float[] polygon : polygons) if (inside(polygon, minX + px + 0.5F, minY + py + 0.5F)) {
                    mask[py * width + px] = true; break;
                }
            }
            // The outline follows the projected mesh, including stair cutouts and the activation diamond.
            for (int py = 0; py < height; py++) {
                int start = -1;
                for (int px = 0; px <= width; px++) {
                    boolean edge = px < width && !pixel(px, py) && nearby(px, py);
                    if (edge && start < 0) start = px;
                    if (!edge && start >= 0) {
                        outline.add(new int[] { minX + start, minY + py, minX + px }); start = -1;
                    }
                }
            }
        }

        boolean contains(float mx, float my) { return pixel((int) mx - minX, (int) my - minY); }
        boolean pixel(int px, int py) { return px >= 0 && py >= 0 && px < width && py < height && mask[py * width + px]; }
        boolean nearby(int px, int py) {
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) if (pixel(px + dx, py + dy)) return true;
            return false;
        }
        void outline(GuiGraphics graphics, boolean selected) {
            int color = selected ? kind.color : (kind.color & 0xFFFFFF) | 0xA0000000;
            for (int[] run : outline) graphics.fill(run[0], run[1], run[2], run[1] + 1, color);
        }
        static boolean inside(float[] polygon, float px, float py) {
            boolean result = false;
            for (int i = 0, j = 3; i < 4; j = i++) {
                float ix = polygon[i * 2], iy = polygon[i * 2 + 1];
                float jx = polygon[j * 2], jy = polygon[j * 2 + 1];
                if ((iy > py) != (jy > py) && px < (jx - ix) * (py - iy) / (jy - iy) + ix) result = !result;
            }
            return result;
        }
    }
}
