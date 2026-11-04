package com.pla.spiral_abyss.client.screen;

import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.BossOptions.PreviewAnimation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import dev.architectury.platform.Platform;

/** Keeps optional animation-library linkage outside the ordinary client rendering path. */
final class OptionalGeoPreview {
    private static boolean unavailable;
    private OptionalGeoPreview() {}

    static boolean supports(LivingEntity entity) {
        return supports(entity, PreviewAnimation.DEFAULT);
    }

    static boolean supports(LivingEntity entity, PreviewAnimation animation) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (unavailable || id == null || (!animation.configured() && !id.getNamespace().equals("block_factorys_bosses"))
                || !Platform.isModLoaded("geckolib")) return false;
        try { return GeckoPreviewSampler.supports(entity); }
        catch (LinkageError exception) {
            unavailable = true;
            LogUtils.getLogger().warn("Installed GeckoLib API does not support Abyss preview sampling", exception);
            return false;
        }
    }

    static boolean render(LivingEntity entity, long seed, PoseStack pose,
                          MultiBufferSource buffers, int light) {
        return render(entity, seed, PreviewAnimation.DEFAULT, pose, buffers, light);
    }

    static boolean render(LivingEntity entity, long seed, PreviewAnimation animation, PoseStack pose,
                          MultiBufferSource buffers, int light) {
        return supports(entity, animation) && GeckoPreviewSampler.render(entity, seed, animation, pose, buffers, light);
    }
}
