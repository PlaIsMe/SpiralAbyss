package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static com.pla.spiral_abyss.encounter.data.MobCategory.COMMON_MOB;
import static com.pla.spiral_abyss.encounter.data.MobCategory.FODDER_MOB;

/** Creates a complete wave or rolls back every mob it added. */
public final class WaveSpawner {
    public static final String RUN_TAG = "spiral_abyss_run";
    public static final String DAMAGE_TAG = "spiral_abyss_damage_scale";
    public static final String BOSS_TAG = "spiral_abyss_wave_boss";
    public List<Mob> spawn(ServerLevel level, Vec3 center, ServerPlayer target,
                           List<FloorDefinition.Enemy> composition, UUID runId,
                           WaveEquipment equipment, int floor, double partyScale,
                           MobCatalog.ScalingSnapshot scaling) {
        return spawn(level, center, target, composition, runId, equipment, floor, partyScale, scaling, 0);
    }
    public List<Mob> spawn(ServerLevel level, Vec3 center, ServerPlayer target,
                           List<FloorDefinition.Enemy> composition, UUID runId,
                           WaveEquipment equipment, int floor, double partyScale,
                           MobCatalog.ScalingSnapshot scaling, int endlessWave) {
        List<Mob> created = new ArrayList<>();
        try {
            for (var entry : composition) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entry.id());
                for (int i = 0; i < entry.count(); i++) {
                    Entity entity = type == null ? null : type.create(level);
                    if (!(entity instanceof Mob mob)) {
                        if (entity != null) entity.discard();
                        throw new IllegalArgumentException(entry.id() + " is not a summonable Mob");
                    }
                    created.add(mob);
                    mob.moveTo(center.x, center.y, center.z, 0, 0);
                    // Keep zombie examples from adding random chicken jockeys outside the mob list.
                    SpawnGroupData group = mob instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null;
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(center)),
                            MobSpawnType.COMMAND, group, null);
                    if (mob instanceof net.minecraft.world.entity.monster.Slime slime && entry.category() != null) {
                        if (entry.category() == FODDER_MOB)
                            slime.setSize(1 + level.random.nextInt(2), true);
                        else if (entry.category() == COMMON_MOB) slime.setSize(4, true);
                    }
                    if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin) piglin.setImmuneToZombification(true);
                    if (mob instanceof net.minecraft.world.entity.monster.piglin.Piglin piglin) piglin.setBaby(false);
                    equipment.apply(mob);
                    DragonArena.prepare(mob, center);
                    // Spawn initialization may add riders (for example a spider jockey).
                    new ArrayList<>(mob.getPassengers()).forEach(Entity::discard);
                    mob.stopRiding();
                    mob.setPersistenceRequired();
                    mob.getPersistentData().putUUID(RUN_TAG, runId);
                    mob.getPersistentData().putBoolean(BOSS_TAG,
                            entry.category() == com.pla.spiral_abyss.encounter.data.MobCategory.BOSS);
                    if (MobCatalog.bossOptions(entry.id()).hideBossBar())
                        mob.getPersistentData().putBoolean(BossHealthBars.HIDE_BOSS_BAR_TAG, true);
                    if (endlessWave > 0) WaveScaling.applyEndless(mob, endlessWave, entry.category(), partyScale);
                    else WaveScaling.apply(mob, floor, entry.category(), entry.id(), partyScale, scaling);
                    if (!place(level, mob, center)) throw new IllegalStateException("No safe spawn position for " + entry.id());
                    mob.setTarget(target);
                    if (!level.addFreshEntity(mob)) throw new IllegalStateException("Spawn rejected for " + entry.id());
                    WaveAllies.add(mob);
                }
            }
            return List.copyOf(created);
        } catch (RuntimeException ex) {
            created.forEach(mob -> { WaveAllies.remove(mob); mob.discard(); });
            throw ex;
        }
    }

    public boolean place(ServerLevel level, Mob mob, Vec3 center) {
        double spawnY = center.y + MobCatalog.bossOptions(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())).spawnYOffset();
        var environment = com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog.get(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
        if (environment != null && environment.centered()) {
            level.getChunkAt(BlockPos.containing(center));
            mob.moveTo(center.x, spawnY, center.z, 0, 0);
            return level.getWorldBorder().isWithinBounds(mob.getBoundingBox()) && level.noCollision(mob);
        }
        if (mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            level.getChunkAt(BlockPos.containing(center));
            mob.moveTo(center.x, spawnY + 20, center.z, level.random.nextFloat() * 360, 0);
            return level.getWorldBorder().isWithinBounds(mob.getBoundingBox()) && level.noCollision(mob);
        }
        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 5 + level.random.nextDouble() * 12;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            BlockPos feet = BlockPos.containing(x, center.y, z);
            level.getChunkAt(feet);
            // Keep the support check on the platform while allowing configured multipart
            // bosses to start above it. Collision and liquid checks still use the full mob.
            mob.moveTo(x, spawnY, z, level.random.nextFloat() * 360, 0);
            if (level.getWorldBorder().isWithinBounds(mob.getBoundingBox())
                    && level.getBlockState(feet.below()).isSolidRender(level, feet.below())
                    && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) return true;
        }
        return false;
    }
}
