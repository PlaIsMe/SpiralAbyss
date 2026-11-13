package com.pla.spiral_abyss.encounter;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.*;

/** Optional native puzzle encounters. No Qliphoth or FDLib classes link on an absent-mod server. */
public final class QliphothEncounters {
    private static final TicketType<UUID> TICKET = TicketType.create("spiral_abyss_puzzle", UUID::compareTo, 40);
    private static final String PLAYER_FIGHT = "spiral_abyss_qliphoth_fight";
    private static final String OWNER = "spiral_abyss_environment_owner";
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
    private static final ThreadLocal<Fight> CURRENT = new ThreadLocal<>();
    private static final class Fight {
        final LivingEntity boss;
        final Vec3 center;
        final Set<Entity> helpers = new HashSet<>();
        final Set<UUID> players = new HashSet<>();
        final Map<UUID, ChunkPos> tickets = new HashMap<>();
        boolean ready;
        Fight(LivingEntity boss, Vec3 center) {
            this.boss = boss; this.center = center;
        }
    }
    private QliphothEncounters() {}

    public static boolean supports(ResourceLocation id) {
        return id.getNamespace().equals("fdbosses") && Set.of("chesed", "malkuth", "geburah").contains(id.getPath());
    }
    public static boolean helper(Entity entity) {
        return entity != null && EntityType.getKey(entity.getType()).getNamespace().equals("fdbosses")
                && AbyssPlatform.data(entity).hasUUID(OWNER);
    }
    public static void prepare(LivingEntity boss, Vec3 center) {
        if (!supports(EntityType.getKey(boss.getType()))) return;
        var profile = BossEnvironmentCatalog.get(EntityType.getKey(boss.getType()));
        if (profile == null || !profile.flatRoom()) throw new IllegalArgumentException("Qliphoth requires its boss room");
        FIGHTS.put(boss.getUUID(), new Fight(boss, center));
        invoke(boss, "setSpawnPosition", boss.position());
    }
    /** Called after the floor, perch and explicit puzzle helpers are complete. */
    public static void ready(LivingEntity boss) {
        Fight fight = FIGHTS.get(boss.getUUID());
        if (fight == null) return;
        String spawnerId = EntityType.getKey(boss.getType()).getPath() + "_boss_spawner";
        Entity spawner = fight.helpers.stream().filter(entity -> EntityType.getKey(entity.getType()).getPath().equals(spawnerId))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing native boss spawner " + spawnerId));
        invoke(spawner, "setActive", false);
        invoke(boss, "setSpawnedBy", spawner);
        invoke(boss, "setSpawnPosition", boss.position());
        boss.tickCount = 0;
        fight.ready = true;
    }
    public static void own(LivingEntity boss, Entity entity) {
        Fight fight = FIGHTS.get(boss.getUUID());
        if (fight == null) return;
        var data = AbyssPlatform.data(entity);
        data.putUUID(OWNER, boss.getUUID());
        data.putUUID(WaveSpawner.RUN_TAG, AbyssPlatform.data(boss).getUUID(WaveSpawner.RUN_TAG));
        if (AbyssPlatform.data(boss).hasUUID("spiral_abyss_host"))
            data.putUUID("spiral_abyss_host", AbyssPlatform.data(boss).getUUID("spiral_abyss_host"));
        fight.helpers.add(entity);
    }
    /** Synchronous native summons, including children of casting circles, retain the root owner. */
    public static void capture(Entity entity) {
        Fight fight = CURRENT.get();
        if (fight != null && entity.level() == fight.boss.level() && entity != fight.boss
                && EntityType.getKey(entity.getType()).getNamespace().equals("fdbosses")) own(fight.boss, entity);
    }
    public static void tick(Entity entity, Runnable action) {
        if (entity.level().isClientSide || !entity.level().dimension().equals(AbyssDimension.LEVEL)) { action.run(); return; }
        Fight fight = FIGHTS.get(entity.getUUID());
        boolean root = fight != null;
        if (fight == null && helper(entity)) fight = FIGHTS.get(AbyssPlatform.data(entity).getUUID(OWNER));
        if (fight == null) { action.run(); return; }
        if (root && !fight.ready) return;
        if (root) {
            ServerLevel level = (ServerLevel) entity.level();
            for (Entity helper : fight.helpers) {
                boolean outside = helper.level() != level || Math.abs(helper.getX() - fight.center.x) > 128
                        || Math.abs(helper.getZ() - fight.center.z) > 128 || helper.getY() < 0 || helper.getY() > 240;
                if (outside && !helper.isRemoved()) helper.discard();
                ChunkPos previousChunk = fight.tickets.get(helper.getUUID());
                if (helper.isRemoved()) {
                    if (previousChunk != null) level.getChunkSource().removeRegionTicket(TICKET, previousChunk, 2, helper.getUUID());
                    fight.tickets.remove(helper.getUUID());
                } else {
                    ChunkPos chunk = helper.chunkPosition();
                    if (previousChunk != null && !previousChunk.equals(chunk))
                        level.getChunkSource().removeRegionTicket(TICKET, previousChunk, 2, helper.getUUID());
                    level.getChunkSource().addRegionTicket(TICKET, chunk, 2, helper.getUUID());
                    fight.tickets.put(helper.getUUID(), chunk);
                }
            }
            fight.helpers.removeIf(Entity::isRemoved);
            for (var player : ((ServerLevel) entity.level()).players())
                if (player.position().distanceToSqr(fight.center) < 64 * 64) {
                    fight.players.add(player.getUUID());
                    AbyssPlatform.data(player).putString(PLAYER_FIGHT, EntityType.getKey(entity.getType()).getPath());
                }
        }
        Fight previous = CURRENT.get();
        CURRENT.set(fight);
        try { action.run(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    /** Keep direct writes from native spawners/casting helpers under the root's arena guard. */
    public static LivingEntity blockSource() {
        Fight fight = CURRENT.get();
        return fight == null ? null : fight.boss;
    }
    public static boolean owns(Entity entity) {
        if (!helper(entity)) return false;
        Fight fight = FIGHTS.get(AbyssPlatform.data(entity).getUUID(OWNER));
        return fight != null && fight.helpers.contains(entity) && !entity.isRemoved();
    }
    public static void clear(LivingEntity boss) {
        Fight fight = FIGHTS.remove(boss.getUUID());
        if (fight == null) return;
        try {
            if (fight.ready && EntityType.getKey(boss.getType()).getPath().equals("geburah")) {
                invoke(boss, "trySetAllPlayersSins", List.of(), 0, true);
                invoke(boss, "removeCrystalsFromPlayerInventories");
                invoke(boss, "removeAllArenaTrash");
            }
            // The native despawn hook removes music and reactivates the owned spawner only.
            if (fight.ready) invoke(boss, "onFDDespawn");
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Qliphoth encounter cleanup failed for {}", EntityType.getKey(boss.getType()), ex);
        } finally {
            for (UUID id : fight.players) {
                var player = boss.level().getServer().getPlayerList().getPlayer(id);
                if (player != null) clearPlayer(player);
            }
            fight.helpers.forEach(Entity::discard);
            fight.helpers.clear();
            if (boss.level() instanceof ServerLevel level)
                fight.tickets.forEach((id, chunk) -> level.getChunkSource().removeRegionTicket(TICKET, chunk, 2, id));
            fight.tickets.clear();
        }
    }
    /** A persistent marker also covers reconnect after a crash or a missing optional mod. */
    public static void clearPlayer(ServerPlayer player) {
        var data = AbyssPlatform.data(player);
        String boss = data.getString(PLAYER_FIGHT);
        if (boss.isEmpty()) return;
        Set<String> effects = switch (boss) {
            case "chesed" -> Set.of("chesed_gaze", "chesed_darken", "chesed_energized", "shocked");
            case "malkuth" -> Set.of("knight", "coward");
            case "geburah" -> Set.of("sinner");
            default -> Set.of();
        };
        for (String name : effects) {
            var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("fdbosses", name));
            effect.ifPresent(player::removeEffect);
        }
        try {
            Class<?> calls = Class.forName("com.finderfeed.fdlib.FDLibCalls");
            calls.getMethod("stopCutsceneForPlayer", ServerPlayer.class).invoke(null, player);
            if (boss.equals("geburah")) {
                Class<?> sinsType = Class.forName("com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins");
                Object sins = sinsType.getMethod("getPlayerSins", Player.class).invoke(null, player);
                invoke(sins, "setActiveSins", List.of());
                invoke(sins, "setSinnedTimes", 0);
                invoke(sins, "setSinGainCooldown", 0);
                sinsType.getMethod("setPlayerSins", Player.class, sinsType).invoke(null, player, sins);
                for (int index = 0; index < player.getInventory().getContainerSize(); index++)
                    if (BuiltInRegistries.ITEM.getKey(player.getInventory().getItem(index).getItem())
                            .equals(ResourceLocation.fromNamespaceAndPath("fdbosses", "geburah_explosive_crystal")))
                        player.getInventory().setItem(index, ItemStack.EMPTY);
            } else if (boss.equals("malkuth")) {
                Class.forName("com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler")
                        .getMethod("setCurrentWeakness", Player.class, int.class).invoke(null, player, 0);
            }
            data.remove(PLAYER_FIGHT);
        } catch (ClassNotFoundException ex) {
            data.remove(PLAYER_FIGHT); // The provider was removed; no optional class is required.
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LogUtils.getLogger().error("Cannot clear native Qliphoth player state for {}", player.getUUID(), ex);
        }
    }
    private static Object invoke(Object target, String name, Object... args) {
        try {
            for (Method method : target.getClass().getMethods()) if (method.getName().equals(name)
                    && method.getParameterCount() == args.length) return method.invoke(target, args);
            throw new NoSuchMethodException(name);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsupported Qliphoth native contract " + name, ex);
        }
    }
}
