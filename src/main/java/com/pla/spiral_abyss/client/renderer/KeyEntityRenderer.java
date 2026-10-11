package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.init.BlockInit;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KeyEntityRenderer extends MobRenderer<KeyEntity, KeyEntityModel<KeyEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "textures/entity/key_entity.png");
    private final EntityRendererProvider.Context context;

    public KeyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new KeyEntityModel<>(context.bakeLayer(ModModelLayers.KEY_ENTITY_LAYER)), 0);
        this.context = context;
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
        context.getItemRenderer().renderStatic(new ItemStack(BlockInit.SPINNING_BLOCK.get()),
                ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poses, buffer, entity.level(), 0);
        poses.popPose();
    }
}
