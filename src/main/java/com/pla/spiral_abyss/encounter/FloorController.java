package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironments;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.encounter.data.*;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeTracker;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;

/** One encounter per host arena; timing and participants are isolated from other hosts. */
public final class FloorController {
    public enum State { READY, RUNNING, BETWEEN_WAVES, FAILED, COMPLETED }

    private final ServerLevel level;
    private final ArenaSession session;
    private double partyScale = 1;
    private final FloorProgress progress;
    private final Supplier<FloorCatalog.Snapshot> catalog;
    private final WaveSpawner spawner = new WaveSpawner();
    private final BossHealthBars bossHealthBars = new BossHealthBars();
    private final FloorClock clock = new FloorClock();
    private final WaveStages<Mob> stages = new WaveStages<>();
    private final BossEnvironments bossEnvironments;
    private WaveEquipment waveEquipment;
    private net.minecraft.resources.ResourceLocation selectedWaveType;
    private State state;
    private FloorDefinition floor;
    private MobCatalog.Equipment equipment;
    private MobCatalog.ScalingSnapshot mobScaling = MobCatalog.ScalingSnapshot.EMPTY;
    private com.pla.spiral_abyss.config.GearConfig.Floor gear;
    private Map<net.minecraft.resources.ResourceLocation, List<FloorDefinition.RewardEntry>> mobRewards = Map.of();
    private final Set<net.minecraft.resources.ResourceLocation> encounteredMobs = new LinkedHashSet<>();
    private UUID runId;
    private Vec3 center = new Vec3(0.5, 65, 0.5);
    private int waveIndex;
    private int delayTicks;
    private int waveEnemies;
    private final int[] attemptStars = new int[WaveChallenges.WAVE_COUNT];
    private List<Challenge> challenges = List.of();
    private ChallengeTracker challengeTracker;
    private int revision;
    private AbyssNetwork.EncounterStatus readyHud;
    private int readyHudFloor;
    private List<Challenge> readyChallenges = List.of();

    public FloorController(ArenaSession session, Supplier<FloorCatalog.Snapshot> catalog) {
        this.session = session;
        this.level = session.level();
        this.progress = session.profile().progress;
        this.center = session.bounds().center();
        this.catalog = catalog;
        this.bossEnvironments = new BossEnvironments(level, session.bounds(), session.host());
        this.state = progress.pending() != null ? State.COMPLETED : State.READY;
        this.revision = catalog.get().revision();
    }

