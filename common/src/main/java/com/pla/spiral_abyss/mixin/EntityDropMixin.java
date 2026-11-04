package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.AbyssMobDrops;
import com.pla.spiral_abyss.encounter.MobTickContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDropMixin {
    // Native remove overrides may directly create items before super.remove, even when
    // arena cleanup discards a helper outside its own tick or before reinforcement adoption.
    @Redirect(method = "discard", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void spiralAbyss$scopeRemoval(Entity entity, Entity.RemovalReason reason) {
        MobTickContext.tick(entity, () -> entity.remove(reason));
    }

    // Every vanilla spawnAtLocation overload delegates to this method. Also covers live drops,
    // e.g. chicken eggs, shearing, and custom mobs that use the standard entity drop helper.
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$preventMobItemDrop(ItemStack stack, float offset,
                                               CallbackInfoReturnable<ItemEntity> callback) {
        if (AbyssMobDrops.suppress((Entity) (Object) this)) callback.setReturnValue(null);
    }
}
