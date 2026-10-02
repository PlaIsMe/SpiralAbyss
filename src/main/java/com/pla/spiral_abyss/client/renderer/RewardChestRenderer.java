package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.entity.RewardChestEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class RewardChestRenderer extends MobRenderer<RewardChestEntity, RewardChestModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("spiral_abyss", "textures/entity/reward_chest.png");
    public RewardChestRenderer(EntityRendererProvider.Context context) {
        super(context, new RewardChestModel(context.bakeLayer(RewardChestModel.LAYER_LOCATION)), 0.6f);
    }
    @Override public ResourceLocation getTextureLocation(RewardChestEntity entity) { return TEXTURE; }
}
