package com.pla.spiral_abyss.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;

public final class AbyssPlatform {
    @ExpectPlatform
    public static CompoundTag data(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canShieldBlock(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static CreativeModeTab.Builder creativeTab() {
        throw new AssertionError();
    }
}
