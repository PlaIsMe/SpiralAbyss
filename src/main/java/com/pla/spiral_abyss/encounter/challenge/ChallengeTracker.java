package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.encounter.FloorController;
import com.pla.spiral_abyss.encounter.WaveChallenges;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** One wave's party-wide objectives. Only admitted combatants and actual spawned enemies count. */
public final class ChallengeTracker {
    public record Progress(Component description, int progress, int target, boolean complete, boolean failed) {}
    private record Shot(UUID player, ItemStack weapon, boolean airborne) {}
    private record Use(ItemStack weapon, long tick) {}
    private record Hit(UUID player, ItemStack weapon, boolean projectile, boolean trickshot, boolean explosion,
                       boolean playerBurning, boolean mobBurning, Set<ResourceLocation> playerEffects,
                       Set<ResourceLocation> mobEffects, long attributedAt) {}
    private record Damage(Mob mob, Hit hit, LivingDamageEvent event) {}
    private record Death(Mob mob, Hit hit, LivingDeathEvent event) {}
    private record Impact(Challenge.Type type, ProjectileImpactEvent event) {}
    private static final int DOT_ATTRIBUTION_TICKS = 300;
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private final ArenaSession session;
    private final int waveEnemies;
    private final List<Challenge> challenges;
    private final List<ChallengeCounter> counters;
    private final int equipmentPartySize, requiredEquipmentPlayers;
    private final List<Set<UUID>> armorViolations;
    private final int[] equipmentProgress;
    private final Map<UUID, Shot> shots = new HashMap<>();
    private final Map<UUID, Use> uses = new HashMap<>();
    private final Map<UUID, Damage> lastHits = new HashMap<>();
    private final List<Damage> damage = new ArrayList<>();
    private final List<LivingDamageEvent> receivedDamage = new ArrayList<>();
    private final List<Impact> impacts = new ArrayList<>();
    private final List<LivingUseTotemEvent> usedTotems = new ArrayList<>();
    private final List<ShieldBlockEvent> shieldBlocks = new ArrayList<>();
    private final Map<UUID, Death> deaths = new LinkedHashMap<>();
    private final Set<UUID> countedDeaths = new HashSet<>();

