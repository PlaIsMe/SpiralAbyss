package com.pla.spiral_abyss.world;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.encounter.*;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
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
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.event.AbyssBlockProtection;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/** One leased arena and original owner's progress; normal attempts capture a fixed roster. */
public final class ArenaSession {
    public static final String DEFEATED_TAG = "spiral_abyss_defeated";
    private static final TicketType<UUID> MOB_TICKET = TicketType.create("spiral_abyss_mob", UUID::compareTo, 40);
    private final AbyssParties parties;
    private final ServerLevel level;
    private final AbyssPlayers.Profile profile;
    private final FloorController controller;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> rewardParticipants = new LinkedHashSet<>();
    // Unclaimed results live only for this visit. Disconnecting forfeits that player's offer.
    private int[] attemptStars = new int[3];
    private FloorDefinition.Rewards offeredRewards;
    private int displayedTiers;
    private final Set<UUID> defeated = new HashSet<>();
    private final Map<UUID, Vec3> lastSafe = new HashMap<>();
    private boolean displayReady;
    private long nextDisplayRefresh = Long.MAX_VALUE;
    private final Set<UUID> admitted = new LinkedHashSet<>();
    private UUID leader;

    public ArenaSession(AbyssParties parties, ServerLevel level, AbyssPlayers.Profile profile) {
        this.parties = parties; this.level = level; this.profile = profile;
        this.leader = profile.owner;
        this.controller = new FloorController(this, FloorCatalog::snapshot);
        AbyssDimension.initializeArena(level, profile);
    }
    public boolean endless() { return profile.endlessMode; }
    public long endlessSeed(int wave) { return parties.cycle().bannerPoseSeed() ^ host().getMostSignificantBits() ^ host().getLeastSignificantBits() ^ (0x9E3779B97F4A7C15L * wave); }
    public ServerLevel level() { return level; }
    public AbyssPlayers.Profile profile() { return profile; }
    public List<Challenge> challenges(int floor) { return parties.challenges(floor); }
    public ResourceLocation selectedWaveType(int floor, int wave, FloorDefinition.Wave definition) { return parties.cycle().selectedType(floor, wave, definition); }
    public List<FloorDefinition.Enemy> cycleLineup(int floor, int wave,
            FloorDefinition.Wave definition) { return parties.cycleLineup(floor, wave, definition); }
    public UUID host() { return profile.owner; }
    /** Leadership can change without moving the arena or its saved reward bundle. */
    public boolean isLeader(ServerPlayer player) { return player.getUUID().equals(leader); }
    public ArenaBounds bounds() { return profile.bounds(); }
    public FloorController controller() { return controller; }
    public boolean selectFloor(ServerPlayer player, int floor) { return parties.enterFloor(player, floor); }
    public boolean canAdvance(ServerPlayer player) {
        return canNavigate(player) && parties.profile(player.getUUID()).progress.claimed(controller.currentFloor());
    }
    /** An opened claim belongs to this player; other participants' unclicked chests do not hold them here. */
    public boolean canNavigate(ServerPlayer player) {
        if (endless()) return contains(player) && !controller.active();
        boolean openedOwnPayout = host().equals(player.getUUID()) && profile.progress.hasPayouts();
        return contains(player) && !controller.active() && (!looting()
                || (rewardParticipants.contains(player.getUUID()) || openedOwnPayout)
                && parties.profile(player.getUUID()).progress.claimed(controller.currentFloor()));
    }
    public Set<UUID> participants() { return Set.copyOf(participants); }
    public Set<UUID> rewardParticipants() { return Set.copyOf(rewardParticipants); }
    public boolean partyCanEnterFloor(ServerPlayer requester, int floor) {
        for (ServerPlayer player : players()) if (!parties.checkUnlocked(requester, player, floor)) return false;
        return true;
    }
    public Set<UUID> admitted() { return Set.copyOf(admitted); }
    public boolean contains(ServerPlayer player) { return admitted.contains(player.getUUID()) && player.level() == level; }
    public List<ServerPlayer> players() {
        return level.players().stream().filter(this::contains).filter(p -> !p.hasDisconnected()).toList();
    }
    public boolean canAdmit(UUID player) {
        if (endless()) return true;
        if (looting()) return admitted.contains(player) || admitted.isEmpty() && player.equals(host());
        return !controller.active() || admitted.contains(player) && participants.contains(player);
    }
    public boolean looting() {
        if (endless()) return controller.waitingForExperience();
        if (profile.progress.hasPayouts()) return true;
        for (int tier = 1; tier <= displayedTiers; tier++) if (!eligibleForTier(tier).isEmpty()) return true;
        return false;
    }
    private List<ServerPlayer> eligibleForTier(int tier) {
        if (offeredRewards == null) return List.of();
        var present = players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
        var eligible = ChallengeRewards.eligible(rewardParticipants, present.stream().map(ServerPlayer::getUUID).toList(),
                id -> parties.profile(id).progress, controller.currentFloor(), tier, attemptStars);
        return present.stream().filter(p -> eligible.contains(p.getUUID())).toList();
    }
    public void completeChallenge(int[] stars, FloorDefinition.Rewards rewards) {
        attemptStars = stars.clone(); offeredRewards = rewards;
        displayedTiers = 0;
        players().forEach(player -> player.sendSystemMessage(Component.translatable(
                "message.spiral_abyss.challenge_result", controller.currentFloor(), Arrays.stream(stars).sum())
                .append(" ").append(Component.translatable("message.spiral_abyss.menu_hint",
                        Component.keybind("key.spiral_abyss.menu")))));
        for (ServerPlayer player : players()) {
            if (!rewardParticipants.contains(player.getUUID())) continue;
            var progress = parties.profile(player.getUUID()).progress;
            displayedTiers = Math.max(displayedTiers, Arrays.stream(progress.projectedStars(controller.currentFloor(), stars)).sum() / 3);
        }
    }
    public boolean openReward(ServerPlayer opener, int tier) {
        if (tier < 1 || tier > displayedTiers || controller.active() || !rewardParticipants.contains(opener.getUUID())) return false;
        var payout = profile.progress.payout(tier);
        if (payout.chestTicks() >= 0) return false;
        var eligible = eligibleForTier(tier);
        if (eligible.isEmpty()) {
            // A replay may improve a wave without crossing a new chest threshold. Clicking
            // its already claimed chest records that improvement, but never rerolls loot.
            var personal = parties.profile(opener.getUUID()).progress;
            if (personal.tierClaimed(controller.currentFloor(), tier)) personal.recordStars(controller.currentFloor(), attemptStars);
            refreshDisplay();
            return false;
        }
        // Each eligible member contributes the chest tier in table rolls.
        // Resolve the roster now: departed clones neither increase drops nor receive credit.
        var bundle = FloorRewards.roll(level, chestPosition(tier), offeredRewards, tier, eligible.size());
        for (ServerPlayer player : eligible)
            parties.profile(player.getUUID()).progress.commitClaim(controller.currentFloor(), tier, attemptStars);
        var personal = parties.profile(opener.getUUID()).progress;
        if (personal.tierClaimed(controller.currentFloor(), tier)) personal.recordStars(controller.currentFloor(), attemptStars);
        payout.begin(bundle);
        refreshDisplay();
        return true;
    }
    private Vec3 chestPosition(int tier) {
        return bounds().center().add(tier == 2 ? -4 : tier == 1 ? 4 : 0, 0, tier == 3 ? 0 : 2);
    }
    public void admit(ServerPlayer player) {
        admitted.add(player.getUUID());
        // Endless permits immediate live-wave reinforcement. Existing mob attributes
        // retain their captured scale; only the next wave recalculates party size.
        if (endless() && controller.active()) participants.add(player.getUUID());
        CompoundTag mark = AbyssPlatform.data(player).getCompound("PlayerPersisted").getCompound(DEFEATED_TAG);
        if (controller.active() && (defeated.contains(player.getUUID())
                || (mark.hasUUID("Run") && mark.getUUID("Run").equals(controller.runId())
                && mark.hasUUID("Host") && mark.getUUID("Host").equals(host())))) {
            defeated.add(player.getUUID()); markDefeated(player); player.setGameMode(GameType.SPECTATOR);
        } else if (!mark.isEmpty()) recover(player, true);
    }
    public void remove(UUID player) {
        ServerPlayer departing = level.getServer().getPlayerList().getPlayer(player);
        if (departing != null) QliphothEncounters.clearPlayer(departing);
        admitted.remove(player); lastSafe.remove(player); rewardParticipants.remove(player);
        var remaining = players();
        if (player.equals(leader) && !remaining.isEmpty()) {
            ServerPlayer next = remaining.get(level.random.nextInt(remaining.size()));
            leader = next.getUUID();
            remaining.forEach(member -> member.sendSystemMessage(Component.translatable(
                    "message.spiral_abyss.host_changed", next.getDisplayName())));
        }
        // A departure can be the final living teammate even while spectators remain online.
        if (controller.active() && (combatants().isEmpty() || endless() && player.equals(host()))) controller.defeated();
    }
    public boolean captureParty() {
        var living = players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
        if (living.stream().noneMatch(this::isLeader) || endless() && living.stream().noneMatch(p -> p.getUUID().equals(host()))) return false;
        participants.clear(); rewardParticipants.clear(); defeated.clear();
        offeredRewards = null; attemptStars = new int[3]; displayedTiers = 0;
        living.forEach(p -> {
            participants.add(p.getUUID());
            rewardParticipants.add(p.getUUID());
        });
        return true;
    }
    public void clearParticipants() {
        participants.clear(); rewardParticipants.clear(); offeredRewards = null;
        attemptStars = new int[3]; displayedTiers = 0;
    }
    public boolean isCombatant(ServerPlayer player) {
        return contains(player) && participants.contains(player.getUUID()) && !defeated.contains(player.getUUID())
                && player.isAlive() && !player.isSpectator();
    }
    /** Forge fires the totem hook at zero health, before vanilla restores the player to one health. */
    public boolean isCombatantUsingTotem(ServerPlayer player) {
        return contains(player) && participants.contains(player.getUUID()) && !defeated.contains(player.getUUID())
                && !player.isRemoved() && !player.hasDisconnected() && !player.isSpectator();
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
            else player.sendSystemMessage(Component.translatable("message.spiral_abyss.spectating"));
        } else recover(player, true);
    }
    private void markDefeated(ServerPlayer player) {
        CompoundTag persisted = AbyssPlatform.data(player).getCompound("PlayerPersisted");
        CompoundTag mark = new CompoundTag();
        mark.putUUID("Host", host()); mark.putUUID("Run", controller.runId());
        persisted.put(DEFEATED_TAG, mark);
        AbyssPlatform.data(player).put("PlayerPersisted", persisted);
    }
    public static boolean hasDefeatMarker(ServerPlayer player) {
        return AbyssPlatform.data(player).getCompound("PlayerPersisted").contains(DEFEATED_TAG);
    }
    public static void restoreSurvival(ServerPlayer player) {
        CompoundTag persisted = AbyssPlatform.data(player).getCompound("PlayerPersisted");
        persisted.remove(DEFEATED_TAG);
        AbyssPlatform.data(player).put("PlayerPersisted", persisted);
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
        controller.bossEnvironments().clear();
        AbyssBlockProtection.clearCombatHazards(level, bounds());
        // Includes boss summons and lingering attacks, so recovery is safe after a team wipe.
        var area = new AABB(bounds().center().x - ArenaBounds.FLIGHT_RADIUS, 0,
                bounds().center().z - ArenaBounds.FLIGHT_RADIUS, bounds().center().x + ArenaBounds.FLIGHT_RADIUS,
                level.getMaxBuildHeight(), bounds().center().z + ArenaBounds.FLIGHT_RADIUS);
        level.getEntities((Entity) null, area, entity -> !(entity instanceof ArenaInteractable)
                && (entity instanceof Mob || entity instanceof Projectile
                || entity instanceof AreaEffectCloud || entity instanceof EvokerFangs))
                .forEach(Entity::discard);
        for (ServerPlayer player : players()) {
            if (defeated.contains(player.getUUID()) || hasDefeatMarker(player)) recover(player, true);
        }
        defeated.clear();
    }

    /** Prepare the next Endless wave after reviving the surviving party's defeated members. */
    public boolean captureEndlessWave() {
        // The boundary refreshes the roster and scale, including live-wave reinforcements.
        finishAttempt();
        return captureParty();
    }
    /** Revive only the captured, still-present teammates during the five-second transition. */
    public void reviveForNextWave() {
        for (ServerPlayer player : players()) if (participants.contains(player.getUUID())
                && defeated.remove(player.getUUID())) recover(player, true);
    }

    public void tick() {
        // Entity persistence loads asynchronously. Never create replacement furniture before that finishes.
        if (!displayReady && furnitureLoaded()) {
            AbyssBlockProtection.clearCombatHazards(level, bounds());
            clearDroppedRewards();
            displayReady = true; refreshDisplay();
        }
        for (ServerPlayer player : players()) {
            if (waitingForParty(player) && !player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
            containPlayer(player);
        }
        if (!displayReady) return;
        controller.tick();
        for (int tier = 1; !endless() && tier <= 3; tier++) {
            var payout = profile.progress.payout(tier);
            if (payout.chestTicks() < 0) continue;
            payout.tickOpening();
            var bundle = payout.pending();
            if (bundle != null && payout.chestTicks() >= LootChestEntity.OPEN_TICKS) {
                int dispensingTicks = payout.chestTicks() - LootChestEntity.OPEN_TICKS;
                if (dispensingTicks % LootChestEntity.DROP_INTERVAL_TICKS == 0) {
                    boolean rare = bundle.rare(payout.releasedItems());
                    var stack = payout.takeNextReward();
                    if (stack != null) FloorRewards.releaseItem(level, chestPosition(tier), stack, rare, controller.currentFloor());
                }
                if (payout.releasedItems() >= bundle.items().size()
                        && dispensingTicks >= (long) bundle.items().size() * LootChestEntity.DROP_INTERVAL_TICKS) {
                    int experience = bundle.experience();
                    payout.complete();
                    FloorRewards.releaseExperience(level, chestPosition(tier), experience);
                    if (!profile.progress.hasPayouts()) players().forEach(player -> player.sendSystemMessage(
                            Component.translatable("message.spiral_abyss.menu_hint", Component.keybind("key.spiral_abyss.menu"))));
                }
            }
            refreshDisplay();
        }
        if (level.getGameTime() >= nextDisplayRefresh || level.getGameTime() % 20 == 0) refreshDisplay();
    }

    public void containPlayer(ServerPlayer player) {
        if (bounds().outsidePlayer(player)) {
            player.stopRiding();
            Vec3 safe = lastSafe.getOrDefault(player.getUUID(), bounds().entrance());
            if (safe.y < controller.bossEnvironments().waterSurface() || !level.noCollision(player, player.getBoundingBox().move(safe.subtract(player.position())))) safe = surfaceDestination(player, bounds().entrance());
            player.teleportTo(level, safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
        } else if (player.onGround()) lastSafe.put(player.getUUID(), player.position());
    }

    public void rescueMob(LivingEntity mob) {
        if (DragonArena.defeated(mob)) return;
        if (mob instanceof Mob ai) ArenaFlight.steer(ai, bounds());
        if (!bounds().rescueMob(mob, MobCatalog.isGhostMob(EntityType.getKey(mob.getType())))) return;
        Vec3 center = surfaceDestination(mob, bounds().center());
        mob.teleportTo(center.x, center.y + (mob instanceof EnderDragon ? 20 : 0.1), center.z);
        if (mob instanceof Mob ai) ArenaFlight.stop(ai);
    }

    private boolean furnitureLoaded() {
        BlockPos center = BlockPos.containing(bounds().center());
        // Side chests can occupy different chunks from the center key.
        // Wait for the same bounded 3x3 region forced by initializeArena so saved
        // furniture is visible to deduplication before any replacements are created.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (!level.areEntitiesLoaded(ChunkPos.asLong(
                    (center.getX() >> 4) + x, (center.getZ() >> 4) + z))) return false;
        return true;
    }

    /** Resolve emergency return positions against live terrain instead of the original Y=65 plane. */
    private Vec3 surfaceDestination(Entity entity, Vec3 destination) {
        double water = controller.bossEnvironments().waterSurface();
        if (water > 0) {
            Vec3 candidate = new Vec3(destination.x, water, destination.z);
            if (level.noCollision(entity, entity.getBoundingBox().move(candidate.subtract(entity.position())))) return candidate;
        }
        for (int y = (int) ArenaBounds.FLOOR_Y - 1; y < ArenaBounds.CEILING_Y; y++) {
            BlockPos pos = BlockPos.containing(destination.x, y, destination.z);
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) continue;
            double feet = y + shape.max(Direction.Axis.Y);
            Vec3 candidate = new Vec3(destination.x, feet, destination.z);
            var box = entity.getBoundingBox().move(candidate.subtract(entity.position()));
            if (feet >= ArenaBounds.FLOOR_Y && box.maxY <= ArenaBounds.CEILING_Y && level.noCollision(entity, box)) return candidate;
        }
        return destination;
    }

    public void keepMobActive(LivingEntity mob) {
        // Also called from the controller's tracked list when a boss teleports beyond player view distance.
        level.getChunkSource().addRegionTicket(MOB_TICKET, mob.chunkPosition(), 2, mob.getUUID());
    }

    public void refreshDisplay() {
        if (!displayReady) return;
        nextDisplayRefresh = Long.MAX_VALUE;
        if (endless()) { refreshEndlessDisplay(); return; }
        List<ArenaInteractable> furniture = level.getEntitiesOfClass(ArenaInteractable.class, bounds().structureArea());
        boolean hide = controller.active();
        int chestMask = (1 << displayedTiers) - 1;
        // Only opened tiers survive a visit. A saved golden payout does not restore
        // the abandoned copper/iron offers from the previous attempt.
        for (int tier = 1; tier <= 3; tier++)
            if (profile.progress.payout(tier).chestTicks() >= 0) chestMask |= 1 << (tier - 1);
        boolean showChests = controller.showsChest() && chestMask != 0;
        KeyEntity key = null;
        Map<Integer, LootChestEntity> chests = new HashMap<>();
        for (ArenaInteractable entity : furniture) {
            if (hide || !host().equals(entity.arenaHost())) { entity.discard(); continue; }
            if (entity instanceof KeyEntity candidate && !showChests && key == null) { key = candidate; continue; }
            if (entity instanceof LootChestEntity candidate && showChests && (chestMask & (1 << (candidate.tier() - 1))) != 0
                    && !chests.containsKey(candidate.tier())) { chests.put(candidate.tier(), candidate); continue; }
            entity.discard();
        }
        if (hide) return;
        if (!showChests && key == null) {
            key = EntityInit.KEY_ENTITY.get().create(level);
            if (key != null) {
                key.setArenaHost(host()); key.moveTo(bounds().center().x, bounds().center().y, bounds().center().z, 0, 0);
                level.addFreshEntity(key);
            }
        }
        if (!showChests) return;
        for (int tier = 1; tier <= 3; tier++) {
            if ((chestMask & (1 << (tier - 1))) == 0) continue;
            LootChestEntity chest = chests.get(tier);
            Vec3 position = chestPosition(tier);
            if (chest == null) {
                chest = (tier == 1 ? EntityInit.COPPER_CHEST.get() : tier == 2 ? EntityInit.IRON_CHEST.get()
                        : EntityInit.GOLDEN_CHEST.get()).create(level);
                if (chest == null) continue;
                chest.setArenaHost(host());
                chest.setPhase(LootChestEntity.SPAWN, level.getGameTime());
                chest.moveTo(position.x, position.y, position.z, 0, 0); level.addFreshEntity(chest);
            } else if (chest.position().distanceToSqr(position) > 0.0001) {
                chest.teleportTo(position.x, position.y, position.z);
            }
            int ticks = profile.progress.payout(tier).chestTicks();
            int phase = ticks >= 0 ? (ticks < LootChestEntity.OPEN_TICKS ? LootChestEntity.OPEN : LootChestEntity.DISPENSING)
                    : !eligibleForTier(tier).isEmpty() ? LootChestEntity.IDLE : LootChestEntity.LOOTED;
            long start = ticks < 0 ? 0 : level.getGameTime() - ticks + (phase == LootChestEntity.DISPENSING ? LootChestEntity.OPEN_TICKS : 0);
            if (ticks >= 0) chest.setRarePayout(profile.progress.payout(tier).pending().hasRareItems());
            if (ticks < 0 && chest.phase() == LootChestEntity.SPAWN
                    && level.getGameTime() - chest.phaseStart() < LootChestEntity.SPAWN_TICKS) {
                nextDisplayRefresh = Math.min(nextDisplayRefresh, chest.phaseStart() + LootChestEntity.SPAWN_TICKS);
                continue;
            }
            if (ticks >= 0 || chest.phase() != phase) chest.setPhase(phase, ticks < 0 ? level.getGameTime() : start);
        }
    }

    private void refreshEndlessDisplay() {
        boolean chest = controller.showsEndlessChest();
        ArenaInteractable kept = null;
        for (ArenaInteractable entity : level.getEntitiesOfClass(ArenaInteractable.class, bounds().structureArea())) {
            boolean wanted = (!controller.active() || chest) && host().equals(entity.arenaHost())
                    && (chest ? entity instanceof ExperienceChestEntity : entity instanceof KeyEntity);
            if (wanted && kept == null) kept = entity; else entity.discard();
        }
        if (controller.active() && !chest) return;
        if (kept == null) {
            kept = chest ? EntityInit.EXPERIENCE_CHEST.get().create(level) : EntityInit.KEY_ENTITY.get().create(level);
            if (kept != null) {
                kept.setArenaHost(host());
                if (kept instanceof ExperienceChestEntity experienceChest)
                    experienceChest.setPhase(LootChestEntity.SPAWN, level.getGameTime());
                kept.moveTo(bounds().center().x, bounds().center().y, bounds().center().z, 0, 0);
                level.addFreshEntity(kept);
            }
        }
        if (kept instanceof ExperienceChestEntity experienceChest) {
            int ticks = profile.endlessPayout.chestTicks();
            int phase = ticks >= 0 ? (ticks < LootChestEntity.OPEN_TICKS ? LootChestEntity.OPEN : LootChestEntity.DISPENSING)
                    : controller.waitingForExperience() ? LootChestEntity.IDLE : LootChestEntity.LOOTED;
            if (ticks < 0 && experienceChest.phase() == LootChestEntity.SPAWN
                    && level.getGameTime() - experienceChest.phaseStart() < LootChestEntity.SPAWN_TICKS) {
                nextDisplayRefresh = experienceChest.phaseStart() + LootChestEntity.SPAWN_TICKS;
                return;
            }
            long start = ticks >= 0 ? level.getGameTime() - ticks
                    + (phase == LootChestEntity.DISPENSING ? LootChestEntity.OPEN_TICKS : 0) : level.getGameTime();
            if (ticks >= 0 || experienceChest.phase() != phase) experienceChest.setPhase(phase, start);
        }
    }

    /** Restore blocks without changing claims, pending rewards, or chest opening progress. */
    public void restoreStructure() {
        controller.bossEnvironments().clear();
        AbyssBlockProtection.clearCombatHazards(level, bounds());
        clearDroppedRewards();
        AbyssDimension.restoreArena(level, profile);
        lastSafe.clear();
        players().forEach(this::returnToEntrance);
    }

    public void resetArena() {
        controller.close();
        clearParticipants();
        restoreStructure();
        level.getEntities((Entity) null, bounds().structureArea(), entity -> entity instanceof ArenaInteractable
                || entity instanceof ItemEntity || entity instanceof ExperienceOrb)
                .forEach(Entity::discard);
        refreshDisplay();
    }

    public void close() {
        controller.close();
        clearParticipants();
        clearDroppedRewards();
        level.getEntitiesOfClass(ArenaInteractable.class, bounds().structureArea()).forEach(Entity::discard);
        AbyssDimension.releaseArena(level, profile);
    }

    /** All floors share the same physical slot; loose loot belongs only to the previous visit. */
    private void clearDroppedRewards() {
        level.getEntities((Entity) null, rewardArea(), entity -> entity instanceof ItemEntity
                || entity instanceof ExperienceOrb).forEach(Entity::discard);
    }
    private AABB rewardArea() {
        Vec3 center = bounds().center();
        return new AABB(center.x - ArenaBounds.FLIGHT_RADIUS, level.getMinBuildHeight(),
                center.z - ArenaBounds.FLIGHT_RADIUS, center.x + ArenaBounds.FLIGHT_RADIUS,
                level.getMaxBuildHeight(), center.z + ArenaBounds.FLIGHT_RADIUS);
    }
}
