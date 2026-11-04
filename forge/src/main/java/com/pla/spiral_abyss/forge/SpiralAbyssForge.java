package com.pla.spiral_abyss.forge;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.config.*;
import com.pla.spiral_abyss.init.CommandInit;

import dev.architectury.platform.forge.EventBuses;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SpiralAbyss.MOD_ID)
public final class SpiralAbyssForge {
    public SpiralAbyssForge(FMLJavaModLoadingContext context) {
        EventBuses.registerModEventBus(SpiralAbyss.MOD_ID, context.getModEventBus());
        context.registerConfig(
                ModConfig.Type.COMMON, AbyssConfig.SPEC, "spiral_abyss/spiral_abyss-common.toml");
        context.registerConfig(
                ModConfig.Type.CLIENT,
                AbyssClientConfig.SPEC,
                "spiral_abyss/spiral_abyss-client.toml");
        SpiralAbyss.init();
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.RegisterCommandsEvent e) ->
                        CommandInit.register(e.getDispatcher()));
    }
}
