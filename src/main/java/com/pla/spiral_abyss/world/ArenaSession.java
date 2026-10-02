package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.*;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.entity.*;
import com.pla.spiral_abyss.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** One leased arena, one host's progress, and a roster of at most four players. */
public final class ArenaSession {
    private static final TicketType<UUID> MOB_TICKET = TicketType.create("spiral_abyss_mob", UUID::compareTo, 40);
    private final AbyssParties parties;
    private final ServerLevel level;
    private final AbyssPlayers.Profile profile;
    private final FloorController controller;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, Vec3> lastSafe = new HashMap<>();
    private boolean displayReady;
    private final Set<UUID> admitted = new LinkedHashSet<>();

    public ArenaSession(AbyssParties parties, ServerLevel level, AbyssPlayers.Profile profile) {
        this.parties = parties; this.level = level; this.profile = profile;
        this.controller = new FloorController(this, FloorCatalog::snapshot);
        AbyssDimension.initializeArena(level, profile);
    }
    public ServerLevel level() { return level; }
    public AbyssPlayers.Profile profile() { return profile; }
    public UUID host() { return profile.owner; }
    public ArenaBounds bounds() { return profile.bounds(); }
    public FloorController controller() { return controller; }
    public Set<UUID> participants() { return Set.copyOf(participants); }
    public Set<UUID> admitted() { return Set.copyOf(admitted); }
    public boolean contains(ServerPlayer player) { return admitted.contains(player.getUUID()) && player.level() == level; }
    public List<ServerPlayer> players() {
        return level.players().stream().filter(this::contains).toList();
    }
    public boolean canAdmit(UUID player) {
        if (!admitted.contains(player) && admitted.size() >= 4) return false;
        return !controller.active() || participants.contains(player);
    }
    public void admit(UUID player) { admitted.add(player); }
    public void remove(UUID player) { admitted.remove(player); lastSafe.remove(player); }
    public boolean captureParty() {
        var living = players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
        if (living.stream().noneMatch(p -> p.getUUID().equals(host())) || living.size() > 4) return false;
        participants.clear(); living.forEach(p -> participants.add(p.getUUID())); return true;
    }
    public void clearParticipants() { participants.clear(); }

    public void tick() {
        // Entity persistence loads asynchronously. Never create replacement furniture before that finishes.
        BlockPos center = BlockPos.containing(bounds().center());
        if (!displayReady && level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(center).toLong())) { displayReady = true; refreshDisplay(); }
        for (ServerPlayer player : players()) containPlayer(player);
        if (!displayReady) return;
        controller.tick();
        var progress = profile.progress;
        if (progress.chestTicks() >= 0) {
            progress.tickOpening();
            if (!progress.claimed() && progress.chestTicks() >= RewardChestEntity.OPEN_TICKS) {
                FloorRewards.Bundle bundle = progress.pending();
                int floor = progress.floor();
                Set<UUID> beneficiaries = progress.beneficiaries();
                // Mark claims before releasing a single shared loot bundle. Repeated clicks cannot pay twice.
                parties.claim(beneficiaries.stream().filter(id -> !id.equals(host())).toList(), floor);
                progress.claimOpenedChest();
                if (bundle != null) FloorRewards.release(level, bounds().center(), bundle);
            }
            if (progress.chestTicks() >= RewardChestEntity.OPEN_TICKS + RewardChestEntity.SHAKE_TICKS) progress.finishOpening();
            refreshDisplay();
        }
        if (level.getGameTime() % 20 == 0) refreshDisplay();
    }

    public void containPlayer(ServerPlayer player) {
        if (bounds().outsidePlayer(player)) {
            player.stopRiding();
            Vec3 safe = lastSafe.getOrDefault(player.getUUID(), bounds().entrance());
            if (!level.noCollision(player, player.getBoundingBox().move(safe.subtract(player.position())))) safe = bounds().entrance();
            player.teleportTo(level, safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
        } else if (player.onGround()) lastSafe.put(player.getUUID(), player.position());
    }

    public void rescueMob(Mob mob) {
        if (!bounds().rescueMob(mob)) return;
        Vec3 center = bounds().center();
        mob.teleportTo(center.x, center.y + 0.1, center.z);
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO); mob.fallDistance = 0;
    }

    public void keepMobActive(Mob mob) {
        // Also called from the controller's tracked list when a boss teleports beyond player view distance.
        level.getChunkSource().addRegionTicket(MOB_TICKET, mob.chunkPosition(), 2, mob.getUUID());
    }

    public void refreshDisplay() {
        if (!displayReady) return;
        List<ArenaInteractable> furniture = level.getEntitiesOfClass(ArenaInteractable.class, bounds().structureArea());
        boolean chest = profile.progress.completed();
        boolean hide = controller.active();
        ArenaInteractable keeper = null;
        for (ArenaInteractable entity : furniture) {
            if (!hide && keeper == null && host().equals(entity.arenaHost())
                    && ((chest && entity instanceof RewardChestEntity) || (!chest && entity instanceof KeyEntity))) keeper = entity;
            else entity.discard();
        }
        if (hide) return;
        if (keeper == null) {
            keeper = chest ? EntityInit.REWARD_CHEST.get().create(level) : EntityInit.KEY_ENTITY.get().create(level);
            if (keeper == null) return;
            keeper.setArenaHost(host());
            keeper.moveTo(bounds().center().x, bounds().center().y, bounds().center().z, 0, 0);
            level.addFreshEntity(keeper);
        }
        if (keeper instanceof RewardChestEntity rewardChest) {
            int ticks = profile.progress.chestTicks();
            int phase = ticks >= 0 ? (ticks < RewardChestEntity.OPEN_TICKS ? RewardChestEntity.OPEN : RewardChestEntity.SHAKE)
                    : profile.progress.claimed() ? RewardChestEntity.LOOTED : RewardChestEntity.IDLE;
            long start = ticks < 0 ? 0 : level.getGameTime() - ticks + (phase == RewardChestEntity.SHAKE ? RewardChestEntity.OPEN_TICKS : 0);
            rewardChest.setPhase(phase, start);
        }
    }

    public void close() {
        controller.close();
        clearParticipants();
        level.getEntitiesOfClass(ArenaInteractable.class, bounds().structureArea()).forEach(Entity::discard);
        AbyssDimension.releaseArena(level, profile);
    }
}
