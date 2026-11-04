package com.pla.spiral_abyss.neoforge;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.config.*;
import com.pla.spiral_abyss.init.CommandInit;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.network.neoforge.AbyssPayloadRegistrarImpl;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SpiralAbyss.MOD_ID)
public final class SpiralAbyssNeoForge {
    public SpiralAbyssNeoForge(IEventBus bus, ModContainer container) {
        container.registerConfig(
                ModConfig.Type.COMMON, AbyssConfig.SPEC, "spiral_abyss/spiral_abyss-common.toml");
        container.registerConfig(
                ModConfig.Type.CLIENT,
                AbyssClientConfig.SPEC,
                "spiral_abyss/spiral_abyss-client.toml");
        SpiralAbyss.init();
        bus.addListener(
                (net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                        AbyssNetwork.register(
                                new AbyssPayloadRegistrarImpl(
                                        event.registrar(AbyssNetwork.VERSION))));
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.RegisterCommandsEvent event) ->
                        CommandInit.register(event.getDispatcher()));
    }
}
