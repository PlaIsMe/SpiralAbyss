package com.pla.spiral_abyss.client.renderer;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

// Geometry adapted from the user's Blockbench export in reference/entity.
public final class GoldenChestModel extends LootChestModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("spiral_abyss", "golden_chest"), "main");
    public GoldenChestModel(ModelPart root) {
        super(root, GoldenChestAnimations.spawn, GoldenChestAnimations.idle, GoldenChestAnimations.open);
    }
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition chest = partdefinition.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(76, 79).addBox(-10.0F, -3.0F, -8.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(18, 98).addBox(-9.5F, -7.0F, -7.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(31, 98).addBox(-9.5F, -7.0F, 4.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(57, 98).addBox(6.5F, -7.0F, 4.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(44, 98).addBox(6.5F, -7.0F, -7.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(83, 98).addBox(-6.0F, -2.0F, -7.5F, 12.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 106).addBox(-6.0F, -2.0F, 6.5F, 12.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(93, 79).addBox(6.0F, -3.0F, -8.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(1, 22).addBox(-10.0F, -9.0F, -8.0F, 20.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(1, 98).addBox(6.0F, -3.0F, 4.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(38, 79).addBox(8.5F, -2.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(57, 79).addBox(-9.5F, -2.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(110, 79).addBox(-10.0F, -3.0F, 4.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-9.0F, -7.0F, -7.0F, 18.0F, 6.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition lid = chest.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(66, 60).addBox(-9.5F, -4.9F, -15.5F, 3.0F, 3.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(1, 79).addBox(6.5F, -4.9F, -15.5F, 3.0F, 3.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(1, 41).addBox(-10.0F, -1.9F, -16.0F, 20.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(1, 60).addBox(-9.0F, -4.4F, -15.0F, 18.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(70, 98).addBox(-2.0F, -2.4F, -16.5F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.1F, 8.0F));

		PartDefinition tag_1 = chest.addOrReplaceChild("tag_1", CubeListBuilder.create(), PartPose.offset(0.0F, -22.0F, 0.0F));

		PartDefinition tag_2 = chest.addOrReplaceChild("tag_2", CubeListBuilder.create(), PartPose.offset(0.0F, -18.0F, 0.0F));

		PartDefinition hitbox = chest.addOrReplaceChild("hitbox", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -14.1F, -9.0F, 18.0F, 14.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

}
