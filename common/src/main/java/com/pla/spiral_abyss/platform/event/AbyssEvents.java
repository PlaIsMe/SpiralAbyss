package com.pla.spiral_abyss.platform.event;

import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.*;

import java.util.*;
import java.util.function.*;

public final class AbyssEvents {
    public static class Event {
        public enum Result {
            DEFAULT,
            ALLOW,
            DENY
        }

        private boolean canceled;
        private BooleanSupplier canceledReader;
        private Consumer<Boolean> canceledWriter;
        private Result result = Result.DEFAULT;

        public boolean isCanceled() {
            return canceledReader == null ? canceled : canceledReader.getAsBoolean();
        }

        public void setCanceled(boolean value) {
            canceled = value;
            if (canceledWriter != null) canceledWriter.accept(value);
        }

        public void bindCancellation(BooleanSupplier reader, Consumer<Boolean> writer) {
            canceledReader = reader;
            canceledWriter = writer;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result value) {
            result = value;
        }
    }

    public static class ServerStartedEvent extends Event {
        public MinecraftServer server;

        public ServerStartedEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class ServerStoppingEvent extends Event {
        public MinecraftServer server;

        public ServerStoppingEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class ServerStoppedEvent extends Event {
        public MinecraftServer server;

        public ServerStoppedEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class OnDatapackSyncEvent extends Event {
        public ServerPlayer player;
        public PlayerList playerList;

        public OnDatapackSyncEvent(ServerPlayer player, PlayerList playerList) {
            this.player = player;
            this.playerList = playerList;
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        public PlayerList getPlayerList() {
            return playerList;
        }
    }

    public static class EntityJoinLevelEvent extends Event {
        public Entity entity;
        public Level level;

        public EntityJoinLevelEvent(Entity entity, Level level) {
            this.entity = entity;
            this.level = level;
        }

        public Entity getEntity() {
            return entity;
        }

        public Level getLevel() {
            return level;
        }
    }

    public static class EntityLeaveLevelEvent extends Event {
        public Entity entity;
        public Level level;

        public EntityLeaveLevelEvent(Entity entity, Level level) {
            this.entity = entity;
            this.level = level;
        }

        public Entity getEntity() {
            return entity;
        }

        public Level getLevel() {
            return level;
        }
    }

    public static class LivingAttackEvent extends Event {
        public LivingEntity entity;
        public DamageSource source;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingAttackEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getAmount() {
            return AmountReader == null ? amount : AmountReader.get();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            AmountReader = reader;
        }
    }

    public static class LivingHurtEvent extends Event {
        public LivingEntity entity;
        public DamageSource source;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingHurtEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getAmount() {
            return AmountReader == null ? amount : AmountReader.get();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            AmountReader = reader;
        }
    }

    public static class LivingDamageEvent extends Event {
        public LivingEntity entity;
        public DamageSource source;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getAmount() {
            return AmountReader == null ? amount : AmountReader.get();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            AmountReader = reader;
        }
    }

    public static class LivingDeathEvent extends Event {
        public LivingEntity entity;
        public DamageSource source;

        public LivingDeathEvent(LivingEntity entity, DamageSource source) {
            this.entity = entity;
            this.source = source;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }
    }

    public static class LivingHealEvent extends Event {
        public LivingEntity entity;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingHealEvent(LivingEntity entity, float amount) {
            this.entity = entity;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public float getAmount() {
            return AmountReader == null ? amount : AmountReader.get();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            AmountReader = reader;
        }
    }

    public static class LivingChangeTargetEvent extends Event {
        public LivingEntity entity;
        public LivingEntity newTarget;
        private Supplier<LivingEntity> NewTargetReader;

        public LivingChangeTargetEvent(LivingEntity entity, LivingEntity newTarget) {
            this.entity = entity;
            this.newTarget = newTarget;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public LivingEntity getNewTarget() {
            return NewTargetReader == null ? newTarget : NewTargetReader.get();
        }

        public void setNewTarget(LivingEntity value) {
            this.newTarget = value;
        }

        public void bindNewTarget(Supplier<LivingEntity> reader) {
            NewTargetReader = reader;
        }
    }

    public static class LivingDestroyBlockEvent extends Event {
        public LivingEntity entity;
        public BlockPos pos;
        public BlockState state;

        public LivingDestroyBlockEvent(LivingEntity entity, BlockPos pos, BlockState state) {
            this.entity = entity;
            this.pos = pos;
            this.state = state;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public BlockPos getPos() {
            return pos;
        }

        public BlockState getState() {
            return state;
        }
    }

    public static class LivingDropsEvent extends Event {
        public LivingEntity entity;
        public Collection<ItemEntity> drops;

        public LivingDropsEvent(LivingEntity entity, Collection<ItemEntity> drops) {
            this.entity = entity;
            this.drops = drops;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public Collection<ItemEntity> getDrops() {
            return drops;
        }
    }

    public static class LivingExperienceDropEvent extends Event {
        public LivingEntity entity;
        public int droppedExperience;
        private Supplier<Integer> DroppedExperienceReader;

        public LivingExperienceDropEvent(LivingEntity entity, int droppedExperience) {
            this.entity = entity;
            this.droppedExperience = droppedExperience;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public int getDroppedExperience() {
            return DroppedExperienceReader == null
                    ? droppedExperience
                    : DroppedExperienceReader.get();
        }

        public void setDroppedExperience(int value) {
            this.droppedExperience = value;
        }

        public void bindDroppedExperience(Supplier<Integer> reader) {
            DroppedExperienceReader = reader;
        }
    }

    public static class LivingUseTotemEvent extends Event {
        public LivingEntity entity;
        public ItemStack totem;

        public LivingUseTotemEvent(LivingEntity entity, ItemStack totem) {
            this.entity = entity;
            this.totem = totem;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public ItemStack getTotem() {
            return totem;
        }
    }

    public static class ShieldBlockEvent extends Event {
        public LivingEntity entity;
        public DamageSource damageSource;
        public float blockedDamage;
        private Supplier<Float> BlockedDamageReader;

        public ShieldBlockEvent(
                LivingEntity entity, DamageSource damageSource, float blockedDamage) {
            this.entity = entity;
            this.damageSource = damageSource;
            this.blockedDamage = blockedDamage;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getDamageSource() {
            return damageSource;
        }

        public float getBlockedDamage() {
            return BlockedDamageReader == null ? blockedDamage : BlockedDamageReader.get();
        }

        public void setBlockedDamage(float value) {
            this.blockedDamage = value;
        }

        public void bindBlockedDamage(Supplier<Float> reader) {
            BlockedDamageReader = reader;
        }
    }

    public static class LivingEquipmentChangeEvent extends Event {
        public LivingEntity entity;
        public EquipmentSlot slot;
        public ItemStack to;

        public LivingEquipmentChangeEvent(LivingEntity entity, EquipmentSlot slot, ItemStack to) {
            this.entity = entity;
            this.slot = slot;
            this.to = to;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public EquipmentSlot getSlot() {
            return slot;
        }

        public ItemStack getTo() {
            return to;
        }
    }

    public static class ProjectileImpactEvent extends Event {
        public Projectile projectile;
        public HitResult rayTraceResult;

        public ProjectileImpactEvent(Projectile projectile, HitResult rayTraceResult) {
            this.projectile = projectile;
            this.rayTraceResult = rayTraceResult;
        }

        public Projectile getProjectile() {
            return projectile;
        }

        public HitResult getRayTraceResult() {
            return rayTraceResult;
        }

        public enum ImpactResult {
            DEFAULT,
            SKIP_ENTITY,
            STOP_AT_CURRENT,
            STOP_AT_CURRENT_NO_DAMAGE
        }

        private Supplier<ImpactResult> impactReader;

        public void bindImpact(Supplier<ImpactResult> reader) {
            impactReader = reader;
        }

        public ImpactResult getImpactResult() {
            return impactReader == null ? ImpactResult.DEFAULT : impactReader.get();
        }
    }

    public static class ArrowLooseEvent extends Event {
        public Player entity;
        public ItemStack bow;

        public ArrowLooseEvent(Player entity, ItemStack bow) {
            this.entity = entity;
            this.bow = bow;
        }

        public Player getEntity() {
            return entity;
        }

        public ItemStack getBow() {
            return bow;
        }
    }

    public static class FillBucketEvent extends Event {
        public Player entity;

        public FillBucketEvent(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return entity;
        }
    }

    public static class EntityMobGriefingEvent extends Event {
        public Entity entity;

        public EntityMobGriefingEvent(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }

    public static class PlayerEvent {
        public static class PlayerLoggedInEvent extends Event {
            public Player entity;

            public PlayerLoggedInEvent(Player entity) {
                this.entity = entity;
            }

            public Player getEntity() {
                return entity;
            }
        }

        public static class PlayerLoggedOutEvent extends Event {
            public Player entity;

            public PlayerLoggedOutEvent(Player entity) {
                this.entity = entity;
            }

            public Player getEntity() {
                return entity;
            }
        }

        public static class ItemPickupEvent extends Event {
            public Player entity;
            public ItemEntity originalEntity;
            public ItemStack stack;

            public ItemPickupEvent(Player entity, ItemEntity originalEntity, ItemStack stack) {
                this.entity = entity;
                this.originalEntity = originalEntity;
                this.stack = stack;
            }

            public Player getEntity() {
                return entity;
            }

            public ItemEntity getOriginalEntity() {
                return originalEntity;
            }

            public ItemStack getStack() {
                return stack;
            }
        }
    }

    public static class LivingEvent {
        public static class LivingTickEvent extends Event {
            public LivingEntity entity;

            public LivingTickEvent(LivingEntity entity) {
                this.entity = entity;
            }

            public LivingEntity getEntity() {
                return entity;
            }
        }
    }

    public static class BlockEvent {
        public static class BreakEvent extends Event {
            public LevelAccessor level;
            public BlockPos pos;
            public BlockState state;
            public Player player;

            public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
                this.level = level;
                this.pos = pos;
                this.state = state;
                this.player = player;
            }

            public LevelAccessor getLevel() {
                return level;
            }

            public BlockPos getPos() {
                return pos;
            }

            public BlockState getState() {
                return state;
            }

            public Player getPlayer() {
                return player;
            }
        }

        public static class EntityPlaceEvent extends Event {
            public LevelAccessor level;
            public BlockPos pos;
            public BlockState placedBlock;
            public Entity entity;

            public EntityPlaceEvent(
                    LevelAccessor level, BlockPos pos, BlockState placedBlock, Entity entity) {
                this.level = level;
                this.pos = pos;
                this.placedBlock = placedBlock;
                this.entity = entity;
            }

            public LevelAccessor getLevel() {
                return level;
            }

            public BlockPos getPos() {
                return pos;
            }

            public BlockState getPlacedBlock() {
                return placedBlock;
            }

            public Entity getEntity() {
                return entity;
            }
        }
    }

    public static class ExplosionEvent {
        public static class Detonate extends Event {
            public Level level;
            public List<BlockPos> affectedBlocks;

            public Detonate(Level level, List<BlockPos> affectedBlocks) {
                this.level = level;
                this.affectedBlocks = affectedBlocks;
            }

            public Level getLevel() {
                return level;
            }

            public List<BlockPos> getAffectedBlocks() {
                return affectedBlocks;
            }
        }

        public static class Start extends Event {
            public Level level;

            public Start(Level level) {
                this.level = level;
            }

            public Level getLevel() {
                return level;
            }
        }
    }

    public static class LevelEvent {
        public static class Unload extends Event {
            public LevelAccessor level;

            public Unload(LevelAccessor level) {
                this.level = level;
            }

            public LevelAccessor getLevel() {
                return level;
            }
        }
    }

    public static class ChunkEvent {
        public static class Load extends Event {
            public LevelAccessor level;
            public ChunkAccess chunk;

            public Load(LevelAccessor level, ChunkAccess chunk) {
                this.level = level;
                this.chunk = chunk;
            }

            public LevelAccessor getLevel() {
                return level;
            }

            public ChunkAccess getChunk() {
                return chunk;
            }
        }
    }

    public static class TickEvent {
        public enum Phase {
            START,
            END
        }

        public static class ServerTickEvent extends Event {
            public final TickEvent.Phase phase;
            public MinecraftServer server;

            public ServerTickEvent(MinecraftServer server, TickEvent.Phase phase) {
                this.server = server;
                this.phase = phase;
            }

            public MinecraftServer getServer() {
                return server;
            }
        }

        public static class LevelTickEvent extends Event {
            public final TickEvent.Phase phase;
            public Level level;

            public LevelTickEvent(Level level, TickEvent.Phase phase) {
                this.level = level;
                this.phase = phase;
            }
        }
    }

    public static class MobSpawnEvent {
        public static class SpawnPlacementCheck extends Event {
            public ServerLevelAccessor level;
            public MobSpawnType spawnType;

            public SpawnPlacementCheck(ServerLevelAccessor level, MobSpawnType spawnType) {
                this.level = level;
                this.spawnType = spawnType;
            }

            public ServerLevelAccessor getLevel() {
                return level;
            }

            public MobSpawnType getSpawnType() {
                return spawnType;
            }
        }

        public static class FinalizeSpawn extends Event {
            public ServerLevelAccessor level;
            public MobSpawnType spawnType;

            public FinalizeSpawn(ServerLevelAccessor level, MobSpawnType spawnType) {
                this.level = level;
                this.spawnType = spawnType;
            }

            public ServerLevelAccessor getLevel() {
                return level;
            }

            public MobSpawnType getSpawnType() {
                return spawnType;
            }

            private boolean spawnCancelled;

            public void setSpawnCancelled(boolean value) {
                spawnCancelled = value;
            }

            public boolean isSpawnCancelled() {
                return spawnCancelled;
            }
        }
    }

    public static class PlayerInteractEvent extends Event {
        private final Player entity;
        private final ItemStack item;
        private final BlockPos pos;
        private final Direction face;
        private final Entity target;
        private net.minecraft.world.InteractionResult result =
                net.minecraft.world.InteractionResult.PASS;

        public PlayerInteractEvent(
                Player player, ItemStack item, BlockPos pos, Direction face, Entity target) {
            entity = player;
            this.item = item;
            this.pos = pos;
            this.face = face;
            this.target = target;
        }

        public Player getEntity() {
            return entity;
        }

        public ItemStack getItemStack() {
            return item;
        }

        public Level getLevel() {
            return entity.level();
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getFace() {
            return face;
        }

        public Entity getTarget() {
            return target;
        }

        public net.minecraft.world.InteractionResult getCancellationResult() {
            return result;
        }

        public void setCancellationResult(net.minecraft.world.InteractionResult value) {
            result = value;
        }

        public static class RightClickItem extends PlayerInteractEvent {
            public RightClickItem(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }

        public static class RightClickBlock extends PlayerInteractEvent {
            public RightClickBlock(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }

        public static class EntityInteract extends PlayerInteractEvent {
            public EntityInteract(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }

        public static class EntityInteractSpecific extends PlayerInteractEvent {
            public EntityInteractSpecific(
                    Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }
    }

    public static class AddReloadListenerEvent {
        private final Consumer<net.minecraft.server.packs.resources.PreparableReloadListener> add;

        public AddReloadListenerEvent(
                Consumer<net.minecraft.server.packs.resources.PreparableReloadListener> add) {
            this.add = add;
        }

        public void addListener(
                net.minecraft.server.packs.resources.PreparableReloadListener listener) {
            add.accept(listener);
        }
    }
}
