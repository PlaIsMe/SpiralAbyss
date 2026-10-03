package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.entity.ScrollEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.resources.ResourceLocation;

// Made with Blockbench 5.2.1
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public final class ScrollEntityModel extends HierarchicalModel<ScrollEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("spiral_abyss", "scroll_entity"), "main");
	private final ModelPart root;
	private final ModelPart scroll_center;
	private final ModelPart scroll_roll_a;
	private final ModelPart scroll_roll_b;

	public ScrollEntityModel(ModelPart root) {
		this.root = root.getChild("root");
		this.scroll_center = this.root.getChild("scroll_center");
		this.scroll_roll_a = this.root.getChild("scroll_roll_a");
		this.scroll_roll_b = this.root.getChild("scroll_roll_b");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 17.0F, 0.0F, -1.5674F, -0.3115F, -1.576F));

		PartDefinition scroll_center = root.addOrReplaceChild("scroll_center", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition scroll_part_01_r1 = scroll_center.addOrReplaceChild("scroll_part_01_r1", CubeListBuilder.create().texOffs(1, 1).addBox(-4.725F, -2.063F, -4.0F, 9.45F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2182F));

		PartDefinition scroll_roll_a = root.addOrReplaceChild("scroll_roll_a", CubeListBuilder.create().texOffs(37, 1).addBox(-0.7081F, 0.2299F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(1, 11).addBox(-0.7078F, -2.184F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(58, 11).addBox(-1.4139F, -0.4769F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(5.7542F, -1.2228F, 0.0F));

		PartDefinition scroll_part_08_r1 = scroll_roll_a.addOrReplaceChild("scroll_part_08_r1", CubeListBuilder.create().texOffs(39, 11).addBox(0.0F, -0.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4139F, -0.4149F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition scroll_part_07_r1 = scroll_roll_a.addOrReplaceChild("scroll_part_07_r1", CubeListBuilder.create().texOffs(20, 11).addBox(0.0F, -2.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition scroll_part_05_r1 = scroll_roll_a.addOrReplaceChild("scroll_part_05_r1", CubeListBuilder.create().texOffs(94, 1).addBox(0.0F, -0.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2919F, 0.2929F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition scroll_part_04_r1 = scroll_roll_a.addOrReplaceChild("scroll_part_04_r1", CubeListBuilder.create().texOffs(75, 1).addBox(0.0F, -0.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.9993F, -0.4149F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition scroll_part_03_r1 = scroll_roll_a.addOrReplaceChild("scroll_part_03_r1", CubeListBuilder.create().texOffs(56, 1).addBox(0.0F, -0.2346F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1702F, -2.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition scroll_roll_b = root.addOrReplaceChild("scroll_roll_b", CubeListBuilder.create(), PartPose.offset(-5.9539F, -2.9299F, 0.0F));

		PartDefinition scroll_part_17_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_17_r1", CubeListBuilder.create().texOffs(96, 21).addBox(0.0F, -2.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0659F, 0.2465F, 0.0F, 0.0F, 0.0F, 2.3562F));

		PartDefinition scroll_part_16_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_16_r1", CubeListBuilder.create().texOffs(77, 21).addBox(0.0F, -2.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.774F, -2.0464F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition scroll_part_15_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_15_r1", CubeListBuilder.create().texOffs(58, 21).addBox(0.0F, -2.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4798F, -1.3395F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition scroll_part_14_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_14_r1", CubeListBuilder.create().texOffs(39, 21).addBox(0.0F, -4.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7737F, -1.6324F, 0.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition scroll_part_13_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_13_r1", CubeListBuilder.create().texOffs(20, 21).addBox(0.0F, -0.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0666F, 0.6615F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition scroll_part_12_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_12_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -0.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4798F, 0.6615F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition scroll_part_11_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_11_r1", CubeListBuilder.create().texOffs(96, 11).addBox(0.0F, -0.2346F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8957F, 2.2465F, 0.0F, 0.0F, 0.0F, -2.3562F));

		PartDefinition scroll_part_10_r1 = scroll_roll_b.addOrReplaceChild("scroll_part_10_r1", CubeListBuilder.create().texOffs(77, 11).addBox(0.0F, -0.063F, -4.0F, 1.0F, 0.125F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.774F, -0.0464F, 0.0F, 0.0F, 0.0F, 2.3562F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(ScrollEntity entity, float limbSwing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        KeyframeAnimations.animate(this, ScrollEntityAnimations.IDLE, (long) (age * 50), 1, new org.joml.Vector3f());
    }
}
