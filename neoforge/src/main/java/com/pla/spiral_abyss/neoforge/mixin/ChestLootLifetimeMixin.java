package com.pla.spiral_abyss.neoforge.mixin;

import com.pla.spiral_abyss.entity.ChestLootEntity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestLootEntity.class)
public abstract class ChestLootLifetimeMixin extends ItemEntity {
    protected ChestLootLifetimeMixin(
            net.minecraft.world.entity.EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;Z)V",
            at = @At("RETURN"),
            remap = false)
    private void spiralAbyss$lifespan(
            Level level,
            double x,
            double y,
            double z,
            ItemStack stack,
            boolean rare,
            CallbackInfo ci) {
        lifespan = stack.getEntityLifespan(level);
    }
}
