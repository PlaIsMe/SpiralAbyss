package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SpiralAbyss.MOD_ID);

    public static final RegistryObject<BlockItem> SPINNING_BLOCK_ITEM = ITEMS.register("spinning_block",
            () -> new BlockItem(BlockInit.SPINNING_BLOCK.get(),
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<BlockItem> ABYSS_BASE_BLOCK_ITEM = blockItem("abyss_base_block", BlockInit.ABYSS_BASE_BLOCK);
    public static final RegistryObject<BlockItem> ABYSS_PILLAR_ITEM = blockItem("abyss_pillar", BlockInit.ABYSS_PILLAR);
    public static final RegistryObject<BlockItem> ABYSS_RUNE_1_ITEM = blockItem("abyss_rune_1", BlockInit.ABYSS_RUNE_1);
    public static final RegistryObject<BlockItem> ABYSS_RUNE_2_ITEM = blockItem("abyss_rune_2", BlockInit.ABYSS_RUNE_2);
    public static final RegistryObject<BlockItem> ABYSS_RUNE_3_ITEM = blockItem("abyss_rune_3", BlockInit.ABYSS_RUNE_3);
    public static final RegistryObject<BlockItem> ABYSS_RUNE_4_ITEM = blockItem("abyss_rune_4", BlockInit.ABYSS_RUNE_4);
    public static final RegistryObject<BlockItem> ABYSS_BORDER_MIDDLE_ITEM = blockItem("abyss_border_middle", BlockInit.ABYSS_BORDER_MIDDLE);
    public static final RegistryObject<BlockItem> ABYSS_BORDER_OUTSIDE_ITEM = blockItem("abyss_border_outside", BlockInit.ABYSS_BORDER_OUTSIDE);
    public static final RegistryObject<BlockItem> ABYSS_GATE_VERTICAL_ITEM = blockItem("abyss_gate_vertical", BlockInit.ABYSS_GATE_VERTICAL);
    public static final RegistryObject<BlockItem> ABYSS_GATE_HORIZONTAL_ITEM = blockItem("abyss_gate_horizontal", BlockInit.ABYSS_GATE_HORIZONTAL);
    public static final RegistryObject<BlockItem> ABYSS_GATE_CURVE_ITEM = blockItem("abyss_gate_curve", BlockInit.ABYSS_GATE_CURVE);
    public static final RegistryObject<BlockItem> ABYSS_GATE_ACTIVATE_BLOCK_ITEM = blockItem("abyss_gate_activate_block", BlockInit.ABYSS_GATE_ACTIVATE_BLOCK);

    private static RegistryObject<BlockItem> blockItem(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    }
}
