package com.pla.spiral_abyss.encounter;
import com.pla.spiral_abyss.platform.AbyssPlatform;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.world.ArenaBounds;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.function.Consumer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.pla.spiral_abyss.entity.ArenaInteractable;

/** Adopts hostile reinforcements into one current stage, without adding reward candidates. */
public final class EncounterReinforcements {
    public static final String PHASE_BOSS = "spiral_abyss_phase_boss";
    private static final TagKey<EntityType<?>> OWNER_GETTER = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "reinforcement_owner_getter"));
    private static final TagKey<EntityType<?>> SHOOTER_FIELD = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("spiral_abyss", "reinforcement_shooter_field"));
    private static final ClassValue<Optional<Method>> OWNER_METHODS = new ClassValue<>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            try {
                Method method = type.getMethod("getOwner");
                return Entity.class.isAssignableFrom(method.getReturnType()) ? Optional.of(method) : Optional.empty();
            } catch (ReflectiveOperationException | SecurityException ex) { return Optional.empty(); }
        }
    };
    private static final ClassValue<Optional<Field>> SHOOTER_FIELDS = new ClassValue<>() {
        @Override protected Optional<Field> computeValue(Class<?> type) {
            try {
                Field field = type.getField("shooter");
                return Entity.class.isAssignableFrom(field.getType()) ? Optional.of(field) : Optional.empty();
            } catch (ReflectiveOperationException | SecurityException ex) { return Optional.empty(); }
        }
    };
    private final ArenaSession session;
    private final Map<ResourceLocation, LivingEntity> phases = new HashMap<>();
    private final Set<UUID> roots = new HashSet<>();

    public EncounterReinforcements(ArenaSession session) { this.session = session; }

    public void begin(List<LivingEntity> initial) {
        clear();
        for (LivingEntity root : initial) {
            var ids = MobCatalog.bossOptions(EntityType.getKey(root.getType())).phaseMobs();
            if (!ids.isEmpty()) roots.add(root.getUUID());
            for (var id : ids) phases.put(id, root);
        }
    }

    public boolean retainsEnvironment(LivingEntity root) { return roots.contains(root.getUUID()); }

    public void discover(UUID run, Consumer<Mob> accept) {
        var center = session.bounds().center();
        var bounds = new AABB(center.x - ArenaBounds.FLIGHT_RADIUS, session.level().getMinBuildHeight(),
                center.z - ArenaBounds.FLIGHT_RADIUS, center.x + ArenaBounds.FLIGHT_RADIUS,
                ArenaBounds.FLIGHT_CEILING_Y + 16, center.z + ArenaBounds.FLIGHT_RADIUS);
        for (Mob mob : session.level().getEntitiesOfClass(Mob.class, bounds)) {
            var data = AbyssPlatform.data(mob);
            // Existing wave ownership is authoritative, including retired/dead stages.
            if (data.hasUUID(WaveSpawner.RUN_TAG) || !mob.isAlive() || mob.isRemoved()
                    || data.hasUUID("spiral_abyss_environment_owner")
                    || data.hasUUID("spiral_abyss_host") && !session.host().equals(data.getUUID("spiral_abyss_host"))
                    || mob instanceof ArenaInteractable
                    || playerOwned(mob)
                    || mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null
                        && !(owned.getOwner() instanceof Mob owner && session.controller().owns(owner))) continue;
            LivingEntity root = phases.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
            if (root == null && !(mob instanceof Enemy) && mob.getType().getCategory() != MobCategory.MONSTER
                    && !(mob.getTarget() instanceof Player player && session.admitted().contains(player.getUUID()))) continue;
            data.putUUID(WaveSpawner.RUN_TAG, run);
            data.putUUID("spiral_abyss_host", session.host());
            mob.setPersistenceRequired();
            if (root != null) {
                data.putBoolean(WaveSpawner.BOSS_TAG, true);
                data.putString(PHASE_BOSS, EntityType.getKey(root.getType()).toString());
                inheritScaling(root, mob);
            }
            accept.accept(mob);
            WaveAllies.add(mob);
        }
    }

    private boolean playerOwned(Mob mob) {
        // Opt-in contracts for summon items whose living projectiles do not implement OwnableEntity.
        // Inspect only public members; never initialize optional classes or change native ownership.
        try {
            if (mob.getType().is(OWNER_GETTER)) {
                var getter = OWNER_METHODS.get(mob.getClass());
                if (getter.isEmpty() || getter.get().invoke(mob) instanceof Player) return true;
            }
            if (mob.getType().is(SHOOTER_FIELD)) {
                var field = SHOOTER_FIELDS.get(mob.getClass());
                if (field.isEmpty() || field.get().get(mob) instanceof Player) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // A changed optional API must not turn a potentially friendly summon into an enemy.
            return true;
        }
        // Some summon items use persistent ownership instead of vanilla OwnableEntity.
        // Resolve conventional owner/summoner UUID fields without linking optional classes.
        var data = AbyssPlatform.data(mob);
        for (String key : data.getAllKeys()) {
            String name = key.toLowerCase(Locale.ROOT);
            if (!(name.endsWith("owneruuid") || name.endsWith("summoneruuid") || name.equals("owner") || name.equals("summoner"))
                    || !data.hasUUID(key)) continue;
            UUID owner = data.getUUID(key);
            if (session.admitted().contains(owner) || session.level().getServer().getPlayerList().getPlayer(owner) != null) return true;
        }
        return false;
    }

    private static void inheritScaling(LivingEntity root, Mob phase) {
        var original = root.getAttribute(Attributes.MAX_HEALTH);
        var health = phase.getAttribute(Attributes.MAX_HEALTH);
        var modifier = original == null ? null : original.getModifier(WaveScaling.HEALTH_MODIFIER);
        if (health != null && modifier != null) {
            float fraction = phase.getMaxHealth() > 0 ? phase.getHealth() / phase.getMaxHealth() : 1;
            health.removeModifier(WaveScaling.HEALTH_MODIFIER);
            health.addPermanentModifier(new AttributeModifier(WaveScaling.HEALTH_MODIFIER, modifier.amount(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            phase.setHealth(phase.getMaxHealth() * fraction);
        }
        AbyssPlatform.data(phase).putDouble(WaveSpawner.DAMAGE_TAG, AbyssPlatform.data(root).getDouble(WaveSpawner.DAMAGE_TAG));
    }

    public void clear() { phases.clear(); roots.clear(); }
}
