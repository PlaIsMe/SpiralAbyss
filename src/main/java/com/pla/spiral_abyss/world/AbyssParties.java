package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-thread party management. Floor changes reuse a slot; empty parties release it. */
public final class AbyssParties {
    public static final int DIMENSION_SIZE = 2304;
    private static final String RETURN = "spiral_abyss_return", VISIT = "spiral_abyss_visit_host";
    private static final Map<MinecraftServer, AbyssParties> SERVERS = new IdentityHashMap<>();
    private final MinecraftServer server;
    private final AbyssPlayers data;
    private final Map<UUID, ArenaSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, UUID> membership = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private record Invitation(UUID host, int expires) {}
    private AbyssParties(MinecraftServer server) { this.server = server; data = AbyssPlayers.get(server); }
    public static AbyssParties get(MinecraftServer server) { return SERVERS.computeIfAbsent(server, AbyssParties::new); }
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
    public boolean enterOwn(ServerPlayer player) { return enter(player, player.getUUID()); }
    public boolean enterFloor(ServerPlayer player, int number) {
        if (!com.pla.spiral_abyss.encounter.data.FloorCatalog.snapshot().floors().containsKey(number)) return tell(player, "floor_unavailable");
        ArenaSession current = sessionFor(player);
        ArenaSession own = sessions.get(player.getUUID());
        if ((current != null && current.controller().active()) || (own != null && own.controller().active())) return tell(player, "already_running");
        var progress = data.profile(player.getUUID()).progress;
        if (!progress.canSelectFloor(number)) return tell(player, "claim_first");
        // Travel/admission must succeed before changing saved progression.
        if (!enterOwn(player)) return false;
        ArenaSession session = sessionFor(player);
        progress.selectFloor(number);
        session.controller().close(); session.clearParticipants(); session.refreshDisplay();
        player.sendSystemMessage(Component.translatable("message.spiral_abyss.selected_floor", number));
        return true;
    }
    private boolean enter(ServerPlayer player, UUID host) {
        if (!canTravel(player)) return tell(player, "travel_unavailable");
        ArenaSession session = acquire(host);
        if (session == null) return tell(player, "arenas_full");
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
        session.admit(player.getUUID()); membership.put(player.getUUID(), host);
        persisted.putUUID(VISIT, host); player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        // Vanilla initializes clients with the overworld border, even after changing dimensions.
        player.connection.send(new ClientboundInitializeBorderPacket(session.level().getWorldBorder()));
        player.fallDistance = 0; player.setDeltaMovement(Vec3.ZERO);
        return true;
    }
    public boolean leave(ServerPlayer player) {
        if (!canTravel(player)) return tell(player, "travel_unavailable");
        if (!player.level().dimension().equals(AbyssDimension.LEVEL)) return tell(player, "wrong_dimension");
        CompoundTag persisted = persisted(player), saved = persisted.getCompound(RETURN);
        ResourceLocation id = ResourceLocation.tryParse(saved.getString("Dimension"));
        ServerLevel destination = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (destination == null || destination.dimension().equals(AbyssDimension.LEVEL)) destination = server.overworld();
        Vec3 position = new Vec3(saved.getDouble("X"), saved.getDouble("Y"), saved.getDouble("Z"));
        if (id == null || !isSafe(destination, player, position)) {
            position = Vec3.atBottomCenterOf(destination.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destination.getSharedSpawnPos()));
        }
        ArenaSession session = sessionFor(player);
        if (player.changeDimension(destination, new AbyssTeleporter(position, saved.getFloat("Yaw"), saved.getFloat("Pitch"))) == null) return false;
        if (session != null) session.remove(player.getUUID());
        membership.remove(player.getUUID()); persisted.remove(VISIT); persisted.remove(RETURN);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted); player.fallDistance = 0;
        return true;
    }
    public boolean invite(ServerPlayer host, ServerPlayer guest) {
        ArenaSession session = sessionFor(host);
        if (session == null || !session.host().equals(host.getUUID())) return tell(host, "host_only");
        if (host == guest || session.controller().active() || session.admitted().size() >= 4) return tell(host, "party_unavailable");
        invitations.put(guest.getUUID(), new Invitation(host.getUUID(), server.getTickCount() + 1200));
        guest.sendSystemMessage(Component.translatable("message.spiral_abyss.invited", host.getGameProfile().getName(), host.getGameProfile().getName()));
        host.sendSystemMessage(Component.translatable("message.spiral_abyss.invite_sent", guest.getGameProfile().getName()));
        return true;
    }
    public boolean join(ServerPlayer guest, ServerPlayer host) {
        Invitation invitation = invitations.get(guest.getUUID());
        if (invitation == null || !invitation.host.equals(host.getUUID()) || invitation.expires < server.getTickCount()) return tell(guest, "no_invitation");
        ArenaSession target = sessionFor(host);
        if (target == null || !target.host().equals(host.getUUID())) return tell(guest, "host_required");
        if (!enter(guest, host.getUUID())) return false;
        invitations.remove(guest.getUUID()); return true;
    }
    public boolean startKey(ServerPlayer player, KeyEntity key) {
        ArenaSession session = sessionFor(player);
        return validInteraction(player, key, session) && session.controller().start(player, key.position());
    }
    public boolean openChest(ServerPlayer player, RewardChestEntity chest) {
        ArenaSession session = sessionFor(player);
        if (!validInteraction(player, chest, session)) return false;
        boolean opened = session.profile().progress.beginOpening(); session.refreshDisplay(); return opened;
    }
    private boolean validInteraction(ServerPlayer player, ArenaInteractable entity, ArenaSession session) {
        return session != null && player.isAlive() && !player.isSpectator() && !entity.isRemoved()
                && session.host().equals(entity.arenaHost()) && player.distanceToSqr(entity) <= 64;
    }
    public boolean nextFloor(ServerPlayer player) {
        ArenaSession session = sessionFor(player);
        return session == null ? tell(player, "wrong_dimension") : session.controller().nextFloor(player);
    }
    public boolean reset(ServerPlayer player) {
        ArenaSession session = sessions.get(player.getUUID());
        if (session != null) session.controller().close();
        data.profile(player.getUUID()).progress.reset();
        if (session != null) { session.clearParticipants(); session.refreshDisplay(); }
        player.sendSystemMessage(Component.translatable("message.spiral_abyss.reset"));
        return true;
    }
    public void claim(Collection<UUID> beneficiaries, int floor) {
        for (UUID id : beneficiaries) data.profile(id).progress.syncClaim(floor);
    }
    public void tick() {
        ServerLevel level = server.getLevel(AbyssDimension.LEVEL);
        if (level == null) return;
        if (maintainBorder(level)) level.players().forEach(p -> p.connection.send(new ClientboundInitializeBorderPacket(level.getWorldBorder())));
        // Restore a visit after reconnect/restart, or provide a private slot for command-based arrivals.
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (sessionFor(player) != null) continue;
            CompoundTag persisted = persisted(player);
            UUID host = persisted.hasUUID(VISIT) ? persisted.getUUID(VISIT) : player.getUUID();
            if (!enter(player, host) && !enterOwn(player)) leave(player);
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
        List<Entity> stale = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Projectile projectile) {
                ArenaSession owner = sessions.get(hostOf(projectile));
                if (owner == null || Math.abs(entity.getX() - owner.bounds().center().x) > ArenaBounds.FLIGHT_RADIUS
                        || Math.abs(entity.getZ() - owner.bounds().center().z) > ArenaBounds.FLIGHT_RADIUS
                        || entity.getY() < 64 || entity.getY() > 240) stale.add(entity);
                else entity.getPersistentData().putUUID("spiral_abyss_host", owner.host());
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
            invitations.values().removeIf(i -> i.expires < server.getTickCount());
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
        return level.getWorldBorder().isWithinBounds(feet) && feet.getY() > level.getMinBuildHeight()
                && feet.getY() + 2 < level.getMaxBuildHeight() && level.getBlockState(feet.below()).isSolidRender(level, feet.below())
                && level.getFluidState(feet).isEmpty() && level.noCollision(player, player.getBoundingBox().move(position.subtract(player.position())));
    }
    private static boolean tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.spiral_abyss." + key), true); return false;
    }
}
