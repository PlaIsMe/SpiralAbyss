package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.fabric.FabricGameplayHooks;
import com.pla.spiral_abyss.platform.event.AbyssEvents.PlayerEvent;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    @Unique private ItemStack spiralAbyss$before;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void spiralAbyss$before(Player player, CallbackInfo ci) {
        spiralAbyss$before = ((ItemEntity) (Object) this).getItem().copy();
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void spiralAbyss$after(Player player, CallbackInfo ci) {
        var entity = (ItemEntity) (Object) this;
        if (entity.level().isClientSide || spiralAbyss$before == null) return;
        int acquired = spiralAbyss$before.getCount() - entity.getItem().getCount();
        if (acquired > 0) {
            var stack = spiralAbyss$before.copy();
            stack.setCount(acquired);
            FabricGameplayHooks.post(new PlayerEvent.ItemPickupEvent(player, entity, stack));
        }
        spiralAbyss$before = null;
    }
}
