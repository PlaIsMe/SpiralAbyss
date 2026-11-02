package com.pla.spiral_abyss;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.init.*;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

@Mod(SpiralAbyss.MOD_ID)
public class SpiralAbyss {
    public static final String MOD_ID = "spiral_abyss";

    public SpiralAbyss(IEventBus modEventBus, ModContainer container) {
        GearConfig.prepare();
        container.registerConfig(ModConfig.Type.COMMON,
                AbyssConfig.SPEC, "spiral_abyss/spiral_abyss-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT,
                AbyssClientConfig.SPEC, "spiral_abyss/spiral_abyss-client.toml");
        EntityInit.ENTITY_TYPES.register(modEventBus);
        BlockInit.BLOCKS.register(modEventBus);
        BlockEntityInit.BLOCK_ENTITIES.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        CreativeTabInit.TABS.register(modEventBus);
        SoundInit.SOUNDS.register(modEventBus);
        modEventBus.addListener(AbyssNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandInit.register(event.getDispatcher());
    }
}
