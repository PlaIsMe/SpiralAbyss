package com.pla.spiral_abyss.neoforge.client.renderer;

import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.client.renderer.AbyssPortalRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** NeoForge culls the entire portal using the bounds supplied by its renderer extension. */
public final class NeoForgeAbyssPortalRenderer extends AbyssPortalRenderer {
    public NeoForgeAbyssPortalRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    // Compile against NeoForge so javac emits the BlockEntity bridge for its generic hook.
    // A same-named method in vanilla-compiled common code does not override that hook.
    @Override public AABB getRenderBoundingBox(AbyssPortalBlockEntity entity) {
        return entity.getRenderBoundingBox();
    }
}
