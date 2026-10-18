package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Keeps optional animation-library linkage outside the ordinary client rendering path. */
final class OptionalGeoPreview {
    private static boolean unavailable;
    private OptionalGeoPreview() {}

    static boolean supports(LivingEntity entity) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (unavailable || id == null || !id.getNamespace().equals("block_factorys_bosses")
                || !ModList.get().isLoaded("geckolib")) return false;
        try { return GeckoPreviewSampler.supports(entity); }
        catch (LinkageError exception) {
            unavailable = true;
            LogUtils.getLogger().warn("Installed GeckoLib API does not support Abyss preview sampling", exception);
            return false;
        }
    }

    static boolean render(LivingEntity entity, long seed, PoseStack pose,
                          MultiBufferSource buffers, int light) {
        return supports(entity) && GeckoPreviewSampler.render(entity, seed, pose, buffers, light);
    }
}
