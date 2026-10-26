package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.MobTickContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevent opt-in arena mobs from replacing player inventory stacks during their scripted attacks. */
@Mixin(Inventory.class)
public abstract class AbyssPlayerInventoryMixin {
    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$protectInventory(int slot, ItemStack stack, CallbackInfo callback) {
        if (MobTickContext.protectsInventory()) callback.cancel();
    }
}
