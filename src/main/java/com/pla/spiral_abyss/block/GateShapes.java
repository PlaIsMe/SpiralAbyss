package com.pla.spiral_abyss.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Collision follows the imported models, including their narrow profile. */
final class GateShapes {
    static final VoxelShape VERTICAL = box(4, 0, 0, 12, 16, 8);
    static final VoxelShape CURVE = Shapes.or(box(4, 0, 8, 12, 8, 16),
            box(4, 6, 4, 12, 8, 8), box(4, 8, 8, 12, 10, 12), box(4, 8, 0, 12, 16, 8));
    // Runtime model: [8,8,-4]..[20,20,8], rotated 45 degrees about [8,8,-4].
    // Its south-facing depth -4..8 becomes 8..20 for canonical north.
    static final VoxelShape ACTIVATION = diamond();

    private GateShapes() {}
    static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.box(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }
    static VoxelShape turn(VoxelShape shape, Direction facing) {
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        for (int i = 0; i < turns; i++) {
            VoxelShape[] rotated = {Shapes.empty()};
            shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> rotated[0] = Shapes.or(rotated[0],
                    Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
            shape = rotated[0];
        }
        return shape;
    }
    static VoxelShape upsideDown(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> result[0] = Shapes.or(result[0],
                Shapes.box(x1, 1 - y2, z1, x2, 1 - y1, z2)));
        return result[0];
    }
    private static VoxelShape diamond() {
        // Java collision uses axis-aligned boxes. Thin slices approximate the 45-degree
        // square without a full-cube invisible wall. Preserve the exported overhang.
        double radius = 6 * Math.sqrt(2);
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < 32; i++) {
            double y1 = 2 * radius * i / 32, y2 = 2 * radius * (i + 1) / 32;
            double halfWidth = radius - Math.abs((y1 + y2) / 2 - radius);
            shape = Shapes.or(shape, box(8 - halfWidth, 8 + y1, 8, 8 + halfWidth, 8 + y2, 20));
        }
        return shape;
    }
}