    public ChallengeTracker(ArenaSession session, List<Challenge> challenges, int waveEnemies) {
        if (challenges.size() != 3) throw new IllegalArgumentException("A floor needs three available challenges");
        this.session = session;
        this.waveEnemies = waveEnemies;
        this.challenges = List.copyOf(challenges);
        equipmentPartySize = session.participants().size();
        requiredEquipmentPlayers = Challenge.requiredEquipmentPlayers(equipmentPartySize);
        armorViolations = challenges.stream().map(c -> (Set<UUID>) new HashSet<UUID>()).toList();
        equipmentProgress = new int[challenges.size()];
        counters = challenges.stream().map(c -> new ChallengeCounter(c.requiredCount(waveEnemies))).toList();
        update(0); // Equipment restrictions apply before enemies spawn, including the first frame.
    }
    public List<Progress> progress() {
        List<Progress> result = new ArrayList<>();
        for (int index = 0; index < challenges.size(); index++) {
            var challenge = challenges.get(index);
            var counter = counters.get(index);
            boolean count = challenge.type() != Challenge.Type.TIME && challenge.type() != Challenge.Type.NO_ARMOR
                    && challenge.type() != Challenge.Type.ARMOR_ENCHANTMENT;
            Component description = challenge.description(waveEnemies);
            if (challenge.type() == Challenge.Type.NO_ARMOR || challenge.type() == Challenge.Type.ARMOR_ENCHANTMENT)
                description = Component.translatable("challenge.spiral_abyss.players_progress", description,
                        equipmentProgress[index], requiredEquipmentPlayers);
            result.add(new Progress(description, count ? counter.progress() : 0, count ? counter.target() : 0,
                    counter.complete(), counter.failed()));
        }
        return List.copyOf(result);
    }
    public int stars() { return (int) counters.stream().filter(ChallengeCounter::complete).count(); }
    public void update(int elapsedTicks) {
        for (Impact impact : impacts) {
            if (impact.event.isCanceled() || impact.event.getImpactResult() != ProjectileImpactEvent.ImpactResult.DEFAULT) continue;
            for (int index = 0; index < challenges.size(); index++)
                if (challenges.get(index).type() == impact.type) counters.get(index).increment();
        }
        impacts.clear();
        for (LivingUseTotemEvent event : usedTotems) {
            if (event.isCanceled()) continue;
            for (int index = 0; index < challenges.size(); index++)
                if (challenges.get(index).type() == Challenge.Type.TOTEM) counters.get(index).increment();
        }
        usedTotems.clear();
        for (ShieldBlockEvent event : shieldBlocks) {
            if (event.isCanceled() || !(event.getBlockedDamage() > 0)) continue;
            for (int index = 0; index < challenges.size(); index++)
                if (challenges.get(index).type() == Challenge.Type.SHIELD_BLOCK) counters.get(index).increment();
        }
        shieldBlocks.clear();
        for (LivingDamageEvent hit : receivedDamage) {
            if (hit.isCanceled() || !(hit.getAmount() > 0)) continue;
            for (int index = 0; index < challenges.size(); index++)
                if (challenges.get(index).type() == Challenge.Type.TAKE_DAMAGE) counters.get(index).increment();
        }
        receivedDamage.clear();
        for (Damage hit : damage) {
            if (hit.event.isCanceled() || !(hit.event.getAmount() > 0)) continue;
            for (int index = 0; index < challenges.size(); index++) {
                Challenge challenge = challenges.get(index);
                if (challenge.type() == Challenge.Type.MOB_EFFECT && hit.hit.mobEffects.containsAll(challenge.ids()))
                    counters.get(index).incrementDistinct(hit.mob.getUUID());
                if (challenge.type() == Challenge.Type.TRICKSHOT && hit.hit.trickshot) counters.get(index).increment();
            }
        }
        damage.clear();
        // Confirm a real death after all Forge death handlers ran. Canceled deaths and
        // administrative removals never increment kill challenges.
        for (Iterator<Death> iterator = deaths.values().iterator(); iterator.hasNext();) {
            Death death = iterator.next();
            if (death.event.isCanceled()) { iterator.remove(); continue; }
            if (death.mob.isDeadOrDying() || death.mob.getRemovalReason() == Entity.RemovalReason.KILLED) {
                if (countedDeaths.add(death.mob.getUUID())) countKill(death.mob.getUUID(), death.hit);
                iterator.remove();
            } else if (death.mob.isRemoved() || death.mob.isAlive()) iterator.remove();
        }
        List<ServerPlayer> combatants = session.combatants();
        for (int index = 0; index < challenges.size(); index++) {
            Challenge challenge = challenges.get(index);
            ChallengeCounter counter = counters.get(index);
            switch (challenge.type()) {
                case TIME -> {
                    if (!WaveChallenges.beforeDeadline(elapsedTicks, challenge.target())) counter.fail();
                    counter.set(counter.failed() ? 0 : challenge.target());
                }
                case NO_ARMOR -> {
                    Set<UUID> violations = armorViolations.get(index);
                    combatants.stream().filter(player -> challenge.forbiddenSlots().stream()
                            .anyMatch(slot -> !player.getItemBySlot(slot).isEmpty()))
                            .forEach(player -> violations.add(player.getUUID()));
                    updateNoArmor(index, combatants);
                }
                case ARMOR_ENCHANTMENT -> {
                    equipmentProgress[index] = Math.min(requiredEquipmentPlayers, (int) combatants.stream()
                        .filter(player -> challenge.ids().stream().allMatch(id -> Arrays.stream(ARMOR)
                                .anyMatch(slot -> hasEnchantment(player.getItemBySlot(slot), id))))
                        .count());
                    counter.set(equipmentProgress[index] >= requiredEquipmentPlayers ? 1 : 0);
                }
                default -> { }
            }
        }
        long now = session.level().getGameTime();
        lastHits.values().removeIf(hit -> now - hit.hit.attributedAt > DOT_ATTRIBUTION_TICKS
                || hit.event.isCanceled() || !(hit.event.getAmount() > 0));
    }
    private void updateNoArmor(int index, List<ServerPlayer> combatants) {
        Set<UUID> violations = armorViolations.get(index);
        ChallengeCounter counter = counters.get(index);
        // Each player's violation lasts for this wave; teammates may still meet the fixed quota.
        if (equipmentPartySize - violations.size() < requiredEquipmentPlayers) counter.fail();
        long qualifying = combatants.stream().filter(player -> !violations.contains(player.getUUID())).count();
        equipmentProgress[index] = Math.min(requiredEquipmentPlayers, (int) qualifying);
        counter.set(!counter.failed() && qualifying >= requiredEquipmentPlayers ? 1 : 0);
    }
    private boolean tracked(Mob mob) {
        return session.controller().state() == FloorController.State.RUNNING && session.controller().owns(mob)
                && session.controller().enemies().contains(mob);
    }
    public void projectileLaunched(Projectile projectile) {
        if (session.controller().state() == FloorController.State.RUNNING
                && projectile.getOwner() instanceof ServerPlayer player && session.isCombatant(player))
            shots.put(projectile.getUUID(), new Shot(player.getUUID(), projectileWeapon(player),
                    !player.onGround() && !player.isInWaterOrBubble() && !player.isInLava()
                            && !player.getAbilities().flying));
    }
    public void projectileImpacted(ProjectileImpactEvent event, Mob mob) {
        if (!tracked(mob) || !(event.getProjectile().getOwner() instanceof ServerPlayer player)
                || !session.isCombatant(player)) return;
        Challenge.Type type = event.getProjectile() instanceof FishingHook ? Challenge.Type.FISHING
                : event.getProjectile() instanceof ThrownEnderpearl ? Challenge.Type.ENDER_PEARL_HIT : null;
        if (type != null) impacts.add(new Impact(type, event));
    }
    public void totemUsed(ServerPlayer player, LivingUseTotemEvent event) {
        if (session.controller().state() == FloorController.State.RUNNING && session.isCombatantUsingTotem(player)
                && event.getTotem().is(Items.TOTEM_OF_UNDYING)) usedTotems.add(event);
    }
    public void shieldBlocked(ServerPlayer player, ShieldBlockEvent event) {
        if (session.controller().state() != FloorController.State.RUNNING || !session.isCombatant(player)
                || !player.getUseItem().canPerformAction(ToolActions.SHIELD_BLOCK)
                || !(event.getBlockedDamage() > 0)) return;
        Entity attacker = event.getDamageSource().getEntity();
        if (!(attacker instanceof Mob) && event.getDamageSource().getDirectEntity() instanceof Projectile projectile)
            attacker = projectile.getOwner();
        if (attacker instanceof Mob mob && tracked(mob)) shieldBlocks.add(event);
    }
    /** Capture the actual hand before an instant crossbow/throwable use spawns its entity. */
    public void itemUsed(ServerPlayer player, ItemStack stack) {
        if (session.isCombatant(player)) uses.put(player.getUUID(), new Use(stack.copy(), session.level().getGameTime()));
    }
    public void equipmentChanged(ServerPlayer player, EquipmentSlot slot, ItemStack equipped) {
        if (session.controller().state() != FloorController.State.RUNNING || !session.isCombatant(player) || equipped.isEmpty()) return;
        for (int index = 0; index < challenges.size(); index++) {
            Challenge challenge = challenges.get(index);
            if (challenge.type() == Challenge.Type.NO_ARMOR && challenge.forbiddenSlots().contains(slot)) {
                armorViolations.get(index).add(player.getUUID());
                updateNoArmor(index, session.combatants());
            }
        }
    }
    public void damaged(Mob mob, LivingDamageEvent event) {
        if (!(event.getAmount() > 0) || !tracked(mob)) return;
        Hit hit = capture(mob, event.getSource());
        if (hit == null) return;
        Damage evidence = new Damage(mob, hit, event);
        lastHits.put(mob.getUUID(), evidence);
        damage.add(evidence);
    }
    public void playerDamaged(ServerPlayer player, LivingDamageEvent event) {
        if (!(event.getAmount() > 0) || !session.isCombatant(player) || player.hasDisconnected()) return;
        if (event.getSource().is(DamageTypes.GENERIC_KILL) || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null && event.getSource().getDirectEntity() instanceof Projectile projectile)
            attacker = projectile.getOwner();
        if (attacker instanceof Mob mob && tracked(mob)) receivedDamage.add(event);
    }
    public void died(Mob mob, LivingDeathEvent event) {
        if (!tracked(mob)) return;
        Hit hit = capture(mob, event.getSource());
        if (hit != null) deaths.put(mob.getUUID(), new Death(mob, hit, event));
    }
    private void countKill(UUID mob, Hit hit) {
        for (int index = 0; index < challenges.size(); index++) {
            Challenge challenge = challenges.get(index);
        boolean qualifies = switch (challenge.type()) {
                case WEAPON_ENCHANTMENT -> !hit.weapon.isEmpty() && challenge.ids().stream().allMatch(id -> hasEnchantment(hit.weapon, id));
                case WEAPON -> !hit.weapon.isEmpty() && challenge.ids().contains(ForgeRegistries.ITEMS.getKey(hit.weapon.getItem()));
                case PROJECTILE -> hit.projectile;
                case PLAYER_EFFECT -> hit.playerEffects.containsAll(challenge.ids());
                case EXPLOSION -> hit.explosion;
                case PLAYER_BURNING -> hit.playerBurning;
                case MOB_BURNING -> hit.mobBurning;
                default -> false;
            };
            if (qualifies) counters.get(index).incrementDistinct(mob);
        }
    }
    private Hit capture(Mob mob, DamageSource source) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return null;
        Entity direct = source.getDirectEntity();
        Shot shot = direct == null ? null : shots.get(direct.getUUID());
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p
                : direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer p ? p : null;
        if (player == null && shot != null) player = session.level().getServer().getPlayerList().getPlayer(shot.player);
        ItemStack weapon = shot == null ? null : shot.weapon;
        long attributedAt = session.level().getGameTime();
        if (player == null && source.getEntity() == null && source.getDirectEntity() == null && damageOverTime(source)) {
            Damage prior = lastHits.get(mob.getUUID());
            if (prior != null && !prior.event.isCanceled() && prior.event.getAmount() > 0
                    && attributedAt - prior.hit.attributedAt <= DOT_ATTRIBUTION_TICKS) {
                player = session.level().getServer().getPlayerList().getPlayer(prior.hit.player);
                weapon = prior.hit.weapon;
                attributedAt = prior.hit.attributedAt;
            }
        }
        if (player == null || !session.isCombatant(player) || player.hasDisconnected()) return null;
        // Merely holding a sword during a TNT/thorns/spell kill is not a weapon kill.
        // A projectile without its launch snapshot must never borrow a newly held item.
        if (weapon == null) weapon = direct == player && !source.is(DamageTypes.THORNS)
                && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.IS_PROJECTILE)
                && !damageOverTime(source) ? player.getMainHandItem().copy() : ItemStack.EMPTY;
        boolean projectile = direct instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE)
                || direct != null && challenges.stream().filter(c -> c.type() == Challenge.Type.PROJECTILE)
                    .anyMatch(c -> c.ids().contains(ForgeRegistries.ENTITY_TYPES.getKey(direct.getType())));
        boolean trickshot = direct instanceof AbstractArrow && shot != null && shot.airborne
                && (weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW));
        return new Hit(player.getUUID(), weapon, projectile, trickshot, source.is(DamageTypeTags.IS_EXPLOSION),
                player.isOnFire(), mob.isOnFire(), effectIds(player), effectIds(mob), attributedAt);
    }
    private static boolean damageOverTime(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.WITHER);
    }
    private static Set<ResourceLocation> effectIds(LivingEntity entity) {
        Set<ResourceLocation> ids = new HashSet<>();
        entity.getActiveEffects().forEach(effect -> ids.add(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect())));
        return Set.copyOf(ids);
    }
    private static boolean hasEnchantment(ItemStack stack, ResourceLocation id) {
        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        return enchantment != null && EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0;
    }
    private ItemStack projectileWeapon(ServerPlayer player) {
        ItemStack using = player.getUseItem();
        if (!using.isEmpty()) return using.copy();
        Use use = uses.get(player.getUUID());
        if (use != null && use.tick == session.level().getGameTime()) return use.weapon.copy();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mainRanged = main.getItem() instanceof ProjectileWeaponItem || main.is(Items.TRIDENT);
        boolean offRanged = off.getItem() instanceof ProjectileWeaponItem || off.is(Items.TRIDENT);
        if (mainRanged && !offRanged) return main.copy();
        if (offRanged && !mainRanged) return off.copy();
        return ItemStack.EMPTY; // Ambiguous mod launch: count projectile kills, not guessed weapon kills.
    }
}
