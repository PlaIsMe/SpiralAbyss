package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;

public final class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpiralAbyss.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SPIRAL_ABYSS = TABS.register("spiral_abyss", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.spiral_abyss"))
            .icon(() -> {
                // Use the supplied key artwork only for the tab, without registering a key item.
                ItemStack icon = new ItemStack(ItemInit.ABYSS_GATE_ACTIVATE_BLOCK_ITEM.get());
                icon.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
                return icon;
            })
            .displayItems((parameters, output) -> {
                output.accept(ItemInit.ABYSS_GATE_VERTICAL_ITEM.get());
                output.accept(ItemInit.ABYSS_GATE_HORIZONTAL_ITEM.get());
                output.accept(ItemInit.ABYSS_GATE_CURVE_ITEM.get());
                output.accept(ItemInit.ABYSS_GATE_ACTIVATE_BLOCK_ITEM.get());
                output.accept(ItemInit.ABYSS_BASE_BLOCK_ITEM.get());
                output.accept(ItemInit.ABYSS_PILLAR_ITEM.get());
                output.accept(ItemInit.ABYSS_RUNE_1_ITEM.get());
                output.accept(ItemInit.ABYSS_RUNE_2_ITEM.get());
                output.accept(ItemInit.ABYSS_RUNE_3_ITEM.get());
                output.accept(ItemInit.ABYSS_RUNE_4_ITEM.get());
                output.accept(ItemInit.ABYSS_BORDER_MIDDLE_ITEM.get());
                output.accept(ItemInit.ABYSS_BORDER_OUTSIDE_ITEM.get());
            }).build());
    private CreativeTabInit() {}
}
