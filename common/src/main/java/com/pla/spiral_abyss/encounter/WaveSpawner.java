package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;

import static com.pla.spiral_abyss.encounter.data.MobCategory.COMMON_MOB;
import static com.pla.spiral_abyss.encounter.data.MobCategory.FODDER_MOB;

/** Creates a complete wave or rolls back every mob it added. */
public final class WaveSpawner {
    public static final String RUN_TAG = "spiral_abyss_run";
    public static final String DAMAGE_TAG = "spiral_abyss_damage_scale";
    public static final String BOSS_TAG = "spiral_abyss_wave_boss";
    public List<LivingEntity> spawn(ServerLevel level, Vec3 center, ServerPlayer target,
                           List<FloorDefinition.Enemy> composition, UUID runId,
                           WaveEquipment equipment, int floor, double partyScale,
                           MobCatalog.ScalingSnapshot scaling) {
        return spawn(level, center, target, composition, runId, equipment, floor, partyScale, scaling, 0);
    }
    public List<LivingEntity> spawn(ServerLevel level, Vec3 center, ServerPlayer target,
                           List<FloorDefinition.Enemy> composition, UUID runId,
                           WaveEquipment equipment, int floor, double partyScale,
                           MobCatalog.ScalingSnapshot scaling, int endlessWave) {
        // Recheck explicit definitions and staged lineups captured before a config change.
        for (var entry : composition) if (!MobCatalog.selectableMob(entry.id()))
            throw new IllegalArgumentException(entry.id() + " is unavailable or blacklisted in mob_blacklist");
        List<LivingEntity> created = new ArrayList<>();
        try {
            for (var entry : composition) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entry.id());
                for (int i = 0; i < entry.count(); i++) {
                    Entity entity = type == null ? null : type.create(level);
                    if (!(entity instanceof LivingEntity mob)) {
                        if (entity != null) entity.discard();
                        throw new IllegalArgumentException(entry.id() + " is not a summonable living encounter");
                    }
                    if (!(mob instanceof Mob) && !QliphothEncounters.supports(entry.id()))
                        throw new IllegalArgumentException(entry.id() + " has no living encounter adapter");
                    created.add(mob);
                    mob.moveTo(center.x, center.y, center.z, 0, 0);
                    // Keep zombie examples from adding random chicken jockeys outside the mob list.
                    SpawnGroupData group = mob instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null;
                    if (mob instanceof Mob ai) ai.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(center)),
                            MobSpawnType.COMMAND, group);
                    MobSpawnData.apply(mob, MobSpawnData.parse(MobCatalog.bossOptions(entry.id()).spawnNbt()));
                    var initialState = MobCatalog.bossOptions(entry.id()).spawnIntegers();
                    if (!initialState.isEmpty()) {
                        var saved = new CompoundTag();
                        mob.addAdditionalSaveData(saved);
                        initialState.forEach((key, value) -> {
                            if (!saved.contains(key, Tag.TAG_INT))
                                throw new IllegalArgumentException(entry.id() + " has no saved integer " + key);
                            saved.putInt(key, value);
                        });
                        mob.readAdditionalSaveData(saved);
                    }
                    if (mob instanceof Mob ai) MobAwakening.apply(ai);
                    if (mob instanceof Slime slime && entry.category() != null) {
                        if (entry.category() == FODDER_MOB)
                            slime.setSize(1 + level.random.nextInt(2), true);
                        else if (entry.category() == COMMON_MOB) slime.setSize(4, true);
                    }
                    if (mob instanceof AbstractPiglin piglin) piglin.setImmuneToZombification(true);
                    if (mob instanceof Piglin piglin) piglin.setBaby(false);
                    if (mob instanceof Mob ai) equipment.apply(ai);
                    DragonArena.prepare(mob, center);
                    // Spawn initialization may add riders (for example a spider jockey).
                    new ArrayList<>(mob.getPassengers()).forEach(Entity::discard);
                    mob.stopRiding();
                    if (mob instanceof Mob ai) ai.setPersistenceRequired();
                    AbyssPlatform.data(mob).putUUID(RUN_TAG, runId);
                    AbyssPlatform.data(mob).putBoolean(BOSS_TAG,
                            entry.category() == MobCategory.BOSS);
                    if (MobCatalog.bossOptions(entry.id()).hideBossBar())
                        AbyssPlatform.data(mob).putBoolean(BossHealthBars.HIDE_BOSS_BAR_TAG, true);
                    if (endlessWave > 0) WaveScaling.applyEndless(mob, endlessWave, entry.category(), partyScale);
                    else WaveScaling.apply(mob, floor, entry.category(), entry.id(), partyScale, scaling);
                    if (!place(level, mob, center)) throw new IllegalStateException("No safe spawn position for " + entry.id());
                    if (mob instanceof Mob ai) ai.setTarget(target);
                    QliphothEncounters.prepare(mob, center);
                    if (!level.addFreshEntity(mob)) throw new IllegalStateException("Spawn rejected for " + entry.id());
                    WaveAllies.add(mob);
                }
            }
            return List.copyOf(created);
        } catch (RuntimeException ex) {
            created.forEach(mob -> { QliphothEncounters.clear(mob); WaveAllies.remove(mob); mob.discard(); });
            throw ex;
        }
    }

    public boolean place(ServerLevel level, LivingEntity mob, Vec3 center) {
        double spawnY = center.y + MobCatalog.bossOptions(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())).spawnYOffset();
        var environment = BossEnvironmentCatalog.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        if (environment != null && environment.centered()) {
            level.getChunkAt(BlockPos.containing(center));
            mob.moveTo(center.x, spawnY, center.z, 0, 0);
            return level.getWorldBorder().isWithinBounds(mob.getBoundingBox()) && level.noCollision(mob);
        }
        if (mob instanceof EnderDragon) {
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
