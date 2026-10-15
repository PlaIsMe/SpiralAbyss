package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

/** Places one saved base at each private party's spawn; floors reuse that same base. */
public final class AbyssDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "spiral_abyss"));
    private static final ResourceLocation ARENA = ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "abyss_base");
    private AbyssDimension() {}

    public static void initializeArena(ServerLevel level, AbyssPlayers.Profile profile) {
        if (!level.dimension().equals(LEVEL)) throw new IllegalArgumentException("Not the Abyss");
        Vec3 center = profile.bounds().center();
        // Only the 3x3 center chunks are forced while a party is present. Unloaded arenas are cheap.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            int cx = ((int) center.x >> 4) + x, cz = ((int) center.z >> 4) + z;
            if (!level.getForcedChunks().contains(ChunkPos.asLong(cx, cz))) level.setChunkForced(cx, cz, true);
            level.getChunk(cx, cz);
        }
        // Slots are reused by different visits/hosts. A saved placement flag does not mean
        // the blocks are still intact after creative edits or an interrupted encounter.
        restoreArena(level, profile);
    }

    public static void restoreArena(ServerLevel level, AbyssPlayers.Profile profile) {
        if (!level.dimension().equals(LEVEL)) throw new IllegalArgumentException("Not the Abyss");
        com.pla.spiral_abyss.event.AbyssArchitectureRepair.edit(level, () -> restoreArenaBlocks(level, profile));
    }

    private static void restoreArenaBlocks(ServerLevel level, AbyssPlayers.Profile profile) {
        Vec3 center = profile.bounds().center();
        var template = level.getStructureManager().get(ARENA).orElseThrow(() -> new IllegalStateException("Missing abyss_base template"));
        var size = template.getSize();
        if (size.getX() != 65 || size.getY() != 6 || size.getZ() != 65)
            throw new IllegalStateException("abyss_base must fit its 65 x 6 x 65 spawn footprint");
        BlockPos origin = BlockPos.containing(center).offset(-size.getX() / 2, -1, -size.getZ() / 2);
        // Always restore this host's footprint, including creative additions above the base.
        BlockPos top = new BlockPos(origin.getX() + size.getX() - 1, (int) ArenaBounds.FLOOR_Y + 120,
                origin.getZ() + size.getZ() - 1);
        // Include the old template's extra east column. Already-empty cells may still have queued repairs.
        com.pla.spiral_abyss.event.AbyssArchitectureRepair.forget(level, origin, top.east());
        if (profile.needsLegacyCleanup()) clearLegacyArena(level, BlockPos.containing(center));
        for (BlockPos pos : BlockPos.betweenClosed(origin, top)) {
            if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        if (!template.placeInWorld(level, origin, origin, new StructurePlaceSettings().setIgnoreEntities(true), level.random, 2))
            throw new IllegalStateException("Arena placement failed");
        profile.markPlaced();
        com.mojang.logging.LogUtils.getLogger().info("Restored Abyss arena for host {} in slot {} at {}",
                profile.owner, profile.slot, origin);
    }

    private static void clearLegacyArena(ServerLevel level, BlockPos center) {
        // Only the old template's known 66 x 21 x 65 footprint is replaced, never an entire dimension.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-32, -1, -32), center.offset(33, 19, 32))) {
            if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void releaseArena(ServerLevel level, AbyssPlayers.Profile profile) {
        Vec3 center = profile.bounds().center();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            int cx = ((int) center.x >> 4) + x, cz = ((int) center.z >> 4) + z;
            if (level.getForcedChunks().contains(ChunkPos.asLong(cx, cz))) level.setChunkForced(cx, cz, false);
        }
    }
}
