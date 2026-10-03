package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.block.AbyssRuneBlock;
import com.pla.spiral_abyss.block.AbyssPortalBlock;
import com.pla.spiral_abyss.block.AbyssPortalAnchorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SpiralAbyss.MOD_ID);

    public static final RegistryObject<AbyssPortalBlock> ABYSS_PORTAL = BLOCKS.register("abyss_portal",
            () -> new AbyssPortalBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_PORTAL).noLootTable()));
    public static final RegistryObject<AbyssPortalAnchorBlock> ABYSS_PORTAL_ANCHOR = BLOCKS.register("abyss_portal_anchor",
            () -> new AbyssPortalAnchorBlock(BlockBehaviour.Properties.copy(Blocks.POLISHED_ANDESITE_SLAB)
                    .dropsLike(Blocks.POLISHED_ANDESITE_SLAB).lightLevel(state -> 11)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    public static final RegistryObject<Block> SPINNING_BLOCK = BLOCKS.register("spinning_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BEDROCK)));

    public static final RegistryObject<Block> ABYSS_BASE_BLOCK = BLOCKS.register("abyss_base_block",
            () -> new Block(unbreakable()));

    public static final RegistryObject<Block> ABYSS_PILLAR = BLOCKS.register("abyss_pillar",
            () -> new Block(unbreakable()));

    public static final RegistryObject<Block> ABYSS_RUNE_1 = BLOCKS.register("abyss_rune_1",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final RegistryObject<Block> ABYSS_RUNE_2 = BLOCKS.register("abyss_rune_2",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final RegistryObject<Block> ABYSS_RUNE_3 = BLOCKS.register("abyss_rune_3",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final RegistryObject<Block> ABYSS_RUNE_4 = BLOCKS.register("abyss_rune_4",
            () -> new AbyssRuneBlock(unbreakable()));

    public static final RegistryObject<Block> ABYSS_BORDER_MIDDLE = BLOCKS.register("abyss_border_middle",
            () -> new Block(unbreakable()));

    public static final RegistryObject<Block> ABYSS_BORDER_OUTSIDE = BLOCKS.register("abyss_border_outside",
            () -> new Block(unbreakable()));

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.copy(Blocks.BEDROCK)
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
