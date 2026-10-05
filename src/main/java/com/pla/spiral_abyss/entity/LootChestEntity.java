package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Shared furniture state; the session owns claims and the saved dispensing cursor. */
public abstract class LootChestEntity extends ArenaInteractable {
    public static final int IDLE = 0, OPEN = 1, DISPENSING = 2, LOOTED = 3, SPAWN = 4;
    public static final int SPAWN_TICKS = 11, OPEN_TICKS = 10, DROP_INTERVAL_TICKS = 6;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(LootChestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_START = SynchedEntityData.defineId(LootChestEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> RARE = SynchedEntityData.defineId(LootChestEntity.class, EntityDataSerializers.BOOLEAN);
    private final int tier;

    protected LootChestEntity(EntityType<? extends Mob> type, Level level, int tier) {
        super(type, level); this.tier = tier;
    }
    public int tier() { return tier; }
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(1, 2, 1); }
    @Override public void tick() {
        super.tick();
        if (phase() != IDLE) return;
        Player player = level().getNearestPlayer(this, 10);
        if (player != null) lookAt(EntityAnchorArgument.Anchor.EYES, player.position());
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PHASE, IDLE); entityData.define(PHASE_START, 0L); entityData.define(RARE, false);
    }
    public int phase() { return entityData.get(PHASE); }
    public long phaseStart() { return entityData.get(PHASE_START); }
    public boolean rarePayout() { return entityData.get(RARE); }
    public void setRarePayout(boolean rare) { entityData.set(RARE, tier == 3 && rare); }
    public void setPhase(int phase, long start) {
        entityData.set(PHASE, phase); entityData.set(PHASE_START, start);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChestPhase", phase()); tag.putLong("ChestPhaseStart", phaseStart());
        tag.putBoolean("RarePayout", rarePayout());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPhase(Math.max(IDLE, Math.min(SPAWN, tag.getInt("ChestPhase"))), tag.getLong("ChestPhaseStart"));
        setRarePayout(tag.getBoolean("RarePayout"));
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) AbyssParties.get(serverPlayer.server).openChest(serverPlayer, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
