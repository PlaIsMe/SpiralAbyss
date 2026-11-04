package com.pla.spiral_abyss.forge.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/** Uses the same portal-free transfer pattern as the Cataclysm dimension reference. */
public class AbyssTeleporter implements ITeleporter {
    private final Vec3 position;
    private final float yaw;
    private final float pitch;

    public AbyssTeleporter(Vec3 position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public Entity placeEntity(
            Entity entity,
            ServerLevel currentLevel,
            ServerLevel destinationLevel,
            float yaw,
            Function<Boolean, Entity> repositionEntity) {
        return repositionEntity.apply(false);
    }

    @Override
    public PortalInfo getPortalInfo(
            Entity entity,
            ServerLevel destinationLevel,
            Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return new PortalInfo(position, Vec3.ZERO, yaw, pitch);
    }
}
