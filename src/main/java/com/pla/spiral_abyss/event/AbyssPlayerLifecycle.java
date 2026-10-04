package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reconnects always end the previous visit, including saved spectator visits after a restart. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssPlayerLifecycle {
    private AbyssPlayerLifecycle() {}

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AbyssParties.get(player.server).loggedIn(player);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AbyssParties.disconnect(player);
    }
}
