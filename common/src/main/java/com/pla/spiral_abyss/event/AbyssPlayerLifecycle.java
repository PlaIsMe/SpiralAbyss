package com.pla.spiral_abyss.event;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.compat.FlashbackCompat;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;

/** Reconnects always end the previous visit, including saved spectator visits after a restart. */
public final class AbyssPlayerLifecycle {
    private AbyssPlayerLifecycle() {}

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !FlashbackCompat.isReplayServer(player.server))
            AbyssParties.get(player.server).loggedIn(player);
    }

    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !FlashbackCompat.isReplayServer(player.server))
            AbyssParties.disconnect(player);
    }
}
