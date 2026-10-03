package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.item.AbyssDraftItem;
import com.pla.spiral_abyss.item.AbyssKeyItem;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SpiralAbyss.MOD_ID);

    public static final RegistryObject<Item> ABYSS_KEY = ITEMS.register("abyss_key",
            () -> new AbyssKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ABYSS_DRAFT = ITEMS.register("abyss_draft",
            () -> new AbyssDraftItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

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

    private static RegistryObject<BlockItem> blockItem(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    }
}
