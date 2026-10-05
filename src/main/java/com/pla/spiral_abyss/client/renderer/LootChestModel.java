package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.entity.LootChestEntity;
import net.minecraft.client.animation.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Samples exported clips; dispensing and looted chests hold the exact final open frame. */
public abstract class LootChestModel extends HierarchicalModel<LootChestEntity> {
    private final ModelPart root;
    private final AnimationDefinition spawn, idle, open;
    private final Vector3f scratch = new Vector3f();
    protected LootChestModel(ModelPart root, AnimationDefinition spawn, AnimationDefinition idle, AnimationDefinition open) {
        this.root = root; this.spawn = spawn; this.idle = idle; this.open = open;
        root.getChild("chest").getChild("hitbox").visible = false;
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(LootChestEntity entity, float limbSwing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        long millis = (long) (Math.max(0, entity.level().getGameTime() - entity.phaseStart() + age - entity.tickCount) * 50);
        AnimationDefinition clip = switch (entity.phase()) {
            case LootChestEntity.SPAWN -> spawn;
            case LootChestEntity.IDLE -> idle;
            default -> open;
        };
        if (entity.phase() == LootChestEntity.DISPENSING || entity.phase() == LootChestEntity.LOOTED)
            millis = Math.round(open.lengthInSeconds() * 1000);
        else if (!clip.looping()) millis = Math.min(millis, Math.round(clip.lengthInSeconds() * 1000));
        KeyframeAnimations.animate(this, clip, millis, 1, scratch);
    }
}
