package com.pla.spiral_abyss.boss_enviroment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Built-in wither environment registration; datapacks may override these defaults. */
public final class WitherEnv implements BossEnvironmentRegistry.Definition {
    @Override public ResourceLocation entityId() { return ResourceLocation.fromNamespaceAndPath("minecraft", "wither"); }
    @Override public BossEnvironmentCatalog.Profile profile() {
        return new BossEnvironmentCatalog.Profile(entityId(), List.of(Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SOIL.defaultBlockState()), 32, 3, 16, 100, Blocks.SOUL_FIRE.defaultBlockState(), 0.08, null);
    }
}
