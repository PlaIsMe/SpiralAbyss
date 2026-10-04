package com.pla.spiral_abyss;

import com.pla.spiral_abyss.init.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SpiralAbyss.MOD_ID)
public class SpiralAbyss {
    public static final String MOD_ID = "spiral_abyss";

    public SpiralAbyss() {
        com.pla.spiral_abyss.config.GearConfig.prepare();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.pla.spiral_abyss.config.AbyssConfig.SPEC, "spiral_abyss/spiral_abyss-common.toml");
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EntityInit.ENTITY_TYPES.register(modBus);
        BlockInit.BLOCKS.register(modBus);
        BlockEntityInit.BLOCK_ENTITIES.register(modBus);
        LootInit.MODIFIERS.register(modBus);
        ItemInit.ITEMS.register(modBus);
        CreativeTabInit.TABS.register(modBus);
        com.pla.spiral_abyss.network.AbyssNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandInit.register(event.getDispatcher());
    }
}
