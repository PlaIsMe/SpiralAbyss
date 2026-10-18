package com.pla.spiral_abyss;

import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.init.*;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SpiralAbyss.MOD_ID)
public class SpiralAbyss {
    public static final String MOD_ID = "spiral_abyss";

    public SpiralAbyss(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        GearConfig.prepare();
        context.registerConfig(ModConfig.Type.COMMON,
                AbyssConfig.SPEC, "spiral_abyss/spiral_abyss-common.toml");
        EntityInit.ENTITY_TYPES.register(modEventBus);
        BlockInit.BLOCKS.register(modEventBus);
        BlockEntityInit.BLOCK_ENTITIES.register(modEventBus);
        LootInit.MODIFIERS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        CreativeTabInit.TABS.register(modEventBus);
        SoundInit.SOUNDS.register(modEventBus);
        AbyssNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandInit.register(event.getDispatcher());
    }
}
