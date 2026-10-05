package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.init.EntityInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.ArrayDeque;
import java.util.List;

/** Ordinary collectible loot with entity-only rarity metadata (never modifies inventory stacks). */
public final class ChestLootEntity extends ItemEntity {
    private static final EntityDataAccessor<Boolean> RARE = SynchedEntityData.defineId(ChestLootEntity.class, EntityDataSerializers.BOOLEAN);
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    public ChestLootEntity(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }
    public ChestLootEntity(Level level, double x, double y, double z, ItemStack stack, boolean rare) {
        this(EntityInit.CHEST_LOOT.get(), level);
        setPos(x, y, z); setItem(stack.copy()); entityData.set(RARE, rare);
        lifespan = stack.getEntityLifespan(level);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(RARE, false); }
    public boolean rare() { return entityData.get(RARE); }
    public List<Vec3> trail() { return List.copyOf(trail); }
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(4); }
    @Override public void tick() {
        if (level().isClientSide) {
            if (!onGround()) {
                trail.addLast(position().add(0, 0.15, 0));
                while (trail.size() > 12) trail.removeFirst();
            } else trail.clear();
        }
        super.tick();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putBoolean("RareChestLoot", rare());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(RARE, tag.getBoolean("RareChestLoot"));
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
