package com.pla.spiral_abyss.boss_enviroment;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.WaveSpawner;
import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.world.ArenaBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.*;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.world.ArenaFlight;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;

/** Auxiliary entities belong to a boss, but are never added to its stage's enemy list. */
final class BossPlacedEntities {
    static final String OWNER = "spiral_abyss_environment_owner";
    private static final TicketType<UUID> TICKET = TicketType.create("spiral_abyss_environment", UUID::compareTo, 40);
    private final List<Entity> entities = new ArrayList<>();
    private final Map<UUID, ChunkPos> tickets = new HashMap<>();
    private ServerLevel ticketLevel;
    boolean owns(Entity entity) { return entities.stream().anyMatch(owned -> owned == entity && !owned.isRemoved()); }

    private void keepActive(ServerLevel level, Entity entity) {
        ticketLevel = level;
        ChunkPos next = entity.chunkPosition(), previous = tickets.put(entity.getUUID(), next);
        if (previous != null && !previous.equals(next)) level.getChunkSource().removeRegionTicket(TICKET, previous, 2, entity.getUUID());
        level.getChunkSource().addRegionTicket(TICKET, next, 2, entity.getUUID());
    }
    void tick(ServerLevel level, ArenaBounds bounds, boolean halfWater) {
        for (var iterator = entities.iterator(); iterator.hasNext();) {
            Entity entity = iterator.next();
            if (entity.level() != level) entity.discard();
            if (!entity.isRemoved()) {
                boolean outside = Math.abs(entity.getX() - bounds.center().x) > ArenaBounds.FLIGHT_RADIUS
                        || Math.abs(entity.getZ() - bounds.center().z) > ArenaBounds.FLIGHT_RADIUS
                        || entity.getY() < 0 || entity.getY() > 240;
                if (entity instanceof Mob mob) {
                    ArenaFlight.steer(mob, bounds);
                    boolean ghost = MobCatalog.isGhostMob(EntityType.getKey(mob.getType()));
                    if (bounds.rescueMob(mob, ghost)) {
                        Vec3 point = surface(level, bounds.center(), halfWater);
                        mob.teleportTo(point.x, point.y, point.z);
                        ArenaFlight.stop(mob);
                    }
                } else if (outside) entity.discard();
            }
            if (entity.isRemoved()) {
                ChunkPos chunk = tickets.remove(entity.getUUID());
                if (chunk != null) level.getChunkSource().removeRegionTicket(TICKET, chunk, 2, entity.getUUID());
                if (entity instanceof Mob mob) WaveAllies.remove(mob);
                iterator.remove();
            } else keepActive(level, entity);
        }
    }

