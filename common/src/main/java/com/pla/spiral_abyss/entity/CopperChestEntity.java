package com.pla.spiral_abyss.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class CopperChestEntity extends LootChestEntity {
    public CopperChestEntity(EntityType<? extends Mob> type, Level level) { super(type, level, 1); }
}
