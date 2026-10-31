package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.compat.FlashbackCompat;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Reconnects always end the previous visit, including saved spectator visits after a restart. */
@EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssPlayerLifecycle {
    private AbyssPlayerLifecycle() {}

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !FlashbackCompat.isReplayServer(player.server))
            AbyssParties.get(player.server).loggedIn(player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !FlashbackCompat.isReplayServer(player.server))
            AbyssParties.disconnect(player);
    }
}
