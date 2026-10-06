package com.pla.spiral_abyss.boss_enviroment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Built-in ender_dragon environment registration; datapacks may override these defaults. */
public final class EnderDragonEnv implements BossEnvironmentRegistry.Definition {
    @Override public ResourceLocation entityId() { return ResourceLocation.fromNamespaceAndPath("minecraft", "ender_dragon"); }
    @Override public BossEnvironmentCatalog.Profile profile() {
        return new BossEnvironmentCatalog.Profile(entityId(), List.of(Blocks.END_STONE.defaultBlockState()), 32, 3, 14, 100, null, 0, new BossEnvironmentCatalog.Pillars(Blocks.OBSIDIAN.defaultBlockState(), 4, 5, 18, 26, true));
    }
}
