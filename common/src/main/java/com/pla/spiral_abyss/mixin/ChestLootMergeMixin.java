package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.entity.ChestLootEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep reward stack identity and its synced rarity until pickup, including during flight. */
@Mixin(ItemEntity.class)
public abstract class ChestLootMergeMixin {
    @Inject(method = "isMergable", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$keepChestLootSeparate(CallbackInfoReturnable<Boolean> result) {
        if ((Object) this instanceof ChestLootEntity) result.setReturnValue(false);
    }
}
