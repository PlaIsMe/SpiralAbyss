package com.pla.spiral_abyss.platform.fabric;

import com.pla.spiral_abyss.platform.PersistentEntityData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;

public final class AbyssPlatformImpl {
    public static CompoundTag data(Entity entity) {
        return ((PersistentEntityData) entity).spiralAbyss$data();
    }

    public static boolean canShieldBlock(ItemStack stack) {
        return stack.getUseAnimation() == UseAnim.BLOCK;
    }

    public static CreativeModeTab.Builder creativeTab() {
        return net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder();
    }
}
