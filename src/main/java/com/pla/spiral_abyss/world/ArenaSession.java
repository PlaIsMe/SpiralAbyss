package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.*;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.entity.*;
import com.pla.spiral_abyss.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** One leased arena, one host's progress, and a roster captured at attempt start. */
public final class ArenaSession {
    public static final String DEFEATED_TAG = "spiral_abyss_defeated";
    private static final TicketType<UUID> MOB_TICKET = TicketType.create("spiral_abyss_mob", UUID::compareTo, 40);
    private final AbyssParties parties;
    private final ServerLevel level;
    private final AbyssPlayers.Profile profile;
    private final FloorController controller;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> rewardParticipants = new LinkedHashSet<>();
    private final Set<UUID> defeated = new HashSet<>();
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
    public List<com.pla.spiral_abyss.encounter.data.FloorDefinition.Enemy> cycleLineup(int floor, int wave,
            com.pla.spiral_abyss.encounter.data.FloorDefinition.Wave definition) { return parties.cycleLineup(floor, wave, definition); }
    public UUID host() { return profile.owner; }
    public ArenaBounds bounds() { return profile.bounds(); }
    public FloorController controller() { return controller; }
    public boolean selectFloor(ServerPlayer player, int floor) { return parties.enterFloor(player, floor); }
    public Set<UUID> participants() { return Set.copyOf(participants); }
    public Set<UUID> rewardParticipants() { return Set.copyOf(rewardParticipants); }
    public Set<UUID> eligibleRewards() {
        return rewardParticipants.stream().filter(id -> parties.rewardEligible(id, profile.progress.floor()))
                .collect(java.util.stream.Collectors.toSet());
    }
    public boolean partyCanEnterFloor(ServerPlayer requester, int floor) {
        for (ServerPlayer player : players()) if (!parties.checkUnlocked(requester, player, floor)) return false;
        return true;
    }
    public Set<UUID> admitted() { return Set.copyOf(admitted); }
    public boolean contains(ServerPlayer player) { return admitted.contains(player.getUUID()) && player.level() == level; }
    public List<ServerPlayer> players() {
        return level.players().stream().filter(this::contains).toList();
    }
    public boolean canAdmit(UUID player) {
        return !controller.active() || participants.contains(player);
    }
    public void admit(ServerPlayer player) {
        admitted.add(player.getUUID());
        CompoundTag mark = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(DEFEATED_TAG);
        if (controller.active() && (defeated.contains(player.getUUID())
                || (mark.hasUUID("Run") && mark.getUUID("Run").equals(controller.runId())
                && mark.hasUUID("Host") && mark.getUUID("Host").equals(host())))) {
            defeated.add(player.getUUID()); markDefeated(player); player.setGameMode(GameType.SPECTATOR);
        } else if (!mark.isEmpty()) recover(player, true);
    }
    public void remove(UUID player) { admitted.remove(player); lastSafe.remove(player); }
    public boolean captureParty() {
        var living = players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
        if (living.stream().noneMatch(p -> p.getUUID().equals(host()))) return false;
        participants.clear(); rewardParticipants.clear(); defeated.clear();
        living.forEach(p -> {
            participants.add(p.getUUID());
            if (parties.rewardEligible(p.getUUID(), profile.progress.floor())) rewardParticipants.add(p.getUUID());
        });
        return true;
    }
    public void clearParticipants() { participants.clear(); rewardParticipants.clear(); }
    public boolean isCombatant(ServerPlayer player) {
        return contains(player) && participants.contains(player.getUUID()) && !defeated.contains(player.getUUID())
                && player.isAlive() && !player.isSpectator();
    }
    public List<ServerPlayer> combatants() { return players().stream().filter(this::isCombatant).toList(); }
    public boolean waitingForParty(ServerPlayer player) {
        return controller.active() && defeated.contains(player.getUUID());
    }

