package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.entity.RewardChestEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.resources.ResourceLocation;

public class RewardChestModel extends HierarchicalModel<RewardChestEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("spiral_abyss", "reward_chest"), "main");
	private final ModelPart root;
	private final ModelPart base;
	private final ModelPart lid;
	private final ModelPart cube;
	private final ModelPart knob;

	public RewardChestModel(ModelPart root) {
		this.root = root.getChild("root");
		this.base = this.root.getChild("base");
		this.lid = this.root.getChild("lid");
		this.cube = this.lid.getChild("cube");
		this.knob = this.lid.getChild("knob");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(1, 1).addBox(-19.6149F, -12.0178F, 1.0199F, 24.0F, 5.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(78, 1).addBox(-20.6149F, -14.0178F, 0.0199F, 26.0F, 2.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(136, 40).addBox(-20.6149F, -12.0178F, 13.0199F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(149, 40).addBox(2.3851F, -12.0424F, 13.0199F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(162, 40).addBox(2.3851F, -12.0174F, 0.0199F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(175, 40).addBox(-20.6149F, -12.0178F, 0.0199F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(155, 53).addBox(-13.6149F, -12.0178F, 13.6061F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(164, 53).addBox(-3.6149F, -12.0178F, 13.6061F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(173, 53).addBox(-3.6149F, -12.0178F, 0.4061F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(182, 53).addBox(-13.6149F, -12.0178F, 0.4061F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(163, 1).addBox(-20.6149F, -7.0178F, 0.0199F, 26.0F, 2.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(7.6194F, 5.0178F, -8.0F));

		PartDefinition lid = root.addOrReplaceChild("lid", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 8.0F));

		PartDefinition cube = lid.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(163, 21).addBox(6.4851F, -8.2142F, 1.0022F, 1.0F, 3.55F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(86, 21).addBox(-16.6149F, -7.6642F, -0.9978F, 24.0F, 3.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(26, 40).addBox(-16.6149F, -10.4923F, 1.8306F, 24.0F, 2.975F, 8.3375F, new CubeDeformation(0.0F))
		.texOffs(186, 21).addBox(-16.7149F, -8.2142F, 1.0022F, 1.0F, 3.55F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(14, 61).addBox(-0.6149F, -7.6892F, -1.7478F, 2.0F, 3.025F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(206, 53).addBox(-17.6149F, -7.6642F, -1.9978F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(209, 21).addBox(-17.6149F, -11.1997F, 1.5377F, 3.0F, 3.0F, 8.925F, new CubeDeformation(0.0F))
		.texOffs(219, 53).addBox(-17.6149F, -7.6642F, 11.0022F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(232, 53).addBox(5.3772F, -7.6642F, 11.0022F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(1, 40).addBox(5.3872F, -11.1997F, 1.5377F, 3.0F, 3.0F, 8.925F, new CubeDeformation(0.0F))
		.texOffs(1, 61).addBox(5.3872F, -7.6642F, -1.9978F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(23, 61).addBox(-0.6149F, -7.6892F, 11.7522F, 2.0F, 3.025F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(92, 40).addBox(-0.6149F, -11.2244F, 1.7916F, 2.0F, 2.0F, 8.425F, new CubeDeformation(0.0F))
		.texOffs(32, 61).addBox(-10.6149F, -7.6892F, 11.7522F, 2.0F, 3.025F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(114, 40).addBox(-10.6149F, -11.2244F, 1.7916F, 2.0F, 2.0F, 8.425F, new CubeDeformation(0.0F))
		.texOffs(41, 61).addBox(-10.6149F, -7.6892F, -1.7478F, 2.0F, 3.025F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 21).addBox(-17.6149F, -4.6642F, -1.9978F, 26.0F, 2.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(4.6194F, 2.6642F, -13.9822F));

		PartDefinition cube_r1 = cube.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(115, 53).addBox(4.278F, 0.5193F, -4.352F, 2.004F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(135, 53).addBox(14.278F, 0.5193F, -4.352F, 2.004F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.8944F, -8.5142F, 5.2322F, -0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r2 = cube.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(145, 53).addBox(4.278F, 1.6081F, 3.4409F, 2.004F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(125, 53).addBox(14.278F, 1.6081F, 3.4409F, 2.004F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.8944F, -8.5142F, 5.2322F, 0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r3 = cube.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(227, 40).addBox(9.733F, 0.696F, -4.5288F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(188, 40).addBox(-13.272F, 0.696F, -4.5288F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.3444F, -8.4896F, 5.2322F, -0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r4 = cube.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(214, 40).addBox(9.733F, 1.7849F, 2.6177F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(201, 40).addBox(-13.272F, 1.7849F, 2.6177F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.3444F, -8.4896F, 5.2322F, 0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r5 = cube.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(58, 53).addBox(-11.7206F, -4.8889F, 2.0561F, 24.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.8944F, -8.4896F, 5.2629F, -0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r6 = cube.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(1, 53).addBox(-11.7206F, -3.8217F, -4.9889F, 24.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.8944F, -8.4896F, 5.2322F, 0.7854F, 0.0F, 0.0F));

		PartDefinition knob = lid.addOrReplaceChild("knob", CubeListBuilder.create(), PartPose.offset(0.0F, -5.75F, -8.75F));

		PartDefinition knob_r1 = knob.addOrReplaceChild("knob_r1", CubeListBuilder.create().texOffs(191, 53).addBox(-3.0F, -3.0F, -1.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 8.75F, -7.25F, 0.3927F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(RewardChestEntity entity, float limbSwing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        long millis = (long) (Math.max(0, (entity.level().getGameTime() - entity.phaseStart())
                + (age - entity.tickCount)) * 50);
        var animation = switch (entity.phase()) {
            case RewardChestEntity.OPEN -> RewardChestAnimations.open;
            case RewardChestEntity.SHAKE -> RewardChestAnimations.shake;
            case RewardChestEntity.LOOTED -> RewardChestAnimations.looted;
            default -> null;
        };
        if (animation != null) KeyframeAnimations.animate(this, animation, millis, 1, new org.joml.Vector3f());
    }
}
