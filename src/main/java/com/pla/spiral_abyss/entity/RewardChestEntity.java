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

public final class RewardChestEntity extends ArenaInteractable {
    public static final int IDLE = 0, OPEN = 1, SHAKE = 2, LOOTED = 3;
    public static final int OPEN_TICKS = 11, SHAKE_TICKS = 40, DROP_INTERVAL_TICKS = 6;
    private int tier = 1;
    public int tier() { return tier; }
    public void setTier(int tier) { this.tier = Math.max(1, Math.min(3, tier)); }
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_START = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.LONG);

    public RewardChestEntity(EntityType<? extends Mob> type, Level level) { super(type, level); }
    @Override public void tick() {
        super.tick();
        if (phase() == LOOTED) return;
        Player player = level().getNearestPlayer(this, 10);
        if (player != null) lookAt(EntityAnchorArgument.Anchor.EYES, player.position());
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData(); entityData.define(PHASE, IDLE); entityData.define(PHASE_START, 0L);
    }
    public int phase() { return entityData.get(PHASE); }
    public long phaseStart() { return entityData.get(PHASE_START); }
    public void setPhase(int phase, long start) {
        entityData.set(PHASE, phase); entityData.set(PHASE_START, start);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RewardTier", tier);
        tag.putInt("ChestPhase", phase()); tag.putLong("ChestPhaseStart", phaseStart());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTier(tag.contains("RewardTier") ? tag.getInt("RewardTier") : 1);
        setPhase(Math.max(IDLE, Math.min(LOOTED, tag.getInt("ChestPhase"))), tag.getLong("ChestPhaseStart"));
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) AbyssParties.get(serverPlayer.server).openChest(serverPlayer, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
