package com.pla.spiral_abyss.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shapes in model pixels for the unrotated blockstate variants. */
final class GateShapes {
    static final VoxelShape VERTICAL = box(2, 0, -2, 14, 16, 10);
    static final VoxelShape CURVE_BOTTOM = Shapes.or(box(2, 0, 6, 14, 10, 18),
            box(2, 10, 10, 14, 14, 14), box(2, 6, 0, 14, 16, 6), box(2, 10, 6, 14, 16, 10));
    static final VoxelShape CURVE_TOP = Shapes.or(box(2, 2, 10, 14, 6, 14),
            box(2, 0, 0, 14, 10, 6), box(2, 0, 6, 14, 6, 10), box(2, 6, 6, 14, 16, 18));
    // Model [8,0,0]..[24,16,16], rotated 45 degrees around [8,0,0].
    // The model faces south; canonical north is the blockstate's 180-degree rotation.
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
    private static VoxelShape diamond() {
        // Axis-aligned slices enclose the rotated square, including its overhang.
        double radius = 8 * Math.sqrt(2);
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < 32; i++) {
            double y1 = 2 * radius * i / 32, y2 = 2 * radius * (i + 1) / 32;
            double halfWidth = radius - Math.min(Math.abs(y1 - radius), Math.abs(y2 - radius));
            shape = Shapes.or(shape, box(8 - halfWidth, y1, 0, 8 + halfWidth, y2, 16));
        }
        return shape;
    }
}
