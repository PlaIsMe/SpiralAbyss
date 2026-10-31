package com.pla.spiral_abyss.init;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, SpiralAbyss.MOD_ID);

    public static final DeferredHolder<Item, BlockItem> SPINNING_BLOCK_ITEM = ITEMS.register("spinning_block",
            () -> new BlockItem(BlockInit.SPINNING_BLOCK.get(),
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, BlockItem> ABYSS_BASE_BLOCK_ITEM = blockItem("abyss_base_block", BlockInit.ABYSS_BASE_BLOCK);
    public static final DeferredHolder<Item, BlockItem> ABYSS_PILLAR_ITEM = blockItem("abyss_pillar", BlockInit.ABYSS_PILLAR);
    public static final DeferredHolder<Item, BlockItem> ABYSS_RUNE_1_ITEM = blockItem("abyss_rune_1", BlockInit.ABYSS_RUNE_1);
    public static final DeferredHolder<Item, BlockItem> ABYSS_RUNE_2_ITEM = blockItem("abyss_rune_2", BlockInit.ABYSS_RUNE_2);
    public static final DeferredHolder<Item, BlockItem> ABYSS_RUNE_3_ITEM = blockItem("abyss_rune_3", BlockInit.ABYSS_RUNE_3);
    public static final DeferredHolder<Item, BlockItem> ABYSS_RUNE_4_ITEM = blockItem("abyss_rune_4", BlockInit.ABYSS_RUNE_4);
    public static final DeferredHolder<Item, BlockItem> ABYSS_BORDER_MIDDLE_ITEM = blockItem("abyss_border_middle", BlockInit.ABYSS_BORDER_MIDDLE);
    public static final DeferredHolder<Item, BlockItem> ABYSS_BORDER_OUTSIDE_ITEM = blockItem("abyss_border_outside", BlockInit.ABYSS_BORDER_OUTSIDE);
    public static final DeferredHolder<Item, BlockItem> ABYSS_GATE_VERTICAL_ITEM = blockItem("abyss_gate_vertical", BlockInit.ABYSS_GATE_VERTICAL);
    public static final DeferredHolder<Item, BlockItem> ABYSS_GATE_HORIZONTAL_ITEM = blockItem("abyss_gate_horizontal", BlockInit.ABYSS_GATE_HORIZONTAL);
    public static final DeferredHolder<Item, BlockItem> ABYSS_GATE_CURVE_ITEM = blockItem("abyss_gate_curve", BlockInit.ABYSS_GATE_CURVE);
    public static final DeferredHolder<Item, BlockItem> ABYSS_GATE_ACTIVATE_BLOCK_ITEM = blockItem("abyss_gate_activate_block", BlockInit.ABYSS_GATE_ACTIVATE_BLOCK);

    private static DeferredHolder<Item, BlockItem> blockItem(String name, DeferredHolder<Block, ? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    }
}
