package com.pla.spiral_abyss.boss_enviroment;

import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.WaveSpawner;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog.Profile;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.ArenaBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Profile-driven terrain with bounded growth/retraction and independent ownership. */
public final class BossEnvironments {
    public static final String CRYSTAL_OWNER = "spiral_abyss_crystal_owner";
    public static final String CRYSTAL_RUN = "spiral_abyss_crystal_run";
    public static final int CLEANUP_TICKS = 100;
    private final ServerLevel level;
    private final ArenaBounds bounds;
    private final UUID host;
    private final TerrainLayers<BlockPos, BlockState> terrain;
    private final Map<UUID, Effect> effects = new LinkedHashMap<>();
    private record Column(BlockPos base, int height, BlockState state, boolean crystals, boolean dragonShape) {}
    private static final class Effect {
        final Mob boss;
        final Profile profile;
        final List<BossTerrainPlan.Placement> plan;
        final List<Column> columns;
        final Set<BlockPos> placed = new LinkedHashSet<>();
        final Map<BlockPos, BlockPos> plantBases = new HashMap<>();
        final List<EndCrystal> crystals = new ArrayList<>();
        List<BlockPos> retract = List.of();
        int cursor, ticks, cleanupTicks = -1, cleanupCursor;
        Effect(Mob boss, Profile profile, List<BossTerrainPlan.Placement> plan, List<Column> columns) { this.boss = boss; this.profile = profile; this.plan = plan; this.columns = columns; }
    }
    public BossEnvironments(ServerLevel level, ArenaBounds bounds, UUID host) {
        if (!level.dimension().equals(AbyssDimension.LEVEL)) throw new IllegalArgumentException("Boss environment requires the Abyss");
        this.level = level; this.bounds = bounds; this.host = host;
        terrain = new TerrainLayers<>(level::getBlockState, (pos, state) -> {
            // Each plant pair is prevalidated and restored together. Intermediate half changes
            // must not destroy its partner through neighbor updates and spill decoration loot.
            boolean plant = state.getBlock() instanceof DoublePlantBlock
                    || level.getBlockState(pos).getBlock() instanceof DoublePlantBlock;
            level.setBlock(pos, state, plant ? Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE : Block.UPDATE_ALL);
        });
    }
    public boolean protects(BlockPos pos) { return terrain.contains(pos); }
    public int changedBlocks() { return terrain.size(); }
    public int activeBosses() { return effects.size(); }
    public boolean cleaning() { return effects.values().stream().anyMatch(effect -> effect.cleanupTicks >= 0); }
    public boolean ownsCrystal(Entity entity) {
        if (!(entity instanceof EndCrystal crystal) || !crystal.getPersistentData().hasUUID(CRYSTAL_OWNER)) return false;
        Effect effect = effects.get(crystal.getPersistentData().getUUID(CRYSTAL_OWNER));
        return effect != null && effect.cleanupTicks < 0 && effect.crystals.stream().anyMatch(owned -> owned.getUUID().equals(crystal.getUUID()));
    }
    public static boolean crystalFor(EndCrystal crystal, EnderDragon dragon) {
        var data = crystal.getPersistentData();
        return !crystal.isRemoved() && data.hasUUID(CRYSTAL_OWNER) && data.getUUID(CRYSTAL_OWNER).equals(dragon.getUUID()) && data.hasUUID(CRYSTAL_RUN) && dragon.getPersistentData().hasUUID(WaveSpawner.RUN_TAG) && data.getUUID(CRYSTAL_RUN).equals(dragon.getPersistentData().getUUID(WaveSpawner.RUN_TAG));
    }
    public void begin(Collection<? extends Mob> mobs) {
        if (cleaning()) throw new IllegalStateException("Previous boss terrain is still retracting");
        BlockPos center = BlockPos.containing(bounds.center());
        for (Mob mob : mobs) {
            Profile profile = BossEnvironmentCatalog.get(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
            if (profile == null || effects.containsKey(mob.getUUID())) continue;
            List<BossTerrainPlan.Placement> plan = new ArrayList<>(BossTerrainPlan.patches(profile, center, level.random));
            plan.removeIf(placement -> {
                BlockPos base = new BlockPos(placement.pos().getX(), center.getY(), placement.pos().getZ());
                return !level.isEmptyBlock(base) && !terrain.contains(base);
            });
            List<Column> columns = columns(profile, center, mob instanceof EnderDragon);
            // Pillars replace mound entries at their footprint; no per-owner hidden layers are needed.
            plan.removeIf(placement -> columns.stream().anyMatch(column -> pillarCell(column,
                    placement.pos().getX() - column.base().getX(), placement.pos().getZ() - column.base().getZ())));
            for (Column column : columns) for (int y = 0; y < column.height(); y++)
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (pillarCell(column, x, z))
                    plan.add(new BossTerrainPlan.Placement(column.base().offset(x, y, z), column.state(), 2));
            plan.sort(Comparator.comparingDouble((BossTerrainPlan.Placement placement) -> radialOrder(placement.pos(), columns, center)).thenComparingInt(placement -> placement.pos().getY()));
            effects.put(mob.getUUID(), new Effect(mob, profile, List.copyOf(plan), columns));
        }
    }
    private List<Column> columns(Profile profile, BlockPos center, boolean dragonShape) {
        var spec = profile.pillars();
        if (spec == null && dragonShape) spec = new BossEnvironmentCatalog.Pillars(
                Blocks.OBSIDIAN.defaultBlockState(), 4, 5, 18, 26, true);
        if (spec == null) return List.of();
        int count = spec.minimum() + level.random.nextInt(spec.maximum() - spec.minimum() + 1);
        double rotation = level.random.nextDouble() * Math.PI * 2;
        List<Column> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Column chosen = null;
            for (int attempt = 0; attempt < 80 && chosen == null; attempt++) {
                double angle = rotation + index * Math.PI * 2 / count + (level.random.nextDouble() - 0.5) * 0.8;
                double radius = Math.min(21, profile.radius() - 7) + level.random.nextDouble() * 5;
                BlockPos base = center.offset((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
                int height = spec.minHeight() + level.random.nextInt(spec.maxHeight() - spec.minHeight() + 1);
                if (result.stream().anyMatch(other -> other.base().distSqr(base) < 36)) continue;
                if (clearColumn(base, height, center, dragonShape))
                    chosen = new Column(base, height, spec.block(), spec.crystals(), dragonShape);
            }
            if (chosen == null) throw new IllegalStateException("No safe position for boss environment pillar " + (index + 1));
            result.add(chosen);
        }
        return List.copyOf(result);
    }
    private static double radialOrder(BlockPos pos, List<Column> columns, BlockPos center) {
        for (Column column : columns) if (pillarCell(column, pos.getX() - column.base().getX(), pos.getZ() - column.base().getZ()))
            return BossTerrainPlan.distance(column.base(), center);
        return BossTerrainPlan.distance(pos, center);
    }
    /** Dragon pillars are five by five minus four corners; other profile pillars remain three by three. */
    private static boolean pillarCell(Column column, int x, int z) {
        return column.dragonShape() ? dragonPillarCell(x, z) : Math.abs(x) <= 1 && Math.abs(z) <= 1;
    }
    static boolean dragonPillarCell(int x, int z) {
        return Math.abs(x) <= 2 && Math.abs(z) <= 2 && !(Math.abs(x) == 2 && Math.abs(z) == 2);
    }
    private boolean clearColumn(BlockPos base, int height, BlockPos center, boolean dragonShape) {
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (dragonShape ? !dragonPillarCell(x, z) : Math.abs(x) > 1 || Math.abs(z) > 1) continue;
            if (BossTerrainPlan.reserved(base.getX() + x - center.getX(), base.getZ() + z - center.getZ()) || level.getBlockState(base.offset(x, -1, z)).isAir()) return false;
            for (int y = 0; y <= height + 1; y++) {
                BlockPos pos = base.offset(x, y, z);
                if ((!level.isEmptyBlock(pos) && !terrain.contains(pos)) || level.getBlockEntity(pos) != null) return false;
            }
        }
        return true;
    }
    public void tick() {
        for (Effect effect : List.copyOf(effects.values())) {
            Mob boss = effect.boss;
            if (effect.cleanupTicks < 0 && (DragonArena.defeated(boss) || boss.level() != level || level.getEntity(boss.getUUID()) != boss)) retract(effect);
            if (effect.cleanupTicks >= 0) {
                effect.cleanupTicks++;
                int target = (int) Math.ceil(effect.retract.size() * Math.min(effect.cleanupTicks, CLEANUP_TICKS) / (double) CLEANUP_TICKS);
                while (effect.cleanupCursor < target) {
                    BlockPos pos = effect.retract.get(effect.cleanupCursor++);
                    particles(pos, level.getBlockState(pos));
                    removePlacement(effect, pos);
                }
                if (effect.cleanupTicks >= CLEANUP_TICKS) { terrain.remove(boss.getUUID()); effects.remove(boss.getUUID()); }
                continue;
            }
            if (effect.ticks >= effect.profile.ticks()) continue;
            effect.ticks++;
            int target = (int) Math.ceil(effect.plan.size() * effect.ticks / (double) effect.profile.ticks());
            while (effect.cursor < target) place(effect, effect.plan.get(effect.cursor++));
            if (effect.ticks == effect.profile.ticks()) for (Column column : effect.columns) crystal(effect, column);
        }
    }
    private void place(Effect effect, BossTerrainPlan.Placement placement) {
        BlockPos pos = placement.pos();
        if (pos.getY() < ArenaBounds.FLOOR_Y || level.getBlockEntity(pos) != null || !level.isEmptyBlock(pos) && !terrain.contains(pos)) return;
        if (level.getBlockState(pos.below()).isAir() || !placement.state().canSurvive(level, pos)) return;
        if (placement.state().getBlock() instanceof DoublePlantBlock) {
            placePlant(effect, placement);
            return;
        }
        // Lift only walkable terrain, never pillars or hazards. Unsafe cells remain skipped.
        if (!makeRoom(placement)) return;
        terrain.put(effect.boss.getUUID(), pos, placement.state(), placement.priority());
        effect.placed.add(pos);
        if ((effect.cursor & 7) == 0) particles(pos, placement.state());
    }
    private void placePlant(Effect effect, BossTerrainPlan.Placement placement) {
        BlockPos lower = placement.pos(), upper = lower.above();
        // Decoration cannot replace an existing solid overlay or displace either half's actors.
        if (!level.isEmptyBlock(lower) || !level.isEmptyBlock(upper) || level.getBlockEntity(upper) != null
                || upper.getY() >= ArenaBounds.CEILING_Y
                || !level.getEntitiesOfClass(LivingEntity.class, new AABB(lower).expandTowards(0, 1, 0), LivingEntity::isAlive).isEmpty()) return;
        BlockState lowerState = placement.state().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upperState = lowerState.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        Map<BlockPos, BlockState> pair = new LinkedHashMap<>();
        pair.put(lower, lowerState); pair.put(upper, upperState);
        // Some modded plants auto-place their upper half from onPlace. Snapshot both first.
        effect.placed.addAll(pair.keySet());
        effect.plantBases.put(lower, lower); effect.plantBases.put(upper, lower);
        terrain.putAll(effect.boss.getUUID(), pair, placement.priority());
        if ((effect.cursor & 7) == 0) particles(lower, lowerState);
    }
    private void removePlacement(Effect effect, BlockPos pos) {
        BlockPos lower = effect.plantBases.get(pos);
        if (lower != null) {
            terrain.remove(effect.boss.getUUID(), lower.above());
            terrain.remove(effect.boss.getUUID(), lower);
        } else terrain.remove(effect.boss.getUUID(), pos);
    }
    private boolean makeRoom(BossTerrainPlan.Placement placement) {
        BlockPos pos = placement.pos();
        var actors = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), LivingEntity::isAlive);
        if (actors.isEmpty()) return true;
        // A single snow layer has no collision and can cover the grass beneath an actor.
        if (placement.state().is(Blocks.SNOW) && placement.state().getCollisionShape(level, pos).isEmpty()) return true;
        if (placement.priority() != 0) return false;
        var shape = placement.state().getCollisionShape(level, pos);
        if (shape.isEmpty()) return true;
        double top = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
        Map<LivingEntity, Double> lifts = new LinkedHashMap<>();
        for (LivingEntity actor : actors) {
            double lift = top - actor.getBoundingBox().minY;
            if (lift <= 0) continue;
            if (lift > 1.1251 || actor.isPassenger() || actor.isVehicle()) return false;
            AABB moved = actor.getBoundingBox().move(0, lift, 0);
            if (moved.maxY > ArenaBounds.CEILING_Y || !level.noCollision(actor, moved)) return false;
            if (actor instanceof net.minecraft.server.level.ServerPlayer && (moved.minX < bounds.center().x - ArenaBounds.HALF_SIZE || moved.maxX > bounds.center().x + ArenaBounds.HALF_SIZE || moved.minZ < bounds.center().z - ArenaBounds.HALF_SIZE || moved.maxZ > bounds.center().z + ArenaBounds.HALF_SIZE)) return false;
            lifts.put(actor, lift);
        }
        // Validate every actor before moving any of them, so a failed placement is atomic.
        lifts.forEach((actor, lift) -> {
            if (actor instanceof net.minecraft.server.level.ServerPlayer player) player.connection.teleport(player.getX(), player.getY() + lift, player.getZ(), player.getYRot(), player.getXRot());
            else actor.setPos(actor.getX(), actor.getY() + lift, actor.getZ());
            actor.fallDistance = 0;
        });
        return true;
    }
    private void crystal(Effect effect, Column column) {
        if (!column.crystals()) return;
        // An occupied/skipped column never receives a floating crystal.
        for (int y = 0; y < column.height(); y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (pillarCell(column, x, z) && !effect.placed.contains(column.base().offset(x, y, z))) return;
        BlockPos top = column.base().above(column.height());
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(top).inflate(1), LivingEntity::isAlive).isEmpty()) return;
        EndCrystal crystal = EntityType.END_CRYSTAL.create(level);
        if (crystal == null) throw new IllegalStateException("Unable to create arena healing crystal");
        crystal.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
        crystal.setShowBottom(false);
        crystal.getPersistentData().putUUID(CRYSTAL_OWNER, effect.boss.getUUID());
        crystal.getPersistentData().putUUID(CRYSTAL_RUN, effect.boss.getPersistentData().getUUID(WaveSpawner.RUN_TAG));
        crystal.getPersistentData().putUUID("spiral_abyss_host", host);
        effect.crystals.add(crystal);
        if (!level.addFreshEntity(crystal)) throw new IllegalStateException("Arena crystal spawn rejected");
    }
    private void particles(BlockPos pos, BlockState state) {
        if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0.03);
    }
    private void discardCrystals(Effect effect) {
        effect.crystals.forEach(crystal -> {
            Entity loaded = level.getEntity(crystal.getUUID());
            if (loaded instanceof EndCrystal && loaded.getPersistentData().hasUUID(CRYSTAL_OWNER) && effect.boss.getUUID().equals(loaded.getPersistentData().getUUID(CRYSTAL_OWNER))) loaded.discard();
            crystal.discard();
        });
        effect.crystals.clear();
    }
    private void retract(Effect effect) {
        discardCrystals(effect);
        effect.cleanupTicks = 0;
        BlockPos center = BlockPos.containing(bounds.center());
        effect.retract = effect.placed.stream().sorted(Comparator.comparingDouble((BlockPos pos) -> radialOrder(pos, effect.columns, center)).thenComparingInt(BlockPos::getY).reversed()).toList();
    }
    /** Immediate cancellation/repair path, independent of the normal visible defeat animation. */
    public void remove(UUID owner) {
        Effect effect = effects.remove(owner);
        if (effect == null) return;
        discardCrystals(effect);
        // Remove decorations before their support even on immediate cancellation.
        effect.placed.stream().sorted(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed())
                .forEach(pos -> removePlacement(effect, pos));
        terrain.remove(owner);
    }
    public void clear() {
        for (UUID owner : List.copyOf(effects.keySet())) remove(owner);
        terrain.clear();
    }
}
