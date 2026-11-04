package com.pla.spiral_abyss.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Persistent, immobile arena furniture with a server-validated owner identity. */
public abstract class ArenaInteractable extends Mob {
    private UUID arenaHost;
    protected ArenaInteractable(EntityType<? extends Mob> type, Level level) {
        super(type, level); setNoGravity(true); setPersistenceRequired();
    }
    public UUID arenaHost() { return arenaHost; }
    public void setArenaHost(UUID host) { arenaHost = host; }
    @Override public void tick() {
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        setDeltaMovement(Vec3.ZERO);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arenaHost != null) tag.putUUID("ArenaHost", arenaHost);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arenaHost = tag.hasUUID("ArenaHost") ? tag.getUUID("ArenaHost") : null;
        setInvisible(false); setNoGravity(true); setPersistenceRequired();
    }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public void die(DamageSource source) {}
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isNoAi() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeSeenAsEnemy() { return false; }
    @Override public void knockback(double strength, double x, double z) {}
    @Override protected void doPush(Entity entity) {}
}
