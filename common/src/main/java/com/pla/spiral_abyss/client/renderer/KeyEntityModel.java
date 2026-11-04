package com.pla.spiral_abyss.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

// Made with Blockbench 5.2.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class KeyEntityModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	private final ModelPart key_head_perfect_symmetric_3d;
	private final ModelPart stick;

	public KeyEntityModel(ModelPart root) {
		this.key_head_perfect_symmetric_3d = root.getChild("key_head_perfect_symmetric_3d");
		this.stick = root.getChild("stick");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition key_head_perfect_symmetric_3d = partdefinition.addOrReplaceChild("key_head_perfect_symmetric_3d", CubeListBuilder.create().texOffs(2, 84).addBox(13.0F, -50.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(10, 84).addBox(-14.0F, -50.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 84).addBox(13.0F, -49.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(28, 84).addBox(-15.0F, -49.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(38, 84).addBox(13.0F, -48.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(50, 84).addBox(-16.0F, -48.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(62, 84).addBox(13.0F, -46.0F, -1.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(78, 84).addBox(-18.0F, -46.0F, -1.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(94, 84).addBox(16.0F, -45.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(104, 84).addBox(13.0F, -45.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(112, 84).addBox(-14.0F, -45.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(120, 84).addBox(-18.0F, -45.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(130, 84).addBox(12.0F, -44.0F, -1.0F, 7.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(148, 84).addBox(-18.0F, -44.0F, -1.0F, 7.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(166, 84).addBox(10.0F, -42.0F, -1.0F, 9.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(188, 84).addBox(-18.0F, -42.0F, -1.0F, 9.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(210, 84).addBox(8.0F, -40.0F, -1.0F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 89).addBox(-18.0F, -40.0F, -1.0F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(28, 89).addBox(7.0F, -39.0F, -1.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(56, 89).addBox(-18.0F, -39.0F, -1.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(84, 89).addBox(6.0F, -38.0F, -1.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(112, 89).addBox(-17.0F, -38.0F, -1.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(140, 89).addBox(5.0F, -37.0F, -1.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(168, 89).addBox(-16.0F, -37.0F, -1.0F, 12.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 89).addBox(3.0F, -36.0F, -1.0F, 13.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(226, 89).addBox(-15.0F, -36.0F, -1.0F, 13.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 93).addBox(-14.0F, -35.0F, -1.0F, 29.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(64, 93).addBox(-13.0F, -34.0F, -1.0F, 27.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(122, 93).addBox(-12.0F, -33.0F, -1.0F, 25.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(176, 93).addBox(-11.0F, -32.0F, -1.0F, 23.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 97).addBox(-9.0F, -31.0F, -1.0F, 19.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(44, 97).addBox(-5.0F, -30.0F, -1.0F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(70, 97).addBox(-3.0F, -29.0F, -1.0F, 7.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(52, 64).addBox(-14.0F, -11.0F, -1.0F, 13.0F, 15.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition stick = partdefinition.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 41).addBox(-1.0F, -23.0F, -2.0F, 3.0F, 35.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(2, 34).addBox(-1.0F, -28.0F, -1.0F, 3.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		key_head_perfect_symmetric_3d.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		stick.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}
}
