package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** A stationary tabletop recipe, sharing one projection for the table and its ingredients. */
final class ActivationRecipePreview {
    private ActivationRecipePreview() {}

    static void render(GuiGraphics graphics) {
        var client = Minecraft.getInstance();
        graphics.fill(470, 88, 696, 330, 0xEE19243C);
        graphics.renderOutline(470, 88, 226, 242, 0xFF8DE4E0);
        graphics.drawCenteredString(client.font, Component.translatable("ui.spiral_abyss.help.recipe"), 583, 102, 0xFFF3E4C5);
        graphics.flush();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderLights(new Vector3f(-.2F, -1, 1).normalize(), new Vector3f(.2F, -1, 1).normalize());
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(559, 252, 60);
            pose.scale(86, -86, 86);
            pose.mulPose(Axis.XP.rotationDegrees(40));
            pose.mulPose(Axis.YP.rotationDegrees(-35));
            block(graphics, Blocks.CRAFTING_TABLE.defaultBlockState(), -.5, 0, -.5, 1);
            block(graphics, Blocks.LODESTONE.defaultBlockState(), -.11, 1.01, -.11, .22F);
            diamond(graphics, -.31F, 0);
            diamond(graphics, .31F, 0);
            diamond(graphics, 0, -.31F);
            diamond(graphics, 0, .31F);
            graphics.flush();
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
        }
        // Output uses the same actual block mesh as the portal diagram.
        graphics.drawString(client.font, "→", 624, 216, 0xFF8DE4E0, false);
        pose.pushPose();
        try {
            pose.translate(658, 240, 75);
            pose.scale(35, -35, 35);
            pose.mulPose(Axis.XP.rotationDegrees(15));
            pose.mulPose(Axis.YP.rotationDegrees(155));
            block(graphics, BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get().defaultBlockState(), -.5, -.5, -.5, 1);
            graphics.flush();
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
        }
        graphics.drawCenteredString(client.font, Component.translatable("ui.spiral_abyss.help.recipe.ingredients"), 583, 291, 0xFFE1D8ED);
        graphics.drawCenteredString(client.font, Component.translatable("ui.spiral_abyss.help.recipe.result"), 583, 307, 0xFF8DE4E0);
    }

    private static void block(GuiGraphics graphics, BlockState state, double x, double y, double z, float size) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(size, size, size);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void diamond(GuiGraphics graphics, float x, float z) {
        var client = Minecraft.getInstance();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, 1.035, z);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(.30F, .30F, .30F);
        client.getItemRenderer().renderStatic(new ItemStack(Items.DIAMOND), ItemDisplayContext.NONE,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, graphics.bufferSource(), client.level, 0);
        pose.popPose();
    }
}
