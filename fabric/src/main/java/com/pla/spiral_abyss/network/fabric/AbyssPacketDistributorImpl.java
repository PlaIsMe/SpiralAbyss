package com.pla.spiral_abyss.network.fabric;

import com.pla.spiral_abyss.fabric.SpiralAbyssFabric;

import dev.architectury.networking.NetworkManager;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class AbyssPacketDistributorImpl {
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        NetworkManager.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        NetworkManager.sendToServer(payload);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        var server = SpiralAbyssFabric.server();
        if (server != null)
            NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), payload);
    }
}
