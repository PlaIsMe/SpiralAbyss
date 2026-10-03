package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.entity.ScrollEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class ScrollEntityRenderer extends MobRenderer<ScrollEntity, ScrollEntityModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("spiral_abyss", "textures/entity/scroll_entity.png");
    public ScrollEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ScrollEntityModel(context.bakeLayer(ScrollEntityModel.LAYER_LOCATION)), 0.3f);
    }
    @Override public ResourceLocation getTextureLocation(ScrollEntity entity) { return TEXTURE; }
}
