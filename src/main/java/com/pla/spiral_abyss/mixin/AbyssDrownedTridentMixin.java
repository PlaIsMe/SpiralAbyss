package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.encounter.WaveSpawner;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Drowned.class)
public abstract class AbyssDrownedTridentMixin {
    @ModifyArg(method = "performRangedAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"), index = 2)
    private ItemStack spiralAbyss$copyHeldTrident(ItemStack vanilla) {
        Drowned drowned = (Drowned) (Object) this;
        return drowned.getPersistentData().hasUUID(WaveSpawner.RUN_TAG) && drowned.getMainHandItem().is(Items.TRIDENT)
                ? drowned.getMainHandItem().copy() : vanilla;
    }
}
