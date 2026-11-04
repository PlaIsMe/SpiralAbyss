package com.pla.spiral_abyss.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

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

    @ExpectPlatform
    public static Entity transfer(
            ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        throw new AssertionError();
    }
}
