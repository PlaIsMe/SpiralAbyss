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
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EntityInit.ENTITY_TYPES.register(modBus);
        BlockInit.BLOCKS.register(modBus);
        ItemInit.ITEMS.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandInit.register(event.getDispatcher());
    }
}
