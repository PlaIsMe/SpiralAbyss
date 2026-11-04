package com.pla.spiral_abyss.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pla.spiral_abyss.fabric.FabricPlacementContext;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class ItemPlacementMixin {
    @WrapMethod(method = "useOn")
    private InteractionResult spiralAbyss$itemPlacement(
            UseOnContext context, Operation<InteractionResult> original) {
        var previous = FabricPlacementContext.player();
        try {
            FabricPlacementContext.restore(
                    context.getLevel().isClientSide ? null : context.getPlayer());
            return original.call(context);
        } finally {
            FabricPlacementContext.restore(previous);
        }
    }
}
