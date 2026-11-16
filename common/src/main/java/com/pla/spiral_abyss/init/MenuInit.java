package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.menu.RewardChestMenu;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class MenuInit {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(SpiralAbyss.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<MenuType<RewardChestMenu>> REWARD_CHEST = MENUS.register("reward_chest",
            () -> new MenuType<>(RewardChestMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private MenuInit() {}
}
