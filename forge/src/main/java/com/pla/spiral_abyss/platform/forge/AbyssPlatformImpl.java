package com.pla.spiral_abyss.platform.forge;

import com.pla.spiral_abyss.forge.world.AbyssTeleporter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class AbyssPlatformImpl {
    public static CompoundTag data(Entity entity) {
        return entity.getPersistentData();
    }

    public static boolean canShieldBlock(ItemStack stack) {
        return stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK);
    }

    public static CreativeModeTab.Builder creativeTab() {
        return CreativeModeTab.builder();
    }

    public static Entity transfer(
            ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        return player.changeDimension(level, new AbyssTeleporter(position, yaw, pitch));
    }
}
