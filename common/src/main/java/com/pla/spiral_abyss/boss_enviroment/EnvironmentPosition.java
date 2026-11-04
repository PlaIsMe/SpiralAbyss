package com.pla.spiral_abyss.boss_enviroment;

import com.google.gson.JsonParseException;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;

/** Compass positions are relative to the private arena, never the dimension origin. */
public record EnvironmentPosition(int x, int z, double fraction) {
    public static EnvironmentPosition parse(String name) {
        String value = name.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-]", "").replace("corner", "");
        if (value.equals("middle") || value.equals("center")) return new EnvironmentPosition(0, 0, 0);
        double fraction = 0.5;
        int distances = 0;
        for (String distance : new String[]{"close", "middle", "far"}) {
            if (value.contains(distance)) {
                distances++;
                fraction = distance.equals("close") ? 0.25 : distance.equals("far") ? 1 : 0.5;
                value = value.replace(distance, "");
            }
        }
        if (distances > 1) throw new JsonParseException("Use one distance in position: " + name);
        return switch (value) {
            case "north" -> new EnvironmentPosition(0, -1, fraction);
            case "south" -> new EnvironmentPosition(0, 1, fraction);
            case "east" -> new EnvironmentPosition(1, 0, fraction);
            case "west" -> new EnvironmentPosition(-1, 0, fraction);
            case "northeast" -> new EnvironmentPosition(1, -1, fraction);
            case "northwest" -> new EnvironmentPosition(-1, -1, fraction);
            case "southeast" -> new EnvironmentPosition(1, 1, fraction);
            case "southwest" -> new EnvironmentPosition(-1, 1, fraction);
            default -> throw new JsonParseException("Unknown environment position: " + name);
        };
    }
    public Vec3 resolve(Vec3 center, double halfWidth) {
        // Keep the entire actor/block inside the playable square, including far corners.
        double distance = Math.max(0, 30 - Math.max(halfWidth, 0.5) - 0.5) * fraction;
        return center.add(x * distance, 0, z * distance);
    }
}
