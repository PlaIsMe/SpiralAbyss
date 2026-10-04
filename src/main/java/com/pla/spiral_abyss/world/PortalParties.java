package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-authoritative portal reservations. Nothing is trusted from a screen except a requested floor. */
public final class PortalParties {
    private static final int MENU_TIMEOUT_TICKS = 2400;
    private final AbyssParties parties;
    private final MinecraftServer server;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Map<UUID, ArenaMenu> arenaMenus = new HashMap<>();
    private record Selection(UUID token, UUID portal, UUID player, ResourceKey<Level> dimension,
                             BlockPos base, Vec3 returnPosition, int expires) {}
    private record ArenaMenu(UUID token, UUID arenaHost, int floor, UUID followingHost, boolean personalRun, int selectable, int expires) {}
    public PortalParties(AbyssParties parties, MinecraftServer server) { this.parties = parties; this.server = server; }
    public void invalidateAll() { selections.clear(); arenaMenus.clear(); }

    /** Only the party bound to this physical portal may drive its floor indicator or guest admission. */
    public ArenaSession boundSession(AbyssPortalBlockEntity portal) {
        ArenaSession bound = portal.host() == null ? null : parties.hostedBy(portal.host());
        return bound != null && bound.profile().followingHost == null && !bound.profile().personalRun
                && portal.portalId().equals(bound.profile().portal) && !bound.players().isEmpty() ? bound : null;
    }

    public void touch(ServerPlayer player, AbyssPortalFrame frame) {
        if (!(player.level().getBlockEntity(frame.anchor()) instanceof AbyssPortalBlockEntity portal)) return;
        tick();
        UUID host = portal.host();
        ArenaSession bound = boundSession(portal);
        if (bound != null) {
            ArenaSession own = parties.hostedBy(player.getUUID());
            if (own != null && own != bound && !own.players().isEmpty()) { tell(player, "already_hosting"); return; }
            parties.enterPortalGuest(player, host, frame.returnPosition(player));
            return;
        }
        if (selections.values().stream().anyMatch(s -> s.portal.equals(portal.portalId()))) {
            tell(player, "portal_selecting"); return;
        }
        ArenaSession own = parties.hostedBy(player.getUUID());
        if (own != null && !own.players().isEmpty()) { tell(player, "already_hosting"); return; }
        if (own != null) { tell(player, "portal_busy"); return; }
        UUID token = UUID.randomUUID();
        selections.put(player.getUUID(), new Selection(token, portal.portalId(), player.getUUID(), player.level().dimension(),
                frame.anchor(), frame.returnPosition(player), server.getTickCount() + MENU_TIMEOUT_TICKS));
        AbyssNetwork.openFloors(player, token, parties.profile(player.getUUID()).progress.masteredMask(), floorChoices(player),
                126, false);
    }

    public boolean openArenaMenu(ServerPlayer player) {
        ArenaSession session = parties.sessionFor(player);
        if (session == null) return false;
        UUID token = UUID.randomUUID();
        UUID following = parties.followedHost(player);
        int available = 0;
        if (!session.controller().active() && !session.looting()) available = floorChoices(player);
        arenaMenus.put(player.getUUID(), new ArenaMenu(token, session.host(), session.controller().currentFloor(),
                following, parties.profile(player.getUUID()).personalRun, available, server.getTickCount() + MENU_TIMEOUT_TICKS));
        AbyssNetwork.openFloors(player, token, parties.profile(player.getUUID()).progress.masteredMask(), available,
                126, true);
        return true;
    }

