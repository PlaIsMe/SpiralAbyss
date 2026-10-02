package com.pla.spiral_abyss.world;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

/** One void dimension; stable, widely separated arena slots belong to individual hosts. */
public final class AbyssDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(SpiralAbyss.MOD_ID, "spiral_abyss"));
    private static final ResourceLocation ARENA = new ResourceLocation(SpiralAbyss.MOD_ID, "colosseum_arena/colosseum_arena");
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
        if (profile.placed()) return;
        var template = level.getStructureManager().get(ARENA).orElseThrow(() -> new IllegalStateException("Missing arena"));
        BlockPos origin = BlockPos.containing(center).offset(-32, -1, -32);
        if (!template.placeInWorld(level, origin, origin, new StructurePlaceSettings().setIgnoreEntities(true), level.random, 2))
            throw new IllegalStateException("Arena placement failed");
        profile.markPlaced();
    }

    public static void releaseArena(ServerLevel level, AbyssPlayers.Profile profile) {
        Vec3 center = profile.bounds().center();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            int cx = ((int) center.x >> 4) + x, cz = ((int) center.z >> 4) + z;
            if (level.getForcedChunks().contains(ChunkPos.asLong(cx, cz))) level.setChunkForced(cx, cz, false);
        }
    }
}
