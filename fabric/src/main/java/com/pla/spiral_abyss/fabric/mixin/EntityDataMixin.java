package com.pla.spiral_abyss.fabric.mixin;

import com.pla.spiral_abyss.platform.PersistentEntityData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class EntityDataMixin implements PersistentEntityData {
    @Unique private CompoundTag spiralAbyss$data;

    @Override
    public CompoundTag spiralAbyss$data() {
        if (spiralAbyss$data == null) spiralAbyss$data = new CompoundTag();
        return spiralAbyss$data;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void spiralAbyss$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (spiralAbyss$data != null && !spiralAbyss$data.isEmpty())
            tag.put("ForgeData", spiralAbyss$data.copy());
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void spiralAbyss$load(CompoundTag tag, CallbackInfo ci) {
        spiralAbyss$data =
                tag.contains("ForgeData", 10) ? tag.getCompound("ForgeData").copy() : null;
    }
}
