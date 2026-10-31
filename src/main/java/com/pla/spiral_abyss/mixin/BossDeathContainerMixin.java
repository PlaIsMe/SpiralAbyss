package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.event.BossDeathBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class BossDeathContainerMixin {
    // Some mods fill slots directly after attempting to place a death chest,
    // even if placement failed. Preserve any existing container's contents.
    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$suppressDeathItems(int slot, ItemStack stack, CallbackInfo callback) {
        var container = (RandomizableContainerBlockEntity) (Object) this;
        if (BossDeathBlocks.suppressLoot(container.getLevel(), container.getBlockPos())) callback.cancel();
    }

    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceKey;)V",
            at = @At("HEAD"), cancellable = true)
    private void spiral_abyss$suppressAssignedDeathLoot(ResourceKey<LootTable> table, CallbackInfo callback) {
        var container = (RandomizableContainerBlockEntity) (Object) this;
        if (BossDeathBlocks.suppressLoot(container.getLevel(), container.getBlockPos())) callback.cancel();
    }
}
