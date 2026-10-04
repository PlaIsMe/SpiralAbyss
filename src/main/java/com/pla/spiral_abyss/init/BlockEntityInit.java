package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.*;

public final class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SpiralAbyss.MOD_ID);
    public static final RegistryObject<BlockEntityType<AbyssPortalBlockEntity>> ABYSS_PORTAL = BLOCK_ENTITIES.register("abyss_gate_portal",
            () -> BlockEntityType.Builder.of(AbyssPortalBlockEntity::new, BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get()).build(null));
    private BlockEntityInit() {}
}
