package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.entity.*;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-thread portal party management. Floor changes reuse a slot; empty parties release it. */
public final class AbyssParties {
    public static final int DIMENSION_SIZE = 2304;
    private static final String RETURN = "spiral_abyss_return", VISIT = "spiral_abyss_visit_host", VISIT_FLOOR = "spiral_abyss_visit_floor",
            RETURN_REQUIRED = "spiral_abyss_return_required";
    private static final Map<MinecraftServer, AbyssParties> SERVERS = new IdentityHashMap<>();
    private final MinecraftServer server;
    private final AbyssPlayers data;
    private final Map<UUID, ArenaSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, UUID> membership = new HashMap<>();
    private final Map<ServerPlayer, AbyssNetwork.EncounterStatus> hudStates = new IdentityHashMap<>();
    private final PortalParties portals;
    private AbyssParties(MinecraftServer server) { this.server = server; data = AbyssPlayers.get(server); portals = new PortalParties(this, server); }
    public PortalParties portals() { return portals; }
    public AbyssCycle cycle() {
        if (!data.cycle().initialized()) {
            data.cycle().start(System.currentTimeMillis(), java.util.concurrent.ThreadLocalRandom.current().nextLong(), MobCatalog.snapshot());
            data.setDirty();
        }
        return data.cycle();
    }
    public long resetRemainingMillis() { return cycle().remainingMillis(System.currentTimeMillis(), AbyssConfig.RESET_DAYS.get()); }
    public void prepareCycle() {
        var cycle = cycle();
        var fallback = MobCatalog.snapshot();
        cycle.repair(MobCatalog::availableMob, fallback);
        for (var floor : com.pla.spiral_abyss.encounter.data.FloorCatalog.snapshot().floors().values()) {
            try { challenges(floor.number()); }
            catch (IllegalArgumentException ex) { com.mojang.logging.LogUtils.getLogger().warn("Abyss floor {} challenges unavailable: {}", floor.number(), ex.getMessage()); }
            for (int wave = 0; wave < floor.waves().size(); wave++) {
                try {
                    var lineup = cycle.lineup(floor.number(), wave + 1, floor.waves().get(wave), MobCatalog::availableMob, fallback);
                    if (lineup.stream().anyMatch(enemy -> !MobCatalog.availableMob(enemy.id())))
                        throw new IllegalArgumentException("No available replacement for a saved mob; update its category pool");
                }
                catch (IllegalArgumentException ex) { com.mojang.logging.LogUtils.getLogger().warn("Abyss cycle floor {} wave {} unavailable: {}", floor.number(), wave + 1, ex.getMessage()); }
            }
        }
        data.setDirty();
    }
    public List<com.pla.spiral_abyss.encounter.data.FloorDefinition.Enemy> cycleLineup(int floor, int wave,
            com.pla.spiral_abyss.encounter.data.FloorDefinition.Wave definition) {
        var result = cycle().lineup(floor, wave, definition, MobCatalog::availableMob, MobCatalog.snapshot());
        data.setDirty();
        if (result.stream().anyMatch(enemy -> !MobCatalog.availableMob(enemy.id())))
            throw new IllegalArgumentException("No available replacement for a saved cycle mob; install a mob or update its category pool");
        return result;
    }
    public List<com.pla.spiral_abyss.encounter.challenge.Challenge> challenges(int floor) {
        var level = server.getLevel(AbyssDimension.LEVEL);
        if (level == null) throw new IllegalArgumentException("Missing Abyss level");
        var definition = com.pla.spiral_abyss.encounter.data.FloorCatalog.snapshot().floors().get(floor);
        if (definition == null) throw new IllegalArgumentException("Missing challenge floor " + floor);
        List<List<com.pla.spiral_abyss.encounter.data.FloorDefinition.Enemy>> rosters = new ArrayList<>();
        for (int wave = 0; wave < definition.waves().size(); wave++)
            rosters.add(cycleLineup(floor, wave + 1, definition.waves().get(wave)));
        var context = new com.pla.spiral_abyss.encounter.challenge.ChallengePlan.Context(
                rosters.stream().map(roster -> roster.stream().mapToInt(com.pla.spiral_abyss.encounter.data.FloorDefinition.Enemy::count).sum()).toList(),
                new com.pla.spiral_abyss.encounter.challenge.ChallengeEligibility(level, rosters));
        var result = cycle().challenges(definition, context, com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog.snapshot());
        data.setDirty();
        return result;
    }
    public void resetGlobal() { resetGlobal(System.currentTimeMillis()); }
    private void resetGlobal(long cycleStart) {
        // Stop combat and payout before clearing every profile, including offline players.
        server.getPlayerList().getPlayers().forEach(player -> profile(player.getUUID()));
        for (ArenaSession session : List.copyOf(sessions.values())) retire(session);
        portals.invalidateAll();
        data.resetAllProgress();
        cycle().start(cycleStart, java.util.concurrent.ThreadLocalRandom.current().nextLong(), MobCatalog.snapshot());
        prepareCycle();
        data.setDirty();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AbyssNetwork.closeFloors(player);
            AbyssNetwork.encounterStatus(player, AbyssNetwork.EncounterStatus.HIDDEN);
            returnAfterCycleReset(player);
            player.sendSystemMessage(Component.translatable("message.spiral_abyss.global_reset"));
        }
        hudStates.clear();
    }
    private boolean returnAfterCycleReset(ServerPlayer player) {
        var profile = profile(player.getUUID());
        if (!profile.cycleReturnRequired) return true;
        if (player.level().dimension().equals(AbyssDimension.LEVEL) && !leave(player, true)) return false;
        CompoundTag mark = persisted(player);
        mark.remove(VISIT); mark.remove(VISIT_FLOOR); mark.remove(RETURN);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, mark);
        if (ArenaSession.hasDefeatMarker(player)) ArenaSession.restoreSurvival(player);
        profile.cycleReturnRequired = false; data.setDirty();
        return true;
    }
    public ArenaSession hostedBy(UUID host) { return sessions.get(host); }
    public AbyssPlayers.Profile profile(UUID player) { return data.profile(player); }
    public void bindPortal(UUID host, UUID portal) { data.profile(host).portal = portal; data.setDirty(); }
    public boolean checkUnlocked(ServerPlayer recipient, ServerPlayer player, int floor) {
        if (data.profile(player.getUUID()).progress.unlocked(floor)) return true;
        recipient.sendSystemMessage(Component.translatable("message.spiral_abyss.floor_locked_player", player.getDisplayName(), floor));
        return false;
    }
    public static AbyssParties get(MinecraftServer server) { return SERVERS.computeIfAbsent(server, AbyssParties::new); }
    public static void disconnect(ServerPlayer player) {
        // Shutdown closes the service before final logout notifications; do not recreate it.
        AbyssParties parties = SERVERS.get(player.server);
        if (parties != null) parties.loggedOut(player);
    }
    public static void stop(MinecraftServer server) {
        AbyssParties parties = SERVERS.remove(server);
        if (parties != null) new ArrayList<>(parties.sessions.values()).forEach(parties::retire);
    }
    public Collection<ArenaSession> sessions() { return List.copyOf(sessions.values()); }
    public ArenaSession sessionFor(ServerPlayer player) {
        ArenaSession session = sessions.get(membership.get(player.getUUID()));
        return session != null && session.contains(player) ? session : null;
    }
    public UUID hostOf(Entity entity) {
        if (entity.getPersistentData().hasUUID("spiral_abyss_host")) return entity.getPersistentData().getUUID("spiral_abyss_host");
        if (entity instanceof Projectile projectile && projectile.getOwner() != null) return hostOf(projectile.getOwner());
        if (entity instanceof ServerPlayer player) {
            ArenaSession session = sessionFor(player);
            return session == null ? null : session.host();
        }
        ArenaSession session = nearest(entity.position());
        return session == null ? null : session.host();
    }
    public static void configure(ServerLevel level) {
        level.setRainLevel(0); level.setThunderLevel(0);
        maintainBorder(level);
        level.getChunkSource().setViewDistance(6);
        level.getChunkSource().setSimulationDistance(4);
        // Tickets from a terminated server or the old shared arena must not load idle slots forever.
        for (long packed : level.getForcedChunks().toLongArray()) {
            ChunkPos chunk = new ChunkPos(packed);
            level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
    private static boolean maintainBorder(ServerLevel level) {
        var border = level.getWorldBorder();
        if (border.getCenterX() == 0.5 && border.getCenterZ() == 0.5 && border.getSize() == DIMENSION_SIZE
                && border.getLerpTarget() == DIMENSION_SIZE) return false;
        border.setCenter(0.5, 0.5); border.setSize(DIMENSION_SIZE);
        return true;
    }
    private ArenaSession acquire(UUID host) {
        ArenaSession existing = sessions.get(host);
        if (existing != null) return existing;
        Set<Integer> occupied = new HashSet<>();
        sessions.values().forEach(s -> occupied.add(s.profile().slot));
        int slot = 0;
        while (occupied.contains(slot)) slot++;
        if (slot >= AbyssPlayers.ARENA_SLOTS) return null;
        ServerLevel level = server.getLevel(AbyssDimension.LEVEL);
        if (level == null) return null;
        AbyssPlayers.Profile profile = data.profile(host);
        profile.slot = slot;
        ArenaSession session = new ArenaSession(this, level, profile);
        sessions.put(host, session);
        return session;
    }
    public boolean enterOwn(ServerPlayer player) {
        var progress = data.profile(player.getUUID()).progress;
        int floor = progress.pendingFloor() != 0 ? progress.pendingFloor() : progress.floor();
        if (!progress.unlocked(floor)) floor = 1;
        return enterFloor(player, floor);
    }
    public boolean enterPortalGuest(ServerPlayer player, UUID host, Vec3 returnPosition) {
        ArenaSession session = sessions.get(host);
        if (session == null) return false;
        if (host.equals(player.getUUID())) {
            if (!enter(player, host)) return false;
            follow(player.getUUID(), null);
        } else {
            int floor = GuestFloorRules.portalDestination(profile(player.getUUID()).progress, session.controller().currentFloor());
            if (!travelFollowing(player, host, floor, true)) return false;
        }
        rememberPortalReturn(player, returnPosition);
        return true;
    }
    public void rememberPortalReturn(ServerPlayer player, Vec3 returnPosition) {
        CompoundTag persisted = persisted(player);
        CompoundTag origin = persisted.getCompound(RETURN);
        origin.putDouble("X", returnPosition.x); origin.putDouble("Y", returnPosition.y); origin.putDouble("Z", returnPosition.z);
        persisted.put(RETURN, origin); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.setPortalCooldown();
    }
    public boolean enterFloor(ServerPlayer player, int number) {
        ArenaSession current = sessionFor(player);
        if (current != null && current.isLeader(player) && !current.host().equals(player.getUUID()))
            return enterLedFloor(player, current, number);
        if (profile(player.getUUID()).followingHost != null) return followFloor(player, number);
        return enterOwnFloor(player, number);
    }
    private boolean enterLedFloor(ServerPlayer player, ArenaSession session, int number) {
        if (session.controller().active()) return tell(player, "already_running");
        if (!session.canNavigate(player)) return tell(player, "claim_first");
        // The completed attempt keeps its original saved bundle. Future attempts belong
        // to the successor's profile, so an absent original owner earns no free unlocks.
        if (!enterOwnFloor(player, number)) return false;
        follow(player.getUUID(), null);
        return true;
    }
    private boolean enterOwnFloor(ServerPlayer player, int number) {
        if (!com.pla.spiral_abyss.encounter.data.FloorCatalog.snapshot().floors().containsKey(number)) return tell(player, "floor_unavailable");
        if (!checkUnlocked(player, player, number)) return false;
        ArenaSession current = sessionFor(player);
        var progress = data.profile(player.getUUID()).progress;
        if (current == null && progress.pendingFloor() != 0 && number != progress.pendingFloor())
            return tell(player, "claim_first");
        if (current != null && !current.canNavigate(player)) return tell(player, "claim_first");
        ArenaSession own = sessions.get(player.getUUID());
        if (own != null && !own.isLeader(player)) return tell(player, "host_only");
        if ((current != null && current.controller().active()) || (own != null && own.controller().active())) return tell(player, "already_running");
        boolean acknowledged = current != null && current.canNavigate(player);
        if (!progress.canSelectFloor(number, acknowledged)) return tell(player, "claim_first");
        if (own != null && progress.floor() != number && !separateGuests(player, own)) return false;
        // Travel/admission must succeed before changing saved progression.
        if (!enter(player, player.getUUID())) return false;
        ArenaSession session = sessionFor(player);
        // Rebuilding this bounded slot clears loose drops; floor-bound opened cursors remain saved.
        progress.selectFloor(number, acknowledged);
        CompoundTag mark = persisted(player);
        mark.putInt(VISIT_FLOOR, number); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, mark);
        session.controller().close(); session.clearParticipants();
        // A new lease was restored by its constructor. Existing sessions need the same repair
        // when the host changes/retries a floor; guest admission must never rebuild a live fight.
        if (own != null) session.restoreStructure();
        session.refreshDisplay();
        return true;
    }
    private boolean enter(ServerPlayer player, UUID host) {
        if (!returnAfterCycleReset(player)) return false;
        if (!canTravel(player)) return tell(player, "travel_unavailable");
        ArenaSession source = sessionFor(player);
        if (source != null && !source.host().equals(host) && source.controller().active()) return tell(player, "already_running");
        ArenaSession session = acquire(host);
        if (session == null) return tell(player, "arenas_full");
        if (!host.equals(player.getUUID()) && !checkUnlocked(player, player, session.controller().currentFloor())) return false;
        if (!session.canAdmit(player.getUUID())) return tell(player, "party_unavailable");
        CompoundTag persisted = persisted(player);
        if (!player.level().dimension().equals(AbyssDimension.LEVEL)) {
            CompoundTag origin = new CompoundTag();
            origin.putString("Dimension", player.level().dimension().location().toString());
            origin.putDouble("X", player.getX()); origin.putDouble("Y", player.getY()); origin.putDouble("Z", player.getZ());
            origin.putFloat("Yaw", player.getYRot()); origin.putFloat("Pitch", player.getXRot());
            persisted.put(RETURN, origin);
        }
        Vec3 destination = session.bounds().entrance();
        ArenaSession previous = sessionFor(player);
        if (player.level() == session.level()) player.teleportTo(session.level(), destination.x, destination.y, destination.z, 180, 0);
        else if (player.changeDimension(session.level(), new AbyssTeleporter(destination, 180, 0)) == null) {
            if (session.admitted().isEmpty()) retire(session);
            return false;
        }
        if (previous != null && previous != session) previous.remove(player.getUUID());
        membership.put(player.getUUID(), host);
        persisted.putUUID(VISIT, host); persisted.putInt(VISIT_FLOOR, session.controller().currentFloor());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        session.admit(player);
        if (host.equals(player.getUUID()) && !session.controller().active() && session.profile().progress.claimed()) {
            session.controller().close(); session.refreshDisplay();
        }
        // Vanilla initializes clients with the overworld border, even after changing dimensions.
        player.connection.send(new ClientboundInitializeBorderPacket(session.level().getWorldBorder()));
        player.fallDistance = 0; player.setDeltaMovement(Vec3.ZERO);
        return true;
    }
    public boolean leave(ServerPlayer player) {
        return leave(player, false);
    }
    private boolean leave(ServerPlayer player, boolean forced) {
        if (forced) {
            player.stopRiding(); player.ejectPassengers();
            if (player.isSleeping()) player.stopSleepInBed(true, true);
        } else if (!canTravel(player)) return tell(player, "travel_unavailable");
        if (!player.level().dimension().equals(AbyssDimension.LEVEL)) return tell(player, "wrong_dimension");
        CompoundTag persisted = persisted(player), saved = persisted.getCompound(RETURN);
        ResourceLocation id = ResourceLocation.tryParse(saved.getString("Dimension"));
        ServerLevel destination = server.overworld();
        Vec3 position = new Vec3(saved.getDouble("X"), saved.getDouble("Y"), saved.getDouble("Z"));
        Vec3 safe = id != null && id.equals(destination.dimension().location())
                ? safeReturnPosition(destination, player, position) : null;
        position = safe != null ? safe : Vec3.atBottomCenterOf(destination.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destination.getSharedSpawnPos()));
        ArenaSession session = sessionFor(player);
        if (forced) {
            player.teleportTo(destination, position.x, position.y, position.z, saved.getFloat("Yaw"), saved.getFloat("Pitch"));
            if (player.level() != destination) return false;
        } else if (player.changeDimension(destination, new AbyssTeleporter(position, saved.getFloat("Yaw"), saved.getFloat("Pitch"))) == null) return false;
        if (session != null) session.remove(player.getUUID());
        if (session != null && session.players().isEmpty()) retire(session);
        membership.remove(player.getUUID()); persisted.remove(VISIT); persisted.remove(VISIT_FLOOR); persisted.remove(RETURN); persisted.remove(RETURN_REQUIRED);
        follow(player.getUUID(), null);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted); player.fallDistance = 0;
        if (ArenaSession.hasDefeatMarker(player)) ArenaSession.restoreSurvival(player);
        player.setPortalCooldown();
        return true;
    }
    public boolean startKey(ServerPlayer player, KeyEntity key) {
        ArenaSession session = sessionFor(player);
        if (validInteraction(player, key, session) && player.isShiftKeyDown()) return portals.openArenaMenu(player);
        if (!validInteraction(player, key, session) || !session.controller().start(player, key.position())) return false;
        if (session.host().equals(player.getUUID()) && followedHost(player) != null) {
            profile(player.getUUID()).personalRun = true; data.setDirty();
        }
        return true;
    }
    public boolean openScroll(ServerPlayer player, ScrollEntity scroll) {
        ArenaSession session = sessionFor(player);
        return validInteraction(player, scroll, session) && !session.controller().active() && portals.openArenaMenu(player);
    }
    public UUID followedHost(ServerPlayer player) {
        ArenaSession session = sessionFor(player);
        if (session != null && session.isLeader(player) && !session.host().equals(player.getUUID())) return null;
        if (session != null && !session.host().equals(player.getUUID())) return session.host();
        return profile(player.getUUID()).followingHost;
    }
    private void follow(UUID player, UUID host) {
        profile(player).followingHost = host;
        if (host == null) profile(player).personalRun = false;
        data.setDirty();
    }
    public int guestDestination(ServerPlayer player) {
        UUID host = followedHost(player);
        ArenaSession current = sessionFor(player);
        if (host == null || current == null || current.controller().active()) return 0;
        return GuestFloorRules.next(profile(player.getUUID()).progress, current.controller().currentFloor(), current.canNavigate(player));
    }
    public boolean followFloor(ServerPlayer player, int number) {
        UUID host = followedHost(player);
        if (host == null) return tell(player, "selection_expired");
        ArenaSession current = sessionFor(player);
        if (current == null || current.controller().active()) return tell(player, "already_running");
        if (!current.canNavigate(player) || !profile(player.getUUID()).progress.canSelectFloor(number, true))
            return tell(player, "claim_first");
        return travelFollowing(player, host, number, false);
    }
    private boolean travelFollowing(ServerPlayer player, UUID host, int number, boolean portalEntry) {
        if (player.getUUID().equals(host)) return false;
        var progress = profile(player.getUUID()).progress;
        ArenaSession current = sessionFor(player);
        if (!progress.canSelectFloor(number, !portalEntry && current != null && current.canNavigate(player)))
            return tell(player, "claim_first");
        ArenaSession destination = hostedBy(host);
        boolean join = destination != null && GuestFloorRules.joinHost(portalEntry, profile(player.getUUID()).personalRun,
                number, destination.controller().currentFloor(), destination.controller().active(),
                progress.pendingFloor() != 0 || destination.looting());
        boolean entered = join ? enter(player, host) : enterOwnFloor(player, number);
        if (entered) {
            follow(player.getUUID(), host);
            profile(player.getUUID()).personalRun = !join; data.setDirty();
            if (!join) player.sendSystemMessage(Component.translatable("message.spiral_abyss.personal_session", number));
        }
        return entered;
    }

    /** Guests keep the old floor in personal arenas; no floor change silently moves the combat roster. */
    private boolean separateGuests(ServerPlayer requester, ArenaSession session) {
        if (!canTravel(requester)) return tell(requester, "travel_unavailable");
        int oldFloor = session.controller().currentFloor();
        var guests = session.players().stream().filter(p -> !session.isLeader(p)).toList();
        long required = guests.stream().filter(p -> hostedBy(p.getUUID()) == null).count();
        if (sessions.size() + required > AbyssPlayers.ARENA_SLOTS) return tell(requester, "arenas_full");
        for (ServerPlayer guest : guests) {
            ArenaSession own = hostedBy(guest.getUUID());
            if (!canTravel(guest) || !profile(guest.getUUID()).progress.canSelectFloor(oldFloor)
                    || (own != null && (!own.players().isEmpty() || own.controller().active())))
                return tell(requester, "guest_transfer_blocked", guest.getDisplayName());
        }
        for (ServerPlayer guest : guests) {
            var personal = profile(guest.getUUID()).progress;
            personal.selectFloor(oldFloor);
            if (!enter(guest, guest.getUUID())) throw new IllegalStateException("Prevalidated guest transfer failed");
            follow(guest.getUUID(), session.host());
            // Waiting on the previous floor can still follow the host before its next fight starts.
            profile(guest.getUUID()).personalRun = false;
            ArenaSession own = sessionFor(guest);
            own.controller().close(); own.refreshDisplay();
            guest.sendSystemMessage(Component.translatable("message.spiral_abyss.host_moved"));
        }
        return true;
    }
    public boolean openChest(ServerPlayer player, LootChestEntity chest) {
        ArenaSession session = sessionFor(player);
        if (!validInteraction(player, chest, session)) return false;
        if (player.isShiftKeyDown()) return portals.openArenaMenu(player);
        if (chest.phase() == LootChestEntity.SPAWN) return false;
        return session.openReward(player, chest.tier());
    }
    private boolean validInteraction(ServerPlayer player, ArenaInteractable entity, ArenaSession session) {
        return session != null && player.isAlive() && !player.isSpectator() && !entity.isRemoved()
                && session.host().equals(entity.arenaHost()) && player.distanceToSqr(entity) <= 64;
    }
    public boolean nextFloor(ServerPlayer player) {
        if (followedHost(player) != null) return followFloor(player, guestDestination(player));
        ArenaSession session = sessionFor(player);
        return session == null ? tell(player, "wrong_dimension") : session.controller().nextFloor(player);
    }
    public boolean reset(ServerPlayer player) {
        ArenaSession session = sessions.get(player.getUUID());
        // An original owner returning outside cannot erase the successor's live encounter or chest.
        if (session != null && !session.isLeader(player)) return tell(player, "host_only");
        var profile = data.profile(player.getUUID());
        profile.progress.reset();
        follow(player.getUUID(), null);
        // Persist the request even when the host has left and has no arena leased yet.
        profile.requestArenaReset();
        if (session != null) session.resetArena();
        player.sendSystemMessage(Component.translatable("message.spiral_abyss.reset"));
        return true;
    }
    public void loggedIn(ServerPlayer player) {
        if (player.level().dimension().equals(AbyssDimension.LEVEL)) {
            CompoundTag mark = persisted(player);
            mark.putBoolean(RETURN_REQUIRED, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, mark);
        }
        if (ArenaSession.hasDefeatMarker(player)) ArenaSession.restoreSurvival(player);
        returnAfterDisconnect(player);
    }
    public void loggedOut(ServerPlayer player) {
        if (player.level().dimension().equals(AbyssDimension.LEVEL)) {
            CompoundTag mark = persisted(player);
            mark.putBoolean(RETURN_REQUIRED, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, mark);
        }
        ArenaSession session = sessionFor(player);
        if (session != null) {
            session.remove(player.getUUID());
            if (session.players().isEmpty()) retire(session);
        }
        membership.remove(player.getUUID()); hudStates.remove(player);
        follow(player.getUUID(), null);
    }
    private void returnAfterDisconnect(ServerPlayer player) {
        if (!persisted(player).getBoolean(RETURN_REQUIRED)) return;
        ArenaSession.restoreSurvival(player);
        if (player.level().dimension().equals(AbyssDimension.LEVEL) && !leave(player, true)) return;
        CompoundTag mark = persisted(player);
        mark.remove(RETURN_REQUIRED); mark.remove(VISIT); mark.remove(VISIT_FLOOR); mark.remove(RETURN);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, mark);
        follow(player.getUUID(), null);
    }
    public void tick() {
        long now = System.currentTimeMillis();
        int days = AbyssConfig.RESET_DAYS.get();
        if (cycle().due(now, days)) resetGlobal(cycle().nextStart(now, days));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            returnAfterDisconnect(player);
            returnAfterCycleReset(player);
        }
        portals.tick();
        ServerLevel level = server.getLevel(AbyssDimension.LEVEL);
        if (level == null) return;
        if (maintainBorder(level)) level.players().forEach(p -> p.connection.send(new ClientboundInitializeBorderPacket(level.getWorldBorder())));
        // Only the admission service may place players in a room. Reconnects and arbitrary
        // dimension teleports must not rebuild visits or bypass a combat/loot admission lock.
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (profile(player.getUUID()).cycleReturnRequired) continue;
            if (sessionFor(player) != null) continue;
            ArenaSession.restoreSurvival(player);
            leave(player, true);
        }
        for (ArenaSession session : List.copyOf(sessions.values())) {
            for (UUID id : session.admitted()) {
                if (session.players().stream().noneMatch(p -> p.getUUID().equals(id) && !p.hasDisconnected())) {
                    session.remove(id); membership.remove(id);
                }
            }
            if (session.players().isEmpty()) { retire(session); continue; }
            session.tick();
        }
        // Player object identity changes on reconnect/respawn, forcing a fresh snapshot even in a paused delay.
        var online = server.getPlayerList().getPlayers();
        hudStates.keySet().retainAll(online);
        for (ServerPlayer player : online) {
            ArenaSession session = sessionFor(player);
            var status = session == null ? AbyssNetwork.EncounterStatus.HIDDEN : session.controller().hudStatus();
            if (!status.equals(hudStates.put(player, status))) AbyssNetwork.encounterStatus(player, status);
        }
        // An offline eliminated player may reconnect after victory, reset, shutdown, or a dimension transfer.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ArenaSession.hasDefeatMarker(player)) continue;
            ArenaSession session = sessionFor(player);
            if (session == null || !session.waitingForParty(player)) ArenaSession.restoreSurvival(player);
        }
        List<Entity> stale = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Projectile projectile) {
                if (projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident trident && trident.isNoPhysics()
                        && trident.getPersistentData().getBoolean(com.pla.spiral_abyss.event.AbyssMobDrops.NO_PICKUP)
                        && trident.getOwner() instanceof Mob owner && trident.distanceToSqr(owner.getEyePosition()) < 2.25) {
                    // Mob inventories already keep their weapon. A returned Loyalty projectile is finished.
                    stale.add(trident); continue;
                }
                ArenaSession owner = sessions.get(hostOf(projectile));
                if (owner == null || Math.abs(entity.getX() - owner.bounds().center().x) > ArenaBounds.FLIGHT_RADIUS
                        || Math.abs(entity.getZ() - owner.bounds().center().z) > ArenaBounds.FLIGHT_RADIUS
                        || entity.getY() < 64 || entity.getY() > 240) stale.add(entity);
                else entity.getPersistentData().putUUID("spiral_abyss_host", owner.host());
                continue;
            }
            if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal && entity.getPersistentData().hasUUID(com.pla.spiral_abyss.boss_enviroment.BossEnvironments.CRYSTAL_OWNER)) {
                ArenaSession owner = sessions.get(hostOf(entity));
                if (owner == null || !owner.controller().bossEnvironments().ownsCrystal(entity)) stale.add(entity);
                continue;
            }
            if (!(entity instanceof Mob mob) || entity instanceof ArenaInteractable) continue;
            ArenaSession session = entity.getPersistentData().hasUUID("spiral_abyss_host")
                    ? sessions.get(entity.getPersistentData().getUUID("spiral_abyss_host")) : nearest(entity.position());
            if (session == null) { stale.add(entity); continue; }
            entity.getPersistentData().putUUID("spiral_abyss_host", session.host());
            session.rescueMob(mob);
            if (server.getTickCount() % 20 == 0) session.keepMobActive(mob);
        }
        stale.forEach(Entity::discard);
        if (server.getTickCount() % 20 == 0) {
            for (ServerLevel world : server.getAllLevels()) {
                stale.clear();
                for (Entity entity : world.getAllEntities()) {
                    if (entity.getPersistentData().hasUUID(WaveSpawner.RUN_TAG)
                            && sessions.values().stream().noneMatch(s -> s.controller().owns(entity))) stale.add(entity);
                }
                stale.forEach(Entity::discard);
            }
        }
    }
    private ArenaSession nearest(Vec3 point) {
        return sessions.values().stream().filter(s -> Math.abs(s.bounds().center().x - point.x) <= ArenaBounds.FLIGHT_RADIUS
                && Math.abs(s.bounds().center().z - point.z) <= ArenaBounds.FLIGHT_RADIUS).findFirst().orElse(null);
    }
    private void retire(ArenaSession session) {
        session.close(); sessions.remove(session.host()); session.profile().slot = -1;
        membership.values().removeIf(session.host()::equals);
    }
    private static CompoundTag persisted(ServerPlayer p) { return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG); }
    private static boolean canTravel(ServerPlayer p) { return !p.isPassenger() && !p.isVehicle() && !p.isSleeping() && p.canChangeDimensions(); }
    private static boolean isSafe(ServerLevel level, ServerPlayer player, Vec3 position) {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) return false;
        BlockPos feet = BlockPos.containing(position);
        if (!level.getWorldBorder().isWithinBounds(feet) || feet.getY() <= level.getMinBuildHeight()
                || feet.getY() + 2 >= level.getMaxBuildHeight()) return false;
        AABB body = player.getBoundingBox().move(position.subtract(player.position()));
        // Collision checks skip unloaded chunks. Load the saved return area before checking
        // its floor, especially after a server restart when the portal is far from spawn.
        AABB collisionArea = body.inflate(1.0);
        for (int x = net.minecraft.util.Mth.floor(collisionArea.minX) >> 4; x <= net.minecraft.util.Mth.floor(collisionArea.maxX) >> 4; x++)
            for (int z = net.minecraft.util.Mth.floor(collisionArea.minZ) >> 4; z <= net.minecraft.util.Mth.floor(collisionArea.maxZ) >> 4; z++)
                level.getChunk(x, z);
        return !level.noCollision(player, body.move(0, -0.05, 0))
                && level.getFluidState(feet).isEmpty() && !level.containsAnyLiquid(body)
                && level.noCollision(player, body);
    }
    private static Vec3 safeReturnPosition(ServerLevel level, ServerPlayer player, Vec3 saved) {
        if (isSafe(level, player, saved)) return saved;
        if (!Double.isFinite(saved.x) || !Double.isFinite(saved.y) || !Double.isFinite(saved.z)) return null;
        BlockPos origin = BlockPos.containing(saved);
        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                BlockPos column = origin.offset(dx, 0, dz);
                if (!level.getWorldBorder().isWithinBounds(column)) continue;
                level.getChunk(column.getX() >> 4, column.getZ() >> 4);
                for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
                    BlockPos floor = column.offset(0, dy - 1, 0);
                    if (floor.getY() < level.getMinBuildHeight() || floor.getY() >= level.getMaxBuildHeight()) continue;
                    var shape = level.getBlockState(floor).getCollisionShape(level, floor);
                    if (shape.isEmpty()) continue;
                    Vec3 candidate = new Vec3(column.getX() + 0.5, floor.getY() + shape.max(net.minecraft.core.Direction.Axis.Y), column.getZ() + 0.5);
                    if (isSafe(level, player, candidate)) return candidate;
                }
            }
        }
        return null;
    }
    private static boolean tell(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.spiral_abyss." + key, args), true); return false;
    }
}
