package com.pla.spiral_abyss.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class GoldenChestEntity extends LootChestEntity {
    public GoldenChestEntity(EntityType<? extends Mob> type, Level level) { super(type, level, 3); }
}
