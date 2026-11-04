package com.pla.spiral_abyss.platform.fabric;

import com.pla.spiral_abyss.platform.PersistentEntityData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

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

    public static Entity transfer(
            ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        player.teleportTo(level, position.x, position.y, position.z, yaw, pitch);
        return player.level() == level ? player : null;
    }
}
