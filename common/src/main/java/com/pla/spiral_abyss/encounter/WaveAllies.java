package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.compat.FlashbackCompat;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;

/** A single bounded team for spawned wave mobs, with explicit AI/damage guards. */
public final class WaveAllies {
    public static final String TEAM = "abyss";
    private WaveAllies() {}

    public static boolean waveMob(Entity entity) {
        return entity instanceof Mob && entity.level().dimension().equals(AbyssDimension.LEVEL)
                && AbyssPlatform.data(entity).hasUUID(WaveSpawner.RUN_TAG);
    }
    /** Resolve damage ownership without guessing from arena proximity. */
    public static Entity owner(Entity entity) {
        for (int depth = 0; entity != null && depth < 8; depth++) {
            Entity next = entity instanceof Projectile projectile ? projectile.getOwner()
                    : entity instanceof AreaEffectCloud cloud ? cloud.getOwner()
                    : entity instanceof EvokerFangs fangs ? fangs.getOwner()
                    : entity instanceof PrimedTnt tnt ? tnt.getOwner() : null;
            if (next == null || next == entity) return entity;
            entity = next;
        }
        return entity;
    }
    private static boolean friendly(Entity victim, DamageSource source) {
        if (!waveMob(victim)) return false;
        Entity attacker = owner(source.getEntity());
        if (!waveMob(attacker)) attacker = owner(source.getDirectEntity());
        return waveMob(attacker);
    }
    public static void add(Mob mob) {
        if (!waveMob(mob) || !(mob.level() instanceof ServerLevel level)) return;
        addMember(level.getServer().getScoreboard(), ledger(level.getServer()), mob.getScoreboardName());
    }
    static void addMember(Scoreboard board, Membership ledger, String name) {
        var team = board.getPlayerTeam(TEAM);
        if (team == null) team = board.addPlayerTeam(TEAM);
        team.setAllowFriendlyFire(false);
        board.addPlayerToTeam(name, team);
        if (ledger.members.add(name)) ledger.setDirty();
    }
    public static void remove(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        removeMember(level.getServer().getScoreboard(), ledger(level.getServer()), mob.getScoreboardName());
    }
    static void removeMember(Scoreboard board, Membership ledger, String name) {
        if (!ledger.members.remove(name)) return;
        ledger.setDirty();
        var team = board.getPlayerTeam(TEAM);
        if (team != null && board.getPlayersTeam(name) == team) board.removePlayerFromTeam(name, team);
    }
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && waveMob(event.getEntity())) add((Mob) event.getEntity());
    }
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && waveMob(event.getEntity())) remove((Mob) event.getEntity());
    }
    public static void target(LivingChangeTargetEvent event) {
        if (waveMob(event.getEntity()) && waveMob(event.getNewAboutToBeSetTarget())) event.setNewAboutToBeSetTarget(null);
    }
    public static void attack(LivingIncomingDamageEvent event) {
        if (friendly(event.getEntity(), event.getSource())) event.setCanceled(true);
    }
    public static void started(ServerStartedEvent event) {
        if (FlashbackCompat.isReplayServer(event.getServer())) return;
        // Runtime attempts never resume after a server restart. Remove only entries this
        // service owns; preserve unrelated players/mobs already in a user-created team.
        var server = event.getServer();
        var ledger = ledger(server);
        var board = server.getScoreboard();
        clearMembers(board, ledger);
        var abyss = server.getLevel(AbyssDimension.LEVEL);
        if (abyss != null) for (Entity entity : abyss.getAllEntities())
            if (waveMob(entity)) add((Mob) entity);
    }
    private static Membership ledger(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Membership::new, Membership::load, null), "spiral_abyss_team");
    }
    static void clearMembers(Scoreboard board, Membership ledger) {
        for (String member : Set.copyOf(ledger.members)) removeMember(board, ledger, member);
    }
    static final class Membership extends SavedData {
        private final Set<String> members = new HashSet<>();
        static Membership load(CompoundTag tag, HolderLookup.Provider registries) {
            Membership data = new Membership();
            for (Tag member : tag.getList("Members", Tag.TAG_STRING)) data.members.add(member.getAsString());
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            members.forEach(name -> list.add(StringTag.valueOf(name)));
            tag.put("Members", list);
            return tag;
        }
    }
}