    /** Called from cancellable LivingDeathEvent before death packets, drops or the respawn screen. */
    public void defeat(ServerPlayer player) {
        heal(player);
        if (controller.active() && participants.contains(player.getUUID())) {
            defeated.add(player.getUUID());
            markDefeated(player);
            player.setGameMode(GameType.SPECTATOR);
            returnToEntrance(player);
            if (combatants().isEmpty()) controller.defeated();
            else player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.spiral_abyss.spectating"));
        } else recover(player, true);
    }
    private void markDefeated(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag mark = new CompoundTag();
        mark.putUUID("Host", host()); mark.putUUID("Run", controller.runId());
        persisted.put(DEFEATED_TAG, mark);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
    public static boolean hasDefeatMarker(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).contains(DEFEATED_TAG);
    }
    public static void restoreSurvival(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(DEFEATED_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.setCamera(player);
        player.setGameMode(GameType.SURVIVAL);
        heal(player);
    }
    private static void heal(ServerPlayer player) {
        player.stopRiding(); player.removeAllEffects(); player.clearFire(); player.setTicksFrozen(0);
        player.setAirSupply(player.getMaxAirSupply()); player.setAbsorptionAmount(0);
        player.setHealth(player.getMaxHealth()); player.deathTime = 0; player.fallDistance = 0;
        player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(5);
        player.invulnerableTime = 60;
    }
    private void recover(ServerPlayer player, boolean teleport) {
        restoreSurvival(player);
        if (teleport) returnToEntrance(player);
    }
    private void returnToEntrance(ServerPlayer player) {
        Vec3 entrance = bounds().entrance();
        player.setCamera(player);
        player.teleportTo(level, entrance.x, entrance.y, entrance.z, 180, 0);
        player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
        lastSafe.put(player.getUUID(), entrance);
    }
    public void finishAttempt() {
        // Includes boss summons and lingering attacks, so recovery is safe after a team wipe.
        var area = new net.minecraft.world.phys.AABB(bounds().center().x - ArenaBounds.FLIGHT_RADIUS, 0,
                bounds().center().z - ArenaBounds.FLIGHT_RADIUS, bounds().center().x + ArenaBounds.FLIGHT_RADIUS,
                level.getMaxBuildHeight(), bounds().center().z + ArenaBounds.FLIGHT_RADIUS);
        level.getEntities((Entity) null, area, entity -> !(entity instanceof ArenaInteractable)
                && (entity instanceof Mob || entity instanceof net.minecraft.world.entity.projectile.Projectile
                || entity instanceof AreaEffectCloud || entity instanceof net.minecraft.world.entity.projectile.EvokerFangs))
                .forEach(Entity::discard);
        for (ServerPlayer player : players()) {
            if (defeated.contains(player.getUUID()) || hasDefeatMarker(player)) recover(player, true);
        }
        defeated.clear();
    }

    public void tick() {
        // Entity persistence loads asynchronously. Never create replacement furniture before that finishes.
        BlockPos center = BlockPos.containing(bounds().center());
        if (!displayReady && level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(center).toLong())) { displayReady = true; refreshDisplay(); }
        for (ServerPlayer player : players()) {
            if (waitingForParty(player) && !player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
            containPlayer(player);
        }
        if (!displayReady) return;
        controller.tick();
        var progress = profile.progress;
        if (progress.chestTicks() >= 0) {
            progress.tickOpening();
            if (progress.pending() != null && progress.chestTicks() >= RewardChestEntity.OPEN_TICKS) {
                FloorRewards.Bundle bundle = progress.pending();
                if (!progress.beneficiaries().isEmpty()) {
                    // Credit the party once; keep the saved bundle until all stacks have been dispensed.
                    parties.claim(progress.beneficiaries(), progress.floor());
                    progress.markBeneficiariesClaimed();
                }
                int shakingTicks = progress.chestTicks() - RewardChestEntity.OPEN_TICKS;
                if (shakingTicks % RewardChestEntity.DROP_INTERVAL_TICKS == 0) {
                    var stack = progress.takeNextReward();
                    if (stack != null) FloorRewards.releaseItem(level, bounds().center(), stack);
                }
                // Leave a full interval after the final pop, and shake for at least two seconds.
                if (progress.releasedItems() >= bundle.items().size()
                        && shakingTicks >= Math.max(RewardChestEntity.SHAKE_TICKS,
                        (long) bundle.items().size() * RewardChestEntity.DROP_INTERVAL_TICKS)) {
                    int experience = bundle.experience();
                    progress.claimOpenedChest();
                    FloorRewards.releaseExperience(level, bounds().center(), experience);
                }
            }
            if (progress.pending() == null && progress.chestTicks() >= RewardChestEntity.OPEN_TICKS + RewardChestEntity.SHAKE_TICKS)
                progress.finishOpening();
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
        boolean chest = controller.showsChest();
        boolean hide = controller.active();
        ArenaInteractable keeper = null;
        ScrollEntity scroll = null;
        for (ArenaInteractable entity : furniture) {
            if (entity instanceof ScrollEntity candidate) {
                if (!hide && scroll == null && host().equals(entity.arenaHost())) scroll = candidate;
                else entity.discard();
                continue;
            }
            if (!hide && keeper == null && host().equals(entity.arenaHost())
                    && ((chest && entity instanceof RewardChestEntity) || (!chest && entity instanceof KeyEntity))) keeper = entity;
            else entity.discard();
        }
        if (hide) return;
        if (scroll == null) {
            scroll = EntityInit.SCROLL_ENTITY.get().create(level);
            if (scroll != null) {
                scroll.setArenaHost(host());
                Vec3 position = bounds().entrance().add(0, 1, 20);
                scroll.moveTo(position.x, position.y, position.z, 180, 0);
                // No-AI furniture does not initialize its rendered body yaw from moveTo.
                scroll.yBodyRot = scroll.yBodyRotO = 180;
                scroll.yHeadRot = scroll.yHeadRotO = 180;
                level.addFreshEntity(scroll);
            }
        }
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
                    : profile.progress.pending() != null ? RewardChestEntity.IDLE : RewardChestEntity.LOOTED;
            long start = ticks < 0 ? 0 : level.getGameTime() - ticks + (phase == RewardChestEntity.SHAKE ? RewardChestEntity.OPEN_TICKS : 0);
            rewardChest.setPhase(phase, start);
        }
    }

    /** Restore blocks without changing claims, pending rewards, or chest opening progress. */
    public void restoreStructure() {
        AbyssDimension.restoreArena(level, profile);
        lastSafe.clear();
        players().forEach(this::returnToEntrance);
    }

    public void resetArena() {
        controller.close();
        clearParticipants();
        restoreStructure();
        level.getEntities((Entity) null, bounds().structureArea(), entity -> entity instanceof ArenaInteractable
                || entity instanceof net.minecraft.world.entity.item.ItemEntity || entity instanceof ExperienceOrb)
                .forEach(Entity::discard);
        refreshDisplay();
    }

    public void close() {
        controller.close();
        clearParticipants();
        level.getEntitiesOfClass(ArenaInteractable.class, bounds().structureArea()).forEach(Entity::discard);
        AbyssDimension.releaseArena(level, profile);
    }
}
