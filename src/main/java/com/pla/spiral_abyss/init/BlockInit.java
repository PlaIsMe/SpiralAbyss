package com.pla.spiral_abyss.init;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.block.AbyssRuneBlock;
import com.pla.spiral_abyss.block.AbyssPortalBlock;
import com.pla.spiral_abyss.block.AbyssGateVerticalBlock;
import com.pla.spiral_abyss.block.AbyssGateSlabBlock;
import com.pla.spiral_abyss.block.AbyssGateCurveBlock;
import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.material.PushReaction;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, SpiralAbyss.MOD_ID);

    public static final DeferredHolder<Block, AbyssPortalBlock> ABYSS_PORTAL = BLOCKS.register("abyss_portal",
            () -> new AbyssPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL).noLootTable()
                    .lightLevel(state -> 15)));
    public static final DeferredHolder<Block, Block> SPINNING_BLOCK = BLOCKS.register("spinning_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)));

    public static final DeferredHolder<Block, Block> ABYSS_BASE_BLOCK = BLOCKS.register("abyss_base_block",
            () -> new Block(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_PILLAR = BLOCKS.register("abyss_pillar",
            () -> new Block(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_RUNE_1 = BLOCKS.register("abyss_rune_1",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_RUNE_2 = BLOCKS.register("abyss_rune_2",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_RUNE_3 = BLOCKS.register("abyss_rune_3",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_RUNE_4 = BLOCKS.register("abyss_rune_4",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_BORDER_MIDDLE = BLOCKS.register("abyss_border_middle",
            () -> new Block(unbreakable()));

    public static final DeferredHolder<Block, Block> ABYSS_BORDER_OUTSIDE = BLOCKS.register("abyss_border_outside",
            () -> new Block(unbreakable()));

    public static final DeferredHolder<Block, AbyssGateVerticalBlock> ABYSS_GATE_VERTICAL = BLOCKS.register("abyss_gate_vertical",
            () -> new AbyssGateVerticalBlock(gateProperties()));
    public static final DeferredHolder<Block, AbyssGateSlabBlock> ABYSS_GATE_HORIZONTAL = BLOCKS.register("abyss_gate_horizontal",
            () -> new AbyssGateSlabBlock(gateProperties()));
    public static final DeferredHolder<Block, AbyssGateCurveBlock> ABYSS_GATE_CURVE = BLOCKS.register("abyss_gate_curve",
            () -> new AbyssGateCurveBlock(gateProperties()));
    public static final DeferredHolder<Block, AbyssGateActivationBlock> ABYSS_GATE_ACTIVATE_BLOCK = BLOCKS.register("abyss_gate_activate_block",
            () -> new AbyssGateActivationBlock(gateProperties().dynamicShape()
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

    private static BlockBehaviour.Properties gateProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> 15);
    }

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
                .strength(-1.0F, 3_600_000.0F)
                .lightLevel(state -> 15);
    }

    public static boolean isAbyssArchitecture(Block block) {
        return block == ABYSS_BASE_BLOCK.get() || block == ABYSS_PILLAR.get()
                || block == ABYSS_RUNE_1.get() || block == ABYSS_RUNE_2.get()
                || block == ABYSS_RUNE_3.get() || block == ABYSS_RUNE_4.get()
                || block == ABYSS_BORDER_MIDDLE.get() || block == ABYSS_BORDER_OUTSIDE.get();
    }
}
