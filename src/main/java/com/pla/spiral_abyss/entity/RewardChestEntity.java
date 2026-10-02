package com.pla.spiral_abyss.entity;

import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class RewardChestEntity extends ArenaInteractable {
    public static final int IDLE = 0, OPEN = 1, SHAKE = 2, LOOTED = 3;
    public static final int OPEN_TICKS = 11, SHAKE_TICKS = 20;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_START = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.LONG);

    public RewardChestEntity(EntityType<? extends Mob> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData() {
        super.defineSynchedData(); entityData.define(PHASE, IDLE); entityData.define(PHASE_START, 0L);
    }
    public int phase() { return entityData.get(PHASE); }
    public long phaseStart() { return entityData.get(PHASE_START); }
    public void setPhase(int phase, long start) {
        entityData.set(PHASE, phase); entityData.set(PHASE_START, start);
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) AbyssParties.get(serverPlayer.server).openChest(serverPlayer, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
