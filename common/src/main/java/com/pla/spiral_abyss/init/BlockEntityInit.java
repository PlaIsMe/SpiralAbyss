package com.pla.spiral_abyss.init;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(SpiralAbyss.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockEntityType<AbyssPortalBlockEntity>> ABYSS_PORTAL = BLOCK_ENTITIES.register("abyss_gate_portal",
            () -> BlockEntityType.Builder.of(AbyssPortalBlockEntity::new, BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get()).build(null));
    private BlockEntityInit() {}
}
