package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.data.*;
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
    private final FloorClock clock = new FloorClock();
    private final List<Mob> enemies = new ArrayList<>();
    private State state;
    private FloorDefinition floor;
    private AbyssConfig.Equipment equipment;
    private com.pla.spiral_abyss.config.GearConfig.Floor gear;
    private UUID runId;
    private Vec3 center = new Vec3(0.5, 65, 0.5);
    private int waveIndex;
    private int delayTicks;
    private int waveEnemies;
    private int revision;

    public FloorController(ArenaSession session, Supplier<FloorCatalog.Snapshot> catalog) {
        this.session = session;
        this.level = session.level();
        this.progress = session.profile().progress;
        this.center = session.bounds().center();
        this.catalog = catalog;
        this.state = progress.pending() != null ? State.COMPLETED : State.READY;
        this.revision = catalog.get().revision();
    }

    public State state() { return state; }
    public int currentFloor() { return progress.floor(); }
    public int currentWave() { return waveIndex + 1; }
    public int remainingTicks() { return clock.remainingTicks(); }
    public int remainingEnemies() { return enemies.size(); }
    public List<Mob> enemies() { return List.copyOf(enemies); }
    public AbyssNetwork.EncounterStatus hudStatus() {
        return active() ? new AbyssNetwork.EncounterStatus(true, currentFloor(), currentWave(),
                waveEnemies - enemies.size(), waveEnemies, clock.remainingSeconds()) : AbyssNetwork.EncounterStatus.HIDDEN;
    }
    public UUID runId() { return runId; }
    public boolean showsChest() { return state == State.COMPLETED || progress.pending() != null; }

    public boolean start(ServerPlayer player, Vec3 keyPosition) {
        refreshDefinitions();
        if (player.level() != level || !session.contains(player)) return false;
        if (!player.isAlive() || player.isSpectator()) return message(player, "spectator");
        if (active()) return message(player, "already_running");
        if (showsChest()) return message(player, "already_completed");
        if (level.getDifficulty() == Difficulty.PEACEFUL) return message(player, "peaceful");
        if (!session.partyCanEnterFloor(player, progress.floor())) return false;
        floor = catalog.get().floors().get(progress.floor());
        if (floor == null) return message(player, "missing_floor", progress.floor());
        if (!session.captureParty()) return message(player, "host_required");
        equipment = AbyssConfig.equipment();
        gear = com.pla.spiral_abyss.config.GearConfig.load(currentFloor());
        partyScale = FloorRewards.partyScale(session.participants().size());
        runId = UUID.randomUUID();
        waveIndex = 0;
        clock.start(floor.timeTicks());
        return startWave(player);
    }

    public boolean nextFloor(ServerPlayer player) {
        refreshDefinitions();
        if (player.level() != level || !session.contains(player))
            return message(player, "wrong_dimension");
        if (player.isSpectator()) return message(player, "spectator");
        if (!session.host().equals(player.getUUID())) return message(player, "host_only");
        if (active()) return message(player, "already_running");
        if (!progress.claimed() || progress.pending() != null) return message(player, "claim_first");
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
            clock.tick(false);
            if (--delayTicks <= 0) { waveIndex++; startWave(target); }
            return;
        }
        for (Iterator<Mob> iterator = enemies.iterator(); iterator.hasNext();) {
            Mob mob = iterator.next();
            // Count every death/removal, regardless of attacker. Exploding creepers discard
            // themselves without reducing their health or using RemovalReason.KILLED.
            // Also stop tracking entities that leave this level or disappear from its entity map.
            if (mob.isDeadOrDying() || mob.isRemoved() || !mob.isAlive()
                    || mob.level() != level || level.getEntity(mob.getUUID()) != mob) {
                iterator.remove();
                continue;
            }
            session.rescueMob(mob);
            session.keepMobActive(mob);
            if (mob.getTarget() == null || !mob.getTarget().isAlive() || mob.getTarget().isSpectator()
                    || mob.getTarget().level() != level
                    || (mob.getTarget() instanceof ServerPlayer p && !session.isCombatant(p)))
                mob.setTarget(target);
        }
        if (enemies.isEmpty()) {
            if (waveIndex + 1 == floor.waves().size()) complete();
            else {
                state = State.BETWEEN_WAVES;
                delayTicks = floor.waveDelayTicks();
                if (delayTicks == 0) { waveIndex++; startWave(target); }
            }
        } else if (clock.tick(true)) fail("time_up");
    }

    private boolean startWave(ServerPlayer target) {
        try {
            var wave = floor.waves().get(waveIndex);
            var scaled = new FloorDefinition.Wave(wave.healthScale() * partyScale,
                    wave.damageScale() * partyScale, wave.categories(), wave.bosses());
            enemies.addAll(spawner.spawn(level, center, target, scaled, session.cycleLineup(currentFloor(), currentWave(), scaled), runId,
                    new WaveEquipment(equipment, gear, level.random)));
            enemies.forEach(mob -> mob.getPersistentData().putUUID("spiral_abyss_host", session.host()));
            waveEnemies = enemies.size();
            state = State.RUNNING;
            session.refreshDisplay();
            return true;
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Unable to spawn floor {} wave {}", currentFloor(), currentWave(), ex);
            fail("spawn_failed");
            return false;
        }
    }

    private void complete() {
        // Commit completion before generating rewards so repeated ticks/clicks cannot pay twice.
        Set<UUID> eligible = session.eligibleRewards();
        boolean replay = eligible.isEmpty();
        if (!replay) progress.victory(FloorRewards.roll(level, center, floor.rewards(), FloorRewards.partyScale(eligible.size())), eligible);
        session.finishAttempt();
        state = State.COMPLETED;
        clock.clear();
        runId = null;
        session.refreshDisplay();
        announce(replay ? "replay_completed" : "completed", currentFloor());
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
        if (active()) fail("reloaded");
    }

    public boolean owns(Entity entity) {
        return runId != null && entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)
                && runId.equals(entity.getPersistentData().getUUID(WaveSpawner.RUN_TAG));
    }

    public void close() {
        cleanup();
        session.finishAttempt();
        state = progress.pending() != null ? State.COMPLETED : State.READY;
    }

    public void defeated() { if (active()) fail("party_defeated"); }

    private void cleanup() {
        enemies.forEach(Entity::discard);
        enemies.clear();
        runId = null;
        clock.clear();
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
