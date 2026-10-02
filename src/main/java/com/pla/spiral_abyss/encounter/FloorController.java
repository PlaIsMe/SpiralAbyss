package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.*;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
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
    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final List<Mob> enemies = new ArrayList<>();
    private State state;
    private FloorDefinition floor;
    private UUID runId;
    private Vec3 center = new Vec3(0.5, 65, 0.5);
    private int waveIndex;
    private int remainingTicks;
    private int revision;

    public FloorController(ArenaSession session, Supplier<FloorCatalog.Snapshot> catalog) {
        this.session = session;
        this.level = session.level();
        this.progress = session.profile().progress;
        this.center = session.bounds().center();
        this.catalog = catalog;
        this.state = progress.completed() ? State.COMPLETED : State.READY;
        this.revision = catalog.get().revision();
        bar.setVisible(false);
    }

    public State state() { return state; }
    public int currentFloor() { return progress.floor(); }
    public int currentWave() { return waveIndex + 1; }
    public int remainingTicks() { return remainingTicks; }
    public int remainingEnemies() { return enemies.size(); }
    public List<Mob> enemies() { return List.copyOf(enemies); }
    public Component barTitle() { return bar.getName(); }

    public boolean start(ServerPlayer player, Vec3 keyPosition) {
        refreshDefinitions();
        if (player.level() != level || !session.contains(player)) return false;
        if (!player.isAlive() || player.isSpectator()) return message(player, "spectator");
        if (active()) return message(player, "already_running");
        if (progress.completed()) return message(player, "already_completed");
        if (level.getDifficulty() == Difficulty.PEACEFUL) return message(player, "peaceful");
        floor = catalog.get().floors().get(progress.floor());
        if (floor == null) return message(player, "missing_floor", progress.floor());
        if (!session.captureParty()) return message(player, "host_required");
        partyScale = FloorRewards.partyScale(session.participants().size());
        runId = UUID.randomUUID();
        waveIndex = 0;
        return startWave(player);
    }

    public boolean nextFloor(ServerPlayer player) {
        refreshDefinitions();
        if (player.level() != level || !session.contains(player))
            return message(player, "wrong_dimension");
        if (player.isSpectator()) return message(player, "spectator");
        if (!session.host().equals(player.getUUID())) return message(player, "host_only");
        if (!progress.claimed()) return message(player, "claim_first");
        if (progress.floor() >= FloorParser.MAX_FLOORS) return message(player, "last_floor");
        int next = progress.floor() + 1;
        if (!catalog.get().floors().containsKey(next)) return message(player, "missing_floor", next);
        progress.advance();
        session.clearParticipants();
        state = State.READY;
        floor = null;
        waveIndex = 0;
        remainingTicks = 0;
        runId = null;
        hideBar();
        session.refreshDisplay();
        announce("next_floor", next);
        return true;
    }

    public void tick() {
        refreshDefinitions();
        if (!active()) return;
        ServerPlayer target = session.players().stream().filter(p -> p.isAlive() && !p.isSpectator()).findFirst().orElse(null);
        if (target == null) { fail("abandoned"); return; }
        if (level.getDifficulty() == Difficulty.PEACEFUL) { fail("peaceful"); return; }
        if (state == State.BETWEEN_WAVES) {
            if (--remainingTicks <= 0) startWave(target);
            updateBar();
            return;
        }
        for (Iterator<Mob> iterator = enemies.iterator(); iterator.hasNext();) {
            Mob mob = iterator.next();
            // A chunk unload, discard, or dimension transfer is never credited as a kill.
            if (mob.isDeadOrDying() || mob.getRemovalReason() == Entity.RemovalReason.KILLED) {
                iterator.remove();
                continue;
            }
            if (mob.isRemoved() || mob.level() != level) { fail("missing_enemy"); return; }
            session.rescueMob(mob);
            session.keepMobActive(mob);
            if (mob.getTarget() == null || !mob.getTarget().isAlive() || mob.getTarget().isSpectator()
                    || mob.getTarget().level() != level
                    || (mob.getTarget() instanceof ServerPlayer p && !session.contains(p)))
                mob.setTarget(target);
        }
        if (enemies.isEmpty()) {
            if (waveIndex + 1 == floor.waves().size()) complete();
            else {
                waveIndex++;
                state = State.BETWEEN_WAVES;
                remainingTicks = floor.waveDelayTicks();
                if (remainingTicks == 0) startWave(target);
            }
        } else if (--remainingTicks <= 0) fail("time_up");
        updateBar();
    }

    private boolean startWave(ServerPlayer target) {
        try {
            var wave = floor.waves().get(waveIndex);
            var scaled = new FloorDefinition.Wave(wave.timeTicks(), wave.healthScale() * partyScale,
                    wave.damageScale() * partyScale, wave.bosses());
            enemies.addAll(spawner.spawn(level, center, target, scaled, runId));
            enemies.forEach(mob -> mob.getPersistentData().putUUID("spiral_abyss_host", session.host()));
            remainingTicks = floor.waves().get(waveIndex).timeTicks();
            state = State.RUNNING;
            session.refreshDisplay();
            updateBar();
            return true;
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Unable to spawn floor {} wave {}", currentFloor(), currentWave(), ex);
            fail("spawn_failed");
            return false;
        }
    }

    private void complete() {
        // Commit completion before generating rewards so repeated ticks/clicks cannot pay twice.
        progress.victory(FloorRewards.roll(level, center, floor.rewards(), partyScale), session.participants());
        state = State.COMPLETED;
        remainingTicks = 0;
        runId = null;
        hideBar();
        session.refreshDisplay();
        announce("completed", currentFloor());
    }

    private void fail(String reason) {
        cleanup();
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
        state = progress.completed() ? State.COMPLETED : State.READY;
    }

    private void cleanup() {
        enemies.forEach(Entity::discard);
        enemies.clear();
        runId = null;
        remainingTicks = 0;
        hideBar();
    }

    public boolean active() { return state == State.RUNNING || state == State.BETWEEN_WAVES; }

    private void updateBar() {
        if (!active()) { hideBar(); return; }
        for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
            if (!session.contains(player) || !player.isAlive()) bar.removePlayer(player);
        }
        session.players().stream().filter(ServerPlayer::isAlive).forEach(bar::addPlayer);
        int seconds = (remainingTicks + 19) / 20;
        bar.setName(Component.translatable(state == State.RUNNING ? "bar.spiral_abyss.wave" : "bar.spiral_abyss.between",
                currentFloor(), currentWave(), floor.waves().size(), enemies.size(), seconds));
        int total = state == State.RUNNING ? floor.waves().get(waveIndex).timeTicks() : floor.waveDelayTicks();
        bar.setProgress(Math.max(0, Math.min(1, (float) remainingTicks / Math.max(1, total))));
        bar.setColor(state == State.RUNNING ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE);
        bar.setVisible(true);
    }

    private void hideBar() { bar.setVisible(false); bar.removeAllPlayers(); }
    private void announce(String key, Object... args) {
        session.players().forEach(player -> player.displayClientMessage(Component.translatable("message.spiral_abyss." + key, args), false));
    }
    private boolean message(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.spiral_abyss." + key, args), true);
        return false;
    }
}
