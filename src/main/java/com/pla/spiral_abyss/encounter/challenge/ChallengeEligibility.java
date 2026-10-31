package com.pla.spiral_abyss.encounter.challenge;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.function.Predicate;
import com.mojang.logging.LogUtils;

/** Check effect/fire susceptibility against the actual cycle roster, without spawning test mobs. */
public final class ChallengeEligibility implements Predicate<Challenge> {
    private final ServerLevel level;
    private final List<List<FloorDefinition.Enemy>> waves;
    private final Map<ResourceLocation, Optional<LivingEntity>> probes = new HashMap<>();

    public ChallengeEligibility(ServerLevel level, List<List<FloorDefinition.Enemy>> waves) {
        this.level = level;
        this.waves = waves.stream().map(List::copyOf).toList();
    }

    @Override public boolean test(Challenge challenge) {
        if (challenge.type() != Challenge.Type.MOB_BURNING && challenge.type() != Challenge.Type.MOB_EFFECT) return true;
        for (var wave : waves) {
            int total = wave.stream().mapToInt(FloorDefinition.Enemy::count).sum();
            int eligible = wave.stream().filter(enemy -> susceptible(enemy.id(), challenge))
                    .mapToInt(FloorDefinition.Enemy::count).sum();
            if (eligible < challenge.requiredCount(total)) return false;
        }
        return true;
    }

    private boolean susceptible(ResourceLocation id, Challenge challenge) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) return false;
        if (challenge.type() == Challenge.Type.MOB_BURNING) return !type.fireImmune();
        Optional<LivingEntity> probe = probes.computeIfAbsent(id, ignored -> {
            try {
                var entity = type.create(level);
                return entity instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
            } catch (RuntimeException exception) {
                LogUtils.getLogger().warn("Cannot check Abyss effect susceptibility for {}", id, exception);
                return Optional.empty();
            }
        });
        if (probe.isEmpty()) return false;
        try {
            return challenge.ids().stream().allMatch(effectId -> {
                var effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                return effect != null && probe.get().canBeAffected(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), 200));
            });
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
