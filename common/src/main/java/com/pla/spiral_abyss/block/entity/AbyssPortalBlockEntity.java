package com.pla.spiral_abyss.block.entity;

import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import java.util.UUID;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class AbyssPortalBlockEntity extends BlockEntity {
    private UUID portalId = UUID.randomUUID();
    private UUID host;
    private int displayedFloor;
    private boolean hostRunning;
    public UUID portalId() { return portalId; }
    public UUID host() { return host; }
    public int displayedFloor() { return displayedFloor; }
    public boolean hostRunning() { return hostRunning; }
    public void bindHost(UUID host) { this.host = host; setChanged(); refreshIndicator(); }
    private void refreshIndicator() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        var bound = AbyssParties.get(serverLevel.getServer()).portals().boundSession(this);
        int floor = bound == null ? 0 : bound.controller().currentFloor();
        boolean running = bound != null && bound.controller().active();
        if (floor == displayedFloor && running == hostRunning) return;
        displayedFloor = floor; hostRunning = running;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.putInt("DisplayedFloor", displayedFloor); tag.putBoolean("HostRunning", hostRunning);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putUUID("PortalId", portalId);
        if (host != null) tag.putUUID("Host", host);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("PortalId")) portalId = tag.getUUID("PortalId");
        host = tag.hasUUID("Host") ? tag.getUUID("Host") : null;
        displayedFloor = Math.max(0, Math.min(7, tag.getInt("DisplayedFloor")));
        hostRunning = tag.getBoolean("HostRunning");
    }
    public AbyssPortalBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityInit.ABYSS_PORTAL.get(), pos, state); }
    public AbyssPortalFrame frame() { return AbyssPortalFrame.fromActivation(worldPosition, getBlockState()); }
    public static void tick(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 5 == 0 && level.getBlockEntity(pos) instanceof AbyssPortalBlockEntity portal)
            portal.refreshIndicator();
        if (level.getGameTime() % 20 != 0) return;
        var frame = AbyssPortalFrame.fromActivation(pos, state);
        if (!frame.loaded(level)) return;
        if (state.getValue(AbyssGateActivationBlock.ACTIVE)) {
            if (!level.dimension().equals(Level.OVERWORLD) || !frame.active(level)) frame.deactivate(level);
        } else if (level instanceof ServerLevel serverLevel) {
            // Completing or repairing the frame also activates its already-placed controller.
            frame.activate(serverLevel);
        }
    }
    public AABB getRenderBoundingBox() {
        return new AABB(frame().at(-AbyssPortalFrame.RADIUS, 0), frame().at(AbyssPortalFrame.RADIUS, AbyssPortalFrame.TOP).offset(1, 1, 1))
                .inflate(1, .5, 1);
    }
}