    private int floorChoices(ServerPlayer player) {
        int available = 0;
        var progress = parties.profile(player.getUUID()).progress;
        for (int floor : FloorCatalog.snapshot().floors().keySet()) if (progress.canSelectFloor(floor)) available |= 1 << floor;
        return available;
    }
    /** Renew only a still-valid issued token; a heartbeat cannot reopen or authorize a menu. */
    public void keepOpen(ServerPlayer player, UUID token) {
        Selection selection = selections.get(player.getUUID());
        if (selection != null && selection.token.equals(token) && valid(selection, player)) {
            selections.put(player.getUUID(), new Selection(selection.token, selection.portal, selection.player,
                    selection.dimension, selection.base, selection.returnPosition, server.getTickCount() + MENU_TIMEOUT_TICKS));
            return;
        }
        ArenaMenu menu = arenaMenus.get(player.getUUID());
        if (menu != null && menu.token.equals(token) && valid(menu, player))
            arenaMenus.put(player.getUUID(), new ArenaMenu(menu.token, menu.arenaHost, menu.floor, menu.followingHost,
                    menu.personalRun, menu.selectable, server.getTickCount() + MENU_TIMEOUT_TICKS));
    }
    private boolean valid(ArenaMenu menu, ServerPlayer player) {
        ArenaSession session = parties.sessionFor(player);
        return menu.expires >= server.getTickCount() && player.isAlive() && !player.isSpectator() && !player.hasDisconnected()
                && session != null && session.host().equals(menu.arenaHost) && session.controller().currentFloor() == menu.floor
                && Objects.equals(parties.followedHost(player), menu.followingHost)
                && parties.profile(player.getUUID()).personalRun == menu.personalRun;
    }
    public void choose(ServerPlayer player, UUID token, int floor) {
        ArenaMenu menu = arenaMenus.get(player.getUUID());
        if (menu != null && menu.token.equals(token)) {
            arenaMenus.remove(player.getUUID());
            ArenaSession session = parties.sessionFor(player);
            if (!valid(menu, player)) { if (floor != 0) tell(player, "selection_expired"); return; }
            if (floor == -1) { parties.leave(player); return; }
            if (floor <= 0 || (menu.selectable & (1 << floor)) == 0 || session.controller().active()
                    || session.looting()) return;
            if (session.isLeader(player)) parties.enterFloor(player, floor);
            else if (menu.followingHost != null) parties.followFloor(player, floor);
            return;
        }
        Selection selection = selections.get(player.getUUID());
        if (selection == null || !selection.token.equals(token)) {
            if (floor != 0) tell(player, "selection_expired");
            return;
        }
        selections.remove(player.getUUID());
        player.setPortalCooldown();
        if (floor <= 0) return;
        if (!valid(selection, player)) { tell(player, "selection_expired"); return; }
        var portal = (AbyssPortalBlockEntity) player.level().getBlockEntity(selection.base);
        if (parties.hostedBy(player.getUUID()) != null) { tell(player, "already_hosting"); return; }
        if (!parties.enterFloor(player, floor)) return;
        parties.bindPortal(player.getUUID(), selection.portal);
        portal.bindHost(player.getUUID());
        parties.rememberPortalReturn(player, selection.returnPosition);
    }

    private boolean valid(Selection selection, ServerPlayer player) {
        if (selection.expires < server.getTickCount() || !player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || !player.level().dimension().equals(selection.dimension)
                || player.distanceToSqr(Vec3.atCenterOf(selection.base)) > 100
                || !player.level().hasChunkAt(selection.base)) return false;
        return player.level().getBlockEntity(selection.base) instanceof AbyssPortalBlockEntity portal
                && portal.portalId().equals(selection.portal) && portal.frame().active(player.level()) && boundSession(portal) == null;
    }

    public void tick() {
        selections.values().removeIf(selection -> {
            ServerPlayer player = server.getPlayerList().getPlayer(selection.player);
            return player == null || player.hasDisconnected() || !valid(selection, player);
        });
        arenaMenus.entrySet().removeIf(entry -> entry.getValue().expires < server.getTickCount()
                || server.getPlayerList().getPlayer(entry.getKey()) == null);
    }
    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.spiral_abyss." + key), true);
    }
}
