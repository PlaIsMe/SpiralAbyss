package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Creates a complete wave or rolls back every mob it added. */
public final class WaveSpawner {
    public static final String RUN_TAG = "spiral_abyss_run";
    public static final String DAMAGE_TAG = "spiral_abyss_damage_scale";
    private static final UUID HEALTH_MODIFIER = UUID.fromString("2fce3415-4a70-4976-a728-98234fdac4e3");

    public List<Mob> spawn(ServerLevel level, Vec3 center, ServerPlayer target,
                           FloorDefinition.Wave wave, List<FloorDefinition.Enemy> composition, UUID runId, WaveEquipment equipment) {
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
                        if (entry.category() == com.pla.spiral_abyss.encounter.data.MobCategory.FODDER_MOB)
                            slime.setSize(1 + level.random.nextInt(2), true);
                        else if (entry.category() == com.pla.spiral_abyss.encounter.data.MobCategory.COMMON_MOB) slime.setSize(4, true);
                    }
                    if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin) piglin.setImmuneToZombification(true);
                    if (mob instanceof net.minecraft.world.entity.monster.piglin.Piglin piglin) piglin.setBaby(false);
                    equipment.apply(mob);
                    // Spawn initialization may add riders (for example a spider jockey).
                    new ArrayList<>(mob.getPassengers()).forEach(Entity::discard);
                    mob.stopRiding();
                    mob.setPersistenceRequired();
                    mob.getPersistentData().putUUID(RUN_TAG, runId);
                    mob.getPersistentData().putDouble(DAMAGE_TAG, wave.damageScale());
                    var health = mob.getAttribute(Attributes.MAX_HEALTH);
                    if (health != null) health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
                            "Spiral Abyss health", wave.healthScale() - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    mob.setHealth(mob.getMaxHealth());
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
        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 5 + level.random.nextDouble() * 12;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            BlockPos feet = BlockPos.containing(x, center.y, z);
            level.getChunkAt(feet);
            mob.moveTo(x, center.y, z, level.random.nextFloat() * 360, 0);
            if (level.getWorldBorder().isWithinBounds(mob.getBoundingBox())
                    && level.getBlockState(feet.below()).isSolidRender(level, feet.below())
                    && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) return true;
        }
        return false;
    }
}
