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
        private boolean canceled;
        private BooleanSupplier canceledReader;
        private Consumer<Boolean> canceledWriter;

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
    }

    public static class ServerStartedEvent extends Event {
        private MinecraftServer server;

        public ServerStartedEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class ServerStoppingEvent extends Event {
        private MinecraftServer server;

        public ServerStoppingEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class ServerStoppedEvent extends Event {
        private MinecraftServer server;

        public ServerStoppedEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class OnDatapackSyncEvent extends Event {
        private ServerPlayer player;
        private PlayerList playerList;

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

    public static class ServerTickEvent {
        public static class Post extends Event {
            private MinecraftServer server;

            public Post(MinecraftServer server) {
                this.server = server;
            }

            public MinecraftServer getServer() {
                return server;
            }
        }
    }

    public static class LevelTickEvent {
        public static class Post extends Event {
            private Level level;

            public Post(Level level) {
                this.level = level;
            }

            public Level getLevel() {
                return level;
            }
        }
    }

    public static class EntityTickEvent {
        public static class Pre extends Event {
            private Entity entity;

            public Pre(Entity entity) {
                this.entity = entity;
            }

            public Entity getEntity() {
                return entity;
            }
        }
    }

    public static class EntityJoinLevelEvent extends Event {
        private Entity entity;
        private Level level;

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
        private Entity entity;
        private Level level;

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

    public static class LivingIncomingDamageEvent extends Event {
        private LivingEntity entity;
        private DamageSource source;
        private float amount;
        private Supplier<Float> amountReader;
        private Consumer<Float> amountWriter;

        public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
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
            return amountReader == null ? amount : amountReader.get();
        }

        public void setAmount(float value) {
            amount = value;
            if (amountWriter != null) amountWriter.accept(value);
        }

        public void bindAmount(Supplier<Float> reader, Consumer<Float> writer) {
            amountReader = reader;
            amountWriter = writer;
        }
    }

    public static class LivingDamageEvent {
        public static class Pre extends Event {
            private LivingEntity entity;
            private DamageSource source;
            private float newDamage;
            private Supplier<Float> newDamageReader;
            private Consumer<Float> newDamageWriter;

            public Pre(LivingEntity entity, DamageSource source, float newDamage) {
                this.entity = entity;
                this.source = source;
                this.newDamage = newDamage;
            }

            public LivingEntity getEntity() {
                return entity;
            }

            public DamageSource getSource() {
                return source;
            }

            public float getNewDamage() {
                return newDamageReader == null ? newDamage : newDamageReader.get();
            }

            public void setNewDamage(float value) {
                newDamage = value;
                if (newDamageWriter != null) newDamageWriter.accept(value);
            }

            public void bindNewDamage(Supplier<Float> reader, Consumer<Float> writer) {
                newDamageReader = reader;
                newDamageWriter = writer;
            }
        }

        public static class Post extends Event {
            private LivingEntity entity;
            private DamageSource source;
            private float newDamage;
            private Supplier<Float> newDamageReader;
            private Consumer<Float> newDamageWriter;

            public Post(LivingEntity entity, DamageSource source, float newDamage) {
                this.entity = entity;
                this.source = source;
                this.newDamage = newDamage;
            }

            public LivingEntity getEntity() {
                return entity;
            }

            public DamageSource getSource() {
                return source;
            }

            public float getNewDamage() {
                return newDamageReader == null ? newDamage : newDamageReader.get();
            }

            public void setNewDamage(float value) {
                newDamage = value;
                if (newDamageWriter != null) newDamageWriter.accept(value);
            }

            public void bindNewDamage(Supplier<Float> reader, Consumer<Float> writer) {
                newDamageReader = reader;
                newDamageWriter = writer;
            }
        }
    }

    public static class LivingDeathEvent extends Event {
        private LivingEntity entity;
        private DamageSource source;

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
        private LivingEntity entity;
        private float amount;
        private Supplier<Float> amountReader;
        private Consumer<Float> amountWriter;

        public LivingHealEvent(LivingEntity entity, float amount) {
            this.entity = entity;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public float getAmount() {
            return amountReader == null ? amount : amountReader.get();
        }

        public void setAmount(float value) {
            amount = value;
            if (amountWriter != null) amountWriter.accept(value);
        }

        public void bindAmount(Supplier<Float> reader, Consumer<Float> writer) {
            amountReader = reader;
            amountWriter = writer;
        }
    }

    public static class LivingChangeTargetEvent extends Event {
        private LivingEntity entity;
        private LivingEntity newAboutToBeSetTarget;
        private Supplier<LivingEntity> newAboutToBeSetTargetReader;
        private Consumer<LivingEntity> newAboutToBeSetTargetWriter;

        public LivingChangeTargetEvent(LivingEntity entity, LivingEntity newAboutToBeSetTarget) {
            this.entity = entity;
            this.newAboutToBeSetTarget = newAboutToBeSetTarget;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public LivingEntity getNewAboutToBeSetTarget() {
            return newAboutToBeSetTargetReader == null
                    ? newAboutToBeSetTarget
                    : newAboutToBeSetTargetReader.get();
        }

        public void setNewAboutToBeSetTarget(LivingEntity value) {
            newAboutToBeSetTarget = value;
            if (newAboutToBeSetTargetWriter != null) newAboutToBeSetTargetWriter.accept(value);
        }

        public void bindNewAboutToBeSetTarget(
                Supplier<LivingEntity> reader, Consumer<LivingEntity> writer) {
            newAboutToBeSetTargetReader = reader;
            newAboutToBeSetTargetWriter = writer;
        }
    }

    public static class LivingDestroyBlockEvent extends Event {
        private LivingEntity entity;
        private BlockPos pos;
        private BlockState state;

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
        private LivingEntity entity;
        private Collection<ItemEntity> drops;

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
        private LivingEntity entity;
        private int droppedExperience;
        private Supplier<Integer> droppedExperienceReader;
        private Consumer<Integer> droppedExperienceWriter;

        public LivingExperienceDropEvent(LivingEntity entity, int droppedExperience) {
            this.entity = entity;
            this.droppedExperience = droppedExperience;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public int getDroppedExperience() {
            return droppedExperienceReader == null
                    ? droppedExperience
                    : droppedExperienceReader.get();
        }

        public void setDroppedExperience(int value) {
            droppedExperience = value;
            if (droppedExperienceWriter != null) droppedExperienceWriter.accept(value);
        }

        public void bindDroppedExperience(Supplier<Integer> reader, Consumer<Integer> writer) {
            droppedExperienceReader = reader;
            droppedExperienceWriter = writer;
        }
    }

    public static class LivingUseTotemEvent extends Event {
        private LivingEntity entity;
        private ItemStack totem;

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

    public static class LivingShieldBlockEvent extends Event {
        private LivingEntity entity;
        private DamageSource damageSource;
        private float blockedDamage;
        private Supplier<Float> blockedDamageReader;
        private Consumer<Float> blockedDamageWriter;
        private boolean blocked;
        private Supplier<Boolean> blockedReader;
        private Consumer<Boolean> blockedWriter;

        public LivingShieldBlockEvent(
                LivingEntity entity,
                DamageSource damageSource,
                float blockedDamage,
                boolean blocked) {
            this.entity = entity;
            this.damageSource = damageSource;
            this.blockedDamage = blockedDamage;
            this.blocked = blocked;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getDamageSource() {
            return damageSource;
        }

        public float getBlockedDamage() {
            return blockedDamageReader == null ? blockedDamage : blockedDamageReader.get();
        }

        public void setBlockedDamage(float value) {
            blockedDamage = value;
            if (blockedDamageWriter != null) blockedDamageWriter.accept(value);
        }

        public void bindBlockedDamage(Supplier<Float> reader, Consumer<Float> writer) {
            blockedDamageReader = reader;
            blockedDamageWriter = writer;
        }

        public boolean getBlocked() {
            return blockedReader == null ? blocked : blockedReader.get();
        }

        public void setBlocked(boolean value) {
            blocked = value;
            if (blockedWriter != null) blockedWriter.accept(value);
        }

        public void bindBlocked(Supplier<Boolean> reader, Consumer<Boolean> writer) {
            blockedReader = reader;
            blockedWriter = writer;
        }
    }

    public static class LivingEquipmentChangeEvent extends Event {
        private LivingEntity entity;
        private EquipmentSlot slot;
        private ItemStack to;

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
        private Projectile projectile;
        private HitResult rayTraceResult;

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
    }

    public static class ArrowLooseEvent extends Event {
        private Player entity;
        private ItemStack bow;

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

    public static class EntityMobGriefingEvent extends Event {
        private Entity entity;
        private boolean canGrief;
        private Supplier<Boolean> canGriefReader;
        private Consumer<Boolean> canGriefWriter;

        public EntityMobGriefingEvent(Entity entity, boolean canGrief) {
            this.entity = entity;
            this.canGrief = canGrief;
        }

        public Entity getEntity() {
            return entity;
        }

        public boolean canGrief() {
            return canGriefReader == null ? canGrief : canGriefReader.get();
        }

        public void setCanGrief(boolean value) {
            canGrief = value;
            if (canGriefWriter != null) canGriefWriter.accept(value);
        }

        public void bindCanGrief(Supplier<Boolean> reader, Consumer<Boolean> writer) {
            canGriefReader = reader;
            canGriefWriter = writer;
        }
    }

    public static class PlayerEvent {
        public static class PlayerLoggedInEvent extends Event {
            private Player entity;

            public PlayerLoggedInEvent(Player entity) {
                this.entity = entity;
            }

            public Player getEntity() {
                return entity;
            }
        }

        public static class PlayerLoggedOutEvent extends Event {
            private Player entity;

            public PlayerLoggedOutEvent(Player entity) {
                this.entity = entity;
            }

            public Player getEntity() {
                return entity;
            }
        }
    }

    public static class ItemEntityPickupEvent {
        public static class Post extends Event {
            private Player player;
            private ItemEntity itemEntity;
            private ItemStack originalStack;
            private ItemStack currentStack;

            public Post(
                    Player player,
                    ItemEntity itemEntity,
                    ItemStack originalStack,
                    ItemStack currentStack) {
                this.player = player;
                this.itemEntity = itemEntity;
                this.originalStack = originalStack;
                this.currentStack = currentStack;
            }

            public Player getPlayer() {
                return player;
            }

            public ItemEntity getItemEntity() {
                return itemEntity;
            }

            public ItemStack getOriginalStack() {
                return originalStack;
            }

            public ItemStack getCurrentStack() {
                return currentStack;
            }
        }
    }

    public static class BlockEvent {
        public static class BreakEvent extends Event {
            private LevelAccessor level;
            private BlockPos pos;
            private BlockState state;
            private Player player;

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
            private LevelAccessor level;
            private BlockPos pos;
            private BlockState placedBlock;
            private Entity entity;

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
            private Level level;
            private List<BlockPos> affectedBlocks;

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
            private Level level;

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
            private LevelAccessor level;

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
            private LevelAccessor level;
            private ChunkAccess chunk;

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

    public static class MobSpawnEvent {
        public static class SpawnPlacementCheck extends Event {
            private ServerLevelAccessor level;
            private MobSpawnType spawnType;

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

            public enum Result {
                DEFAULT,
                SUCCEED,
                FAIL
            }

            private Result result = Result.DEFAULT;

            public Result getResult() {
                return result;
            }

            public void setResult(Result result) {
                this.result = result;
            }
        }
    }

    public static class FinalizeSpawnEvent extends Event {
        private ServerLevelAccessor level;
        private MobSpawnType spawnType;

        public FinalizeSpawnEvent(ServerLevelAccessor level, MobSpawnType spawnType) {
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
        private final net.minecraft.core.RegistryAccess registry;
        private final Consumer<net.minecraft.server.packs.resources.PreparableReloadListener> add;

        public AddReloadListenerEvent(
                net.minecraft.core.RegistryAccess registry,
                Consumer<net.minecraft.server.packs.resources.PreparableReloadListener> add) {
            this.registry = registry;
            this.add = add;
        }

        public net.minecraft.core.RegistryAccess getRegistryAccess() {
            return registry;
        }

        public void addListener(
                net.minecraft.server.packs.resources.PreparableReloadListener listener) {
            add.accept(listener);
        }
    }
}
