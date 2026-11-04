package com.pla.spiral_abyss.fabric;

import net.minecraft.world.entity.player.Player;

/** Associates item-driven world writes with the player whose use is being evaluated. */
public final class FabricPlacementContext {
    private static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();

    public static Player player() {
        return PLAYER.get();
    }

    public static void restore(Player player) {
        if (player == null) PLAYER.remove();
        else PLAYER.set(player);
    }
}
