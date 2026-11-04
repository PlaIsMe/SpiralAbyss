package com.pla.spiral_abyss.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ExperienceChestRenderer extends LootChestRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("spiral_abyss", "textures/entity/exp_chest.png");
    public ExperienceChestRenderer(EntityRendererProvider.Context context) {
        super(context, new ExperienceChestModel(context.bakeLayer(ExperienceChestModel.LAYER_LOCATION)), TEXTURE);
    }
}
