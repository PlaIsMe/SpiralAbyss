package com.pla.spiral_abyss.platform.neoforge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.ItemAbilities;

public final class AbyssPlatformImpl {
    public static CompoundTag data(Entity entity) {
        return entity.getPersistentData();
    }

    public static boolean canShieldBlock(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    public static CreativeModeTab.Builder creativeTab() {
        return CreativeModeTab.builder();
    }
}
