package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.ArenaBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.LevelAccessor;

/** Protects architecture while allowing temporary fire and TNT during a player's attempt. */
public final class AbyssBlockProtection {
    private static final TagKey<EntityType<?>> NO_ARENA_GRIEFING = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("spiral_abyss", "no_arena_griefing"));
    private AbyssBlockProtection() {}

    /** Opt-in for mobs whose Forge griefing check precedes direct destroyBlock calls. */
    public static void preventDirectMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity().level().dimension().equals(AbyssDimension.LEVEL)
                && event.getEntity().getType().is(NO_ARENA_GRIEFING)) event.setResult(Event.Result.DENY);
    }

    public static void preventBreaking(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().isCreative()
                && (BlockInit.isAbyssArchitecture(event.getState().getBlock()) || managedTerrain(event.getLevel(), event.getPos()))) event.setCanceled(true);
    }

    private static boolean managedTerrain(LevelAccessor level, BlockPos pos) {
        return level instanceof ServerLevel server && server.dimension().equals(AbyssDimension.LEVEL) && AbyssParties.get(server.getServer()).sessions().stream().anyMatch(session -> session.controller().bossEnvironments().protects(pos));
    }
    public static void protectBossTerrain(LivingDestroyBlockEvent event) {
        if (BlockInit.isAbyssArchitecture(event.getState().getBlock())
                || managedTerrain(event.getEntity().level(), event.getPos())) event.setCanceled(true);
    }

    private static boolean restricted(Player player) {
        return !player.isCreative() && player.level().dimension().equals(AbyssDimension.LEVEL);
    }

    private static boolean combatHazard(BlockState state) {
        return state.is(Blocks.TNT) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }

    private static boolean canUseCombatHazard(Player player, BlockPos position) {
        // The server authorizes placement; let the client predict normal item use.
        if (player.level().isClientSide) return true;
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        var session = AbyssParties.get(serverPlayer.server).sessionFor(serverPlayer);
        if (session == null || !session.controller().active() || !session.isCombatant(serverPlayer)) return false;
        var center = session.bounds().center();
        return Math.abs(position.getX() + 0.5 - center.x) <= ArenaBounds.HALF_SIZE
                && Math.abs(position.getZ() + 0.5 - center.z) <= ArenaBounds.HALF_SIZE
                && position.getY() >= ArenaBounds.FLOOR_Y && position.getY() < ArenaBounds.CEILING_Y;
    }

    public static void preventPlacement(BlockEvent.EntityPlaceEvent event) {
        // EntityMultiPlaceEvent inherits this event, covering beds and other multi-block placements.
        if (event.getEntity() instanceof Player player && restricted(player)
                && !(combatHazard(event.getPlacedBlock()) && canUseCombatHazard(player, event.getPos())))
            event.setCanceled(true);
    }

    public static void preventBlockItemUse(PlayerInteractEvent.RightClickBlock event) {
        var item = event.getItemStack().getItem();
        boolean hazardItem = item == Items.TNT || item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE;
        if (restricted(event.getEntity()) && hazardItem) {
            BlockPos target = event.getPos();
            if (!event.getLevel().getBlockState(target).is(Blocks.TNT) && event.getFace() != null)
                target = target.relative(event.getFace());
            if (!canUseCombatHazard(event.getEntity(), target)) {
                event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true);
            }
            return;
        }
        if (restricted(event.getEntity()) && (event.getItemStack().getItem() instanceof BlockItem
                || event.getItemStack().getItem() instanceof BucketItem)) {
            event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true);
        }
    }

    public static void preventBucketUse(PlayerInteractEvent.RightClickItem event) {
        if (restricted(event.getEntity()) && event.getItemStack().getItem() instanceof BucketItem) {
            event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true);
        }
    }

    public static void preventBucketFill(FillBucketEvent event) {
        if (restricted(event.getEntity())) event.setCanceled(true);
    }

    public static void preserveArenaDuringExplosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().dimension().equals(AbyssDimension.LEVEL)) return;
        // Retain normal entity damage and player ownership, including chained TNT explosions.
        event.getAffectedBlocks().removeIf(pos -> !combatHazard(event.getLevel().getBlockState(pos)));
    }

    /** Run only at attempt/visit boundaries, including recovery after loading an old arena. */
    public static void clearCombatHazards(ServerLevel level, ArenaBounds bounds) {
        var center = bounds.center();
        var area = new AABB(center.x - ArenaBounds.FLIGHT_RADIUS, level.getMinBuildHeight(),
                center.z - ArenaBounds.FLIGHT_RADIUS, center.x + ArenaBounds.FLIGHT_RADIUS,
                level.getMaxBuildHeight(), center.z + ArenaBounds.FLIGHT_RADIUS);
        level.getEntitiesOfClass(PrimedTnt.class, area).forEach(Entity::discard);
        // Flint-and-steel places fire directly, bypassing normal BlockItem placement events.
        // A bounded scan also catches those blocks and leftovers persisted across a restart.
        AABB structure = bounds.structureArea();
        BlockPos.betweenClosedStream(BlockPos.containing(structure.minX, structure.minY, structure.minZ),
                BlockPos.containing(structure.maxX - 1, structure.maxY - 1, structure.maxZ - 1))
                .forEach(pos -> {
                    if (level.hasChunkAt(pos) && combatHazard(level.getBlockState(pos)))
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                });
    }
}