    public State state() { return state; }
    public BossEnvironments bossEnvironments() { return bossEnvironments; }
    public int currentFloor() { return progress.floor(); }
    public int currentWave() { return waveIndex + 1; }
    public int remainingTicks() { return clock.remainingTicks(); }
    public int remainingEnemies() { return stages.enemies().size(); }
    public List<Mob> enemies() { return stages.enemies(); }
    public ChallengeTracker challengeTracker() { return challengeTracker; }
    public List<ChallengeTracker.Progress> challengeProgress() {
        return challengeTracker == null ? List.of() : challengeTracker.progress();
    }
    public AbyssNetwork.EncounterStatus hudStatus(ServerPlayer viewer) {
        if (active()) {
            var views = challengeProgress().stream().map(p -> new AbyssNetwork.ChallengeView(p.description(), p.progress(), p.target(), p.complete(), p.failed())).toList();
            return new AbyssNetwork.EncounterStatus(true, false, currentFloor(), currentWave(), stages.removed(), waveEnemies,
                    clock.remainingSeconds(), state == State.BETWEEN_WAVES ? WaveChallenges.countdownSeconds(delayTicks) : 0, views,
                    stages.enemies().stream().filter(mob -> session.isCombatant(viewer) && mob.isAlive() && !mob.isRemoved()
                                    && !(mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon && DragonArena.defeated(dragon))
                                    && AbyssRejection.applies(viewer, mob))
                            .map(mob -> net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())).distinct().toList());
        }
        if ((state != State.READY && state != State.FAILED) || showsChest()) return AbyssNetwork.EncounterStatus.HIDDEN;
        if (readyHud == null || readyHudFloor != currentFloor()) {
            readyHudFloor = currentFloor();
            readyHud = prepareHudStatus();
        }
        return readyHudFor(viewer);
    }
    private AbyssNetwork.EncounterStatus readyHudFor(ServerPlayer viewer) {
        if (readyHud == AbyssNetwork.EncounterStatus.HIDDEN) return readyHud;
        List<AbyssNetwork.ChallengeView> views = null;
        for (int index = 0; index < readyChallenges.size(); index++) {
            Challenge challenge = readyChallenges.get(index);
            boolean blocked = switch (challenge.type()) {
                case NO_ARMOR -> challenge.forbiddenSlots().stream()
                        .anyMatch(slot -> !viewer.getItemBySlot(slot).isEmpty());
                case ARMOR_ENCHANTMENT -> !ChallengeTracker.wearsArmorEnchantments(viewer, challenge.ids());
                default -> false;
            };
            if (!blocked) continue;
            if (views == null) views = new ArrayList<>(readyHud.challenges());
            var original = views.get(index);
            // In a preparation snapshot, failed means currently blocked by equipment;
            // the combat tracker has not started and no failure is committed.
            views.set(index, new AbyssNetwork.ChallengeView(original.description(), original.progress(),
                    original.target(), false, true));
        }
        return views == null ? readyHud : new AbyssNetwork.EncounterStatus(true, true,
                readyHud.floor(), readyHud.wave(), 0, 0, 0, 0, views);
    }
    private AbyssNetwork.EncounterStatus prepareHudStatus() {
        readyChallenges = List.of();
        FloorDefinition definition = catalog.get().floors().get(currentFloor());
        if (definition == null || definition.waves().isEmpty()) return AbyssNetwork.EncounterStatus.HIDDEN;
        try {
            List<Challenge> selected = session.challenges(currentFloor());
            if (selected.size() != 3) return AbyssNetwork.EncounterStatus.HIDDEN;
            int population = session.cycleLineup(currentFloor(), 1, definition.waves().get(0)).stream()
                    .mapToInt(FloorDefinition.Enemy::count).sum();
            var views = selected.stream().map(challenge -> new AbyssNetwork.ChallengeView(
                    challenge.description(population), 0,
                    challenge.type() == Challenge.Type.TIME || challenge.type() == Challenge.Type.NO_ARMOR
                            || challenge.type() == Challenge.Type.ARMOR_ENCHANTMENT ? 0 : challenge.requiredCount(population),
                    false, false)).toList();
            readyChallenges = List.copyOf(selected);
            return new AbyssNetwork.EncounterStatus(true, true, currentFloor(), 1, 0, 0, 0, 0, views);
        } catch (IllegalArgumentException ex) {
            LogUtils.getLogger().warn("Cannot preview Abyss floor {} preparation challenges: {}", currentFloor(), ex.getMessage());
            return AbyssNetwork.EncounterStatus.HIDDEN;
        }
    }
    public UUID runId() { return runId; }
    public boolean showsChest() { return state == State.COMPLETED || progress.pending() != null; }

    public boolean start(ServerPlayer player, Vec3 keyPosition) {
        refreshDefinitions();
        if (player.level() != level || !session.contains(player)) return false;
        if (!player.isAlive() || player.isSpectator()) return message(player, "spectator");
        if (active()) return message(player, "already_running");
        if (showsChest() && session.looting()) return message(player, "already_completed");
        if (level.getDifficulty() == Difficulty.PEACEFUL) return message(player, "peaceful");
        if (!session.partyCanEnterFloor(player, progress.floor())) return false;
        floor = catalog.get().floors().get(progress.floor());
        if (floor == null) return message(player, "missing_floor", progress.floor());
        try {
            challenges = List.copyOf(session.challenges(currentFloor()));
            if (challenges.size() != 3) throw new IllegalArgumentException("Expected three challenges");
        } catch (IllegalArgumentException ex) {
            LogUtils.getLogger().warn("Floor {} has unavailable challenges", currentFloor(), ex);
            return message(player, "challenges_unavailable");
        }
        if (!session.captureParty()) return message(player, "host_required");
        equipment = MobCatalog.equipment();
        mobScaling = MobCatalog.scalingSnapshot();
        mobRewards = MobCatalog.rewards();
        encounteredMobs.clear();
        gear = com.pla.spiral_abyss.config.GearConfig.load(currentFloor());
        partyScale = FloorRewards.partyScale(session.participants().size());
        runId = UUID.randomUUID();
        waveIndex = 0;
        delayTicks = 0;
        Arrays.fill(attemptStars, 0);
        return startWave(player);
    }

    public boolean nextFloor(ServerPlayer player) {
        refreshDefinitions();
        if (player.level() != level || !session.contains(player))
            return message(player, "wrong_dimension");
        if (player.isSpectator()) return message(player, "spectator");
        if (!session.isLeader(player)) return message(player, "host_only");
        if (active()) return message(player, "already_running");
        if (!session.canAdvance(player)) return message(player, "claim_first");
        if (progress.floor() >= FloorParser.MAX_FLOORS) return message(player, "last_floor");
        int next = progress.floor() + 1;
        if (!catalog.get().floors().containsKey(next)) return message(player, "missing_floor", next);
        return session.selectFloor(player, next);
    }

    public void tick() {
        refreshDefinitions();
        if (!active()) return;
        ServerPlayer target = session.combatants().stream().findFirst().orElse(null);
        if (target == null) { fail("abandoned"); return; }
        if (level.getDifficulty() == Difficulty.PEACEFUL) { fail("peaceful"); return; }
        if (state == State.BETWEEN_WAVES) {
            if (--delayTicks <= 0) {
                // A lingering attack can defeat a teammate during the countdown.
                session.reviveForNextWave();
                waveIndex++; startWave(target);
            }
            return;
        }
        // Spend every combat tick, including the tick of the final kill. A wave at
        // its five-minute deadline fails even when the final removal is observed now.
        if (clock.tick(true)) { fail("time_up"); return; }
        challengeTracker.update(clock.elapsedTicks());
        // Only the current stage's captured entity identities can advance this encounter.
        try { bossEnvironments.tick(); }
        catch (RuntimeException ex) { LogUtils.getLogger().error("Boss environment failed in floor {} wave {}", currentFloor(), currentWave(), ex); fail("spawn_failed"); return; }
        stages.removeInvalid(mob -> DragonArena.finished(mob, level));
        bossHealthBars.tick(stages.enemies(), session.players());
        for (Mob mob : stages.enemies()) {
            if (DragonArena.defeated(mob)) { session.keepMobActive(mob); continue; }
            session.rescueMob(mob);
            session.keepMobActive(mob);
            if (mob.getTarget() == null || !mob.getTarget().isAlive() || mob.getTarget().isSpectator()
                    || mob.getTarget().level() != level
                    || (mob.getTarget() instanceof ServerPlayer p && !session.isCombatant(p)))
                mob.setTarget(target);
        }
        if (stages.enemies().isEmpty()) {
            // Boss cleanup retracts visibly before any next stage or final-wave payout.
            if (bossEnvironments.cleaning()) return;
            LogUtils.getLogger().debug("Floor {} wave {} type {} stage {}/{} cleared", currentFloor(), currentWave(), selectedWaveType, stages.index() + 1, stages.size());
            if (!stages.complete()) {
                try { spawnStage(target); }
                catch (RuntimeException ex) { LogUtils.getLogger().error("Unable to spawn floor {} wave {} next stage", currentFloor(), currentWave(), ex); fail("spawn_failed"); }
                return;
            }
            LogUtils.getLogger().debug("Wave type {} complete; floor {} wave {} complete", selectedWaveType, currentFloor(), currentWave());
            if (waveIndex < attemptStars.length) attemptStars[waveIndex] = challengeTracker.stars();
            if (waveIndex + 1 == floor.waves().size()) complete();
            else {
                state = State.BETWEEN_WAVES;
                delayTicks = WaveChallenges.TRANSITION_TICKS;
                session.reviveForNextWave();
            }
        }
    }

    private boolean startWave(ServerPlayer target) {
        try {
            var wave = floor.waves().get(waveIndex);
            stages.prepare(session.cycleLineup(currentFloor(), currentWave(), wave));
            selectedWaveType = session.selectedWaveType(currentFloor(), currentWave(), wave);
            waveEquipment = new WaveEquipment(equipment, gear, level.random);
            waveEnemies = stages.total();
            spawnStage(target);
            // Spawning is synchronous: capture starting equipment before the first combat tick.
            challengeTracker = new ChallengeTracker(session, challenges, waveEnemies);
            clock.start(WaveChallenges.TIME_LIMIT_TICKS);
            state = State.RUNNING;
            session.refreshDisplay();
            return true;
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Unable to spawn floor {} wave {}", currentFloor(), currentWave(), ex);
            fail("spawn_failed");
            return false;
        }
    }

    private void spawnStage(ServerPlayer target) {
        stages.advance(composition -> {
            LogUtils.getLogger().debug("Floor {} wave {} type {} stage {}/{} starting: {}", currentFloor(), currentWave(), selectedWaveType, stages.index() + 1, stages.size(), composition);
            var created = spawner.spawn(level, center, target, composition, runId,
                    waveEquipment, currentFloor(), partyScale, mobScaling);
            created.forEach(mob -> encounteredMobs.add(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())));
            created.forEach(mob -> mob.getPersistentData().putUUID("spiral_abyss_host", session.host()));
            return created;
        });
        bossEnvironments.begin(stages.enemies());
    }

    private void complete() {
        // Offer rewards now; personal stars/progress are committed only on a claim.
        session.completeChallenge(attemptStars.clone(), FloorRewards.withMobRewards(floor.rewards(), encounteredMobs, mobRewards));
        session.finishAttempt();
        state = State.COMPLETED;
        clock.clear();
        runId = null;
        session.refreshDisplay();
    }

    private void fail(String reason) {
        cleanup();
        session.finishAttempt();
        state = State.FAILED;
        session.clearParticipants();
        session.refreshDisplay();
        announce(reason);
    }

    private void refreshDefinitions() {
        int nextRevision = catalog.get().revision();
        if (revision == nextRevision) return;
        revision = nextRevision;
        readyHud = null;
        readyChallenges = List.of();
        if (active()) fail("reloaded");
    }

    public boolean owns(Entity entity) {
        return runId != null && stages.enemies().contains(entity) && entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)
                && runId.equals(entity.getPersistentData().getUUID(WaveSpawner.RUN_TAG));
    }

    public void close() {
        cleanup();
        session.finishAttempt();
        state = progress.pending() != null ? State.COMPLETED : State.READY;
        readyHud = null;
        readyChallenges = List.of();
    }

    public void defeated() { if (active()) fail("party_defeated"); }

    private void cleanup() {
        bossHealthBars.clear();
        bossEnvironments.clear();
        stages.clear(Entity::discard);
        selectedWaveType = null;
        waveEquipment = null;
        mobScaling = MobCatalog.ScalingSnapshot.EMPTY;
        encounteredMobs.clear();
        runId = null;
        clock.clear();
        challengeTracker = null;
        delayTicks = waveEnemies = 0;
    }

    public boolean active() { return state == State.RUNNING || state == State.BETWEEN_WAVES; }

    private void announce(String key, Object... args) {
        session.players().forEach(player -> player.displayClientMessage(Component.translatable("message.spiral_abyss." + key, args), false));
    }
    private boolean message(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.spiral_abyss." + key, args), true);
        return false;
    }
}
