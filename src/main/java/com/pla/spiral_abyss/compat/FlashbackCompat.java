package com.pla.spiral_abyss.compat;

import net.minecraft.server.MinecraftServer;

/** Playback servers reconstruct recorded scenes rather than running live Abyss visits. */
public final class FlashbackCompat {
    private FlashbackCompat() {}

    public static boolean isReplayServer(MinecraftServer server) {
        // Match Flashback's server type without loading its client-only classes or requiring the mod.
        for (Class<?> type = server.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals("com.moulberry.flashback.playback.ReplayServer")) return true;
        }
        return false;
    }
}
