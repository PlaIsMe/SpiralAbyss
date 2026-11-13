package com.pla.spiral_abyss.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pla.spiral_abyss.encounter.MobTickContext;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Part breaks can create items during a player's attack, outside the monster's tick. */
@Pseudo
@Mixin(targets = "net.saksolm.monsterexpansion.entity.custom.AbstractLargeMonster", remap = false)
public abstract class MonsterPartLootMixin {
    // This is the mod's own method name, not an obfuscated Minecraft method.
    @WrapMethod(method = "applyDamageToPart")
    private boolean spiral_abyss$partLoot(String partName, float amount, DamageSource source,
                                         Operation<Boolean> original) {
        Entity entity = (Entity) (Object) this;
        if (!entity.level().dimension().equals(AbyssDimension.LEVEL))
            return original.call(partName, amount, source);
        boolean[] result = new boolean[1];
        MobTickContext.tick(entity, () -> result[0] = original.call(partName, amount, source));
        return result[0];
    }
}
