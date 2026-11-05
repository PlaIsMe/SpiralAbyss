package com.pla.spiral_abyss.boss_enviroment;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog.Profile;
import com.pla.spiral_abyss.world.ArenaBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Bounded ridge or flat-room plans, ordered from center outward and bottom upward. */
public final class BossTerrainPlan {
    // Both faces of each wall must be inside the recovery boundary, including
    // its half-block center offset. Actors hit the wall before containment fires.
    public static final int ROOM_RADIUS = (int) ArenaBounds.HALF_SIZE - 1;
    public static final int ROOM_WALL_HEIGHT = 6;
    public static final int ROOM_BOX_HEIGHT = 32;
    public record Placement(BlockPos pos, BlockState state, int priority) {}
    private BossTerrainPlan() {}
    public static boolean reserved(int x, int z) { return x * x + z * z <= 36 || Math.abs(x) <= 3 && z >= 0; }
    public static double distance(BlockPos pos, BlockPos center) { return Math.hypot(pos.getX() - center.getX(), pos.getZ() - center.getZ()); }
    public static Comparator<Placement> outward(BlockPos center) { return Comparator.comparingDouble((Placement placement) -> distance(placement.pos(), center)).thenComparingInt(placement -> placement.pos().getY()); }
    public static List<Placement> patches(Profile profile, BlockPos center, RandomSource random) {
        if (profile.palette().isEmpty()) return List.of();
        if (profile.flatRoom()) return flatRoom(profile, center, random);
        record Ridge(double x, double z, double width) {}
        List<Ridge> ridges = new ArrayList<>();
        double rotation = random.nextDouble() * Math.PI * 2;
        for (int index = 0; index < profile.patches(); index++) {
            double angle = rotation + (index + random.nextDouble() * 0.4) * Math.PI * 2 / profile.patches();
            double radius = 9 + random.nextDouble() * Math.max(1, profile.radius() - 15);
            ridges.add(new Ridge(Math.cos(angle) * radius, Math.sin(angle) * radius, 3 + random.nextDouble() * 4));
        }
        List<Placement> result = new ArrayList<>();
        for (int x = -profile.radius(); x <= profile.radius(); x++) for (int z = -profile.radius(); z <= profile.radius(); z++) {
            int height = 1;
            if (!reserved(x, z)) for (int index = 0; index < ridges.size(); index++) {
                Ridge from = ridges.get(index), to = ridges.get((index + 1) % ridges.size());
                double dx = to.x() - from.x(), dz = to.z() - from.z(), length = dx * dx + dz * dz;
                double along = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - from.x()) * dx + (z - from.z()) * dz) / length));
                double width = from.width() + (to.width() - from.width()) * along;
                double distance = Math.hypot(x - from.x() - dx * along, z - from.z() - dz * along);
                double shape = Math.max(0, 1 - distance / width);
                height = Math.max(height, 1 + (int) Math.ceil(shape * (profile.height() - 1)));
            }
            // Every eligible foundation column gets a base, including the center and entrance.
            for (int y = 0; y < height; y++) {
                BlockState state = profile.snowDirt()
                        ? (y == height - 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState()
                        : profile.palette().get(random.nextInt(profile.palette().size()));
                result.add(new Placement(center.offset(x, y, z), state, 0));
            }
            if (profile.snowDirt())
                result.add(new Placement(center.offset(x, height, z), Blocks.SNOW.defaultBlockState(), 1));
            else if (!reserved(x, z) && !profile.decorations().isEmpty() && random.nextDouble() < profile.hazardChance())
                result.add(new Placement(center.offset(x, height, z),
                        profile.decorations().get(random.nextInt(profile.decorations().size())), 1));
        }
        result.sort(outward(center));
        return List.copyOf(result);
    }
    /** Replace the foundation at Y=64; clear native ornaments and enclose the playable square. */
    private static List<Placement> flatRoom(Profile profile, BlockPos center, RandomSource random) {
        List<Placement> result = new ArrayList<>();
        int wallHeight = profile.roomBox() ? ROOM_BOX_HEIGHT : ROOM_WALL_HEIGHT;
        for (int x = -ROOM_RADIUS; x <= ROOM_RADIUS; x++) for (int z = -ROOM_RADIUS; z <= ROOM_RADIUS; z++) {
            result.add(new Placement(center.offset(x, -1, z), profile.palette().get(random.nextInt(profile.palette().size())), 2));
            boolean wall = Math.abs(x) == ROOM_RADIUS || Math.abs(z) == ROOM_RADIUS;
            for (int y = 0; y < wallHeight; y++)
                result.add(new Placement(center.offset(x, y, z), wall
                        ? profile.palette().get(random.nextInt(profile.palette().size())) : Blocks.AIR.defaultBlockState(), 2));
            if (profile.roomBox())
                result.add(new Placement(center.offset(x, ROOM_BOX_HEIGHT, z),
                        profile.palette().get(random.nextInt(profile.palette().size())), 2));
        }
        result.sort(outward(center));
        return List.copyOf(result);
    }
}