    static Vec3 surface(ServerLevel level, Vec3 point, boolean halfWater) {
        // Full water is an underwater encounter: keep the boss within players' Y=120 ceiling.
        for (int y = (int) ArenaBounds.CEILING_Y - 1; y >= (int) ArenaBounds.FLOOR_Y - 1; y--) {
            BlockPos pos = BlockPos.containing(point.x, y, point.z);
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) return new Vec3(point.x, Math.max(halfWater ? ArenaBounds.FLOOR_Y + 15 : ArenaBounds.FLOOR_Y, y + shape.max(Direction.Axis.Y)), point.z);
        }
        return new Vec3(point.x, ArenaBounds.FLOOR_Y + (halfWater ? 15 : 0), point.z);
    }

    /** A deterministic expanding square packs count>1 around the requested anchor. */
    static Vec3 candidate(Vec3 anchor, int index, double spacing) {
        if (index == 0) return anchor;
        int ring = (int) Math.ceil((Math.sqrt(index + 1) - 1) / 2);
        int start = (2 * ring - 1) * (2 * ring - 1), offset = index - start, edge = 2 * ring;
        int side = offset / edge, step = offset % edge;
        return anchor.add(switch (side) { case 0 -> -ring + step; case 1 -> ring; case 2 -> ring - step; default -> -ring; } * spacing,
                0, switch (side) { case 0 -> -ring; case 1 -> -ring + step; case 2 -> ring; default -> ring - step; } * spacing);
    }

    static boolean fits(Entity entity, ArenaBounds bounds) {
        var box = entity.getBoundingBox(); var center = bounds.center();
        return box.minX >= center.x - ArenaBounds.HALF_SIZE && box.maxX <= center.x + ArenaBounds.HALF_SIZE
                && box.minZ >= center.z - ArenaBounds.HALF_SIZE && box.maxZ <= center.z + ArenaBounds.HALF_SIZE
                && box.minY >= ArenaBounds.FLOOR_Y && box.maxY <= 240;
    }

    void spawn(ServerLevel level, ArenaBounds bounds, UUID host, Mob boss, BossEnvironmentCatalog.Profile profile) {
        for (var entry : profile.entities()) {
            var type = AbyssRegistries.ENTITY_TYPES.getValue(entry.id());
            if (!AbyssRegistries.ENTITY_TYPES.containsKey(entry.id()) || type == null) {
                LogUtils.getLogger().warn("Skipping unavailable environment entity {}", entry.id()); continue;
            }
            for (int count = 0; count < entry.count(); count++) {
                Entity entity = type.create(level);
                if (entity == null) { LogUtils.getLogger().warn("Cannot create environment entity {}", entry.id()); break; }
                entities.add(entity); // Register before any hook can throw; emergency cleanup owns it now.
                if (entity instanceof Player || entity instanceof ArenaInteractable || entity instanceof ItemEntity) {
                    entity.discard(); LogUtils.getLogger().warn("Unsupported environment entity {}", entry.id()); break;
                }
                AbyssPlatform.data(entity).putUUID(OWNER, boss.getUUID());
                AbyssPlatform.data(entity).putUUID(WaveSpawner.RUN_TAG, AbyssPlatform.data(boss).getUUID(WaveSpawner.RUN_TAG));
                AbyssPlatform.data(entity).putUUID("spiral_abyss_host", host);
                Vec3 anchor = entry.position().resolve(bounds.center(), entity.getBbWidth() / 2);
                entity.setPos(anchor);
                if (entity instanceof Mob mob) {
                    SpawnGroupData group = mob instanceof Zombie
                            ? new Zombie.ZombieGroupData(false, false) : null;
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(anchor)), MobSpawnType.COMMAND, group, null);
                    new ArrayList<>(mob.getPassengers()).forEach(Entity::discard); mob.stopRiding();
                    mob.setPersistenceRequired(); mob.setTarget(boss.getTarget());
                    anchor = entry.position().resolve(bounds.center(), mob.getBbWidth() / 2);
                }
                boolean placed = false;
                for (int attempt = 0; attempt < 256; attempt++) {
                    Vec3 point = candidate(anchor, attempt, Math.max(1.25, entity.getBbWidth() + 0.5));
                    double inset = ArenaBounds.HALF_SIZE - entity.getBbWidth() / 2;
                    // Check X/Z before querying blocks: oversized mod entities must not load remote chunks.
                    if (Math.abs(point.x - bounds.center().x) > inset || Math.abs(point.z - bounds.center().z) > inset) continue;
                    point = surface(level, point, profile.waterDepth() == 15);
                    entity.moveTo(point.x, point.y, point.z, 0, 0);
                    if (!fits(entity, bounds) || !level.noCollision(entity)
                            || entities.stream().anyMatch(other -> other != entity && !other.isRemoved() && other.getBoundingBox().intersects(entity.getBoundingBox()))) continue;
                    // Mob look controllers can overwrite yaw, so initialize body and head as well.
                    Vec3 direction = boss.getEyePosition().subtract(entity.getEyePosition());
                    float yaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90);
                    float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
                    entity.moveTo(point.x, point.y, point.z, yaw, pitch);
                    if (entity instanceof LivingEntity living) { living.setYHeadRot(yaw); living.setYBodyRot(yaw); }
                    if (entity instanceof Mob mob) mob.getLookControl().setLookAt(boss);
                    keepActive(level, entity);
                    if (!level.addFreshEntity(entity)) throw new IllegalStateException("Environment entity spawn rejected: " + entry.id());
                    if (entity instanceof Mob mob) WaveAllies.add(mob);
                    placed = true; break;
                }
                if (!placed) { entity.discard(); LogUtils.getLogger().warn("No safe environment position for {}", entry.id()); }
            }
        }
    }
    void clear() {
        entities.forEach(entity -> { if (entity instanceof Mob mob) WaveAllies.remove(mob); entity.discard(); });
        if (ticketLevel != null) tickets.forEach((id, chunk) -> ticketLevel.getChunkSource().removeRegionTicket(TICKET, chunk, 2, id));
        tickets.clear();
        entities.clear();
    }
}
