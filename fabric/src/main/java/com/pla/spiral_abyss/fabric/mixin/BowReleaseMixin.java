package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.ArrowLooseEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public abstract class BowReleaseMixin {
    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void spiralAbyss$release(
            ItemStack bow, Level level, LivingEntity user, int remaining, CallbackInfo ci) {
        if (!level.isClientSide && user instanceof net.minecraft.world.entity.player.Player player)
            FabricGameplayHooks.post(new ArrowLooseEvent(player, bow));
    }
}
