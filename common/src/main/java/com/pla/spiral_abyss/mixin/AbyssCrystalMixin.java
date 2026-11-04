package com.pla.spiral_abyss.mixin;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironments;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystal.class)
public abstract class AbyssCrystalMixin {
    @Inject(method = "onDestroyedBy", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$notifyOwner(DamageSource source, CallbackInfo ci) {
        EndCrystal crystal = (EndCrystal) (Object) this;
        if (!(crystal.level() instanceof ServerLevel level) || !level.dimension().equals(AbyssDimension.LEVEL) || !AbyssPlatform.data(crystal).hasUUID(BossEnvironments.CRYSTAL_OWNER)) return;
        if (level.getEntity(AbyssPlatform.data(crystal).getUUID(BossEnvironments.CRYSTAL_OWNER)) instanceof EnderDragon dragon) dragon.onCrystalDestroyed(crystal, crystal.blockPosition(), source);
        ci.cancel();
    }
}
