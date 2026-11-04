package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;

import java.util.*;

/** Shows a vanilla bar only for arena players who have no registered boss bar. */
public final class BossHealthBars {
    /** Only suppresses our fallback; custom/native boss bars remain owned by their mod. */
    public static final String HIDE_BOSS_BAR_TAG = "spiral_abyss_hide_boss_bar";
    private static final Set<ServerBossEvent> REGISTERED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ServerBossEvent> FALLBACKS = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<UUID, ServerBossEvent> bars = new HashMap<>();
    private final Map<UUID, Integer> ages = new HashMap<>();

    /** Called from the ServerBossEvent constructor, including events owned by other mods. */
    public static void registered(ServerBossEvent event) { REGISTERED.add(event); }

    public void tick(List<Mob> enemies, List<ServerPlayer> players) {
        Set<UUID> active = new HashSet<>();
        for (Mob mob : enemies) {
            if (!AbyssPlatform.data(mob).getBoolean(WaveSpawner.BOSS_TAG) || !mob.isAlive()
                    || AbyssPlatform.data(mob).getBoolean(HIDE_BOSS_BAR_TAG)) continue;
            UUID id = mob.getUUID();
            active.add(id);
            if (ages.merge(id, 1, Integer::sum) < 5) continue;
            ServerBossEvent bar = bars.get(id);
            for (ServerPlayer player : players) {
                if (hasOtherBar(player)) {
                    if (bar != null) bar.removePlayer(player);
                    continue;
                }
                if (bar == null) {
                    bar = new ServerBossEvent(mob.getDisplayName(), BossEvent.BossBarColor.RED,
                            BossEvent.BossBarOverlay.PROGRESS);
                    FALLBACKS.add(bar);
                    bars.put(id, bar);
                }
                bar.addPlayer(player);
            }
            if (bar != null) {
                bar.setName(mob.getDisplayName());
                bar.setProgress(Math.max(0, Math.min(1, mob.getHealth() / mob.getMaxHealth())));
                for (ServerPlayer player : List.copyOf(bar.getPlayers()))
                    if (!players.contains(player)) bar.removePlayer(player);
            }
        }
        ages.keySet().removeIf(id -> !active.contains(id));
        bars.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) return false;
            entry.getValue().removeAllPlayers();
            return true;
        });
    }

    public void clear() {
        bars.values().forEach(ServerBossEvent::removeAllPlayers);
        bars.clear();
        ages.clear();
    }

    private static boolean hasOtherBar(ServerPlayer player) {
        for (ServerBossEvent event : REGISTERED)
            if (!FALLBACKS.contains(event) && event.getPlayers().contains(player)) return true;
        return false;
    }
}
