package com.pla.spiral_abyss.fabric;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.boss_enviroment.BossEnvironmentCatalog;
import com.pla.spiral_abyss.config.*;
import com.pla.spiral_abyss.encounter.*;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import com.pla.spiral_abyss.encounter.data.*;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.init.*;
import com.pla.spiral_abyss.platform.event.AbyssEvents.*;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.config.ModConfig;

import java.util.concurrent.*;

public final class SpiralAbyssFabric implements ModInitializer {
    private static MinecraftServer server;

    public static MinecraftServer server() {
        return server;
    }

    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(
                SpiralAbyss.MOD_ID,
                ModConfig.Type.COMMON,
                AbyssConfig.SPEC,
                "spiral_abyss/spiral_abyss-common.toml");
        ForgeConfigRegistry.INSTANCE.register(
                SpiralAbyss.MOD_ID,
                ModConfig.Type.CLIENT,
                AbyssClientConfig.SPEC,
                "spiral_abyss/spiral_abyss-client.toml");
        SpiralAbyss.init();
        FabricInteractions.register();
        FabricDefaultAttributeRegistry.register(
                EntityInit.KEY_ENTITY.get(), KeyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                EntityInit.COPPER_CHEST.get(), KeyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                EntityInit.IRON_CHEST.get(), KeyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                EntityInit.GOLDEN_CHEST.get(), KeyEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                EntityInit.EXPERIENCE_CHEST.get(), KeyEntity.createAttributes());
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, selection) -> CommandInit.register(dispatcher));
        registerReload("mobs", new MobCatalog());
        registerReload("floors", new FloorCatalog());
        registerReload("boss_environments", new BossEnvironmentCatalog());
        registerReload("challenges", new ChallengeCatalog());
        ServerLifecycleEvents.SERVER_STARTING.register(value -> server = value);
        ServerLifecycleEvents.SERVER_STARTED.register(
                value -> FabricGameplayHooks.post(new ServerStartedEvent(value)));
        ServerLifecycleEvents.SERVER_STOPPING.register(
                value -> FabricGameplayHooks.post(new ServerStoppingEvent(value)));
        ServerLifecycleEvents.SERVER_STOPPED.register(
                value -> {
                    FabricGameplayHooks.post(new ServerStoppedEvent(value));
                    server = null;
                });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (value, resources, success) -> {
                    if (success) {
                        FloorRewards.resetMissingTableLogs();
                        FloorRewards.validateTables(value);
                    }
                });
        ServerTickEvents.END_SERVER_TICK.register(
                value ->
                        FabricGameplayHooks.post(
                                new TickEvent.ServerTickEvent(value, TickEvent.Phase.END)));
        ServerTickEvents.END_WORLD_TICK.register(
                level ->
                        FabricGameplayHooks.post(
                                new TickEvent.LevelTickEvent(level, TickEvent.Phase.END)));
        ServerWorldEvents.UNLOAD.register(
                (value, level) -> FabricGameplayHooks.post(new LevelEvent.Unload(level)));
        ServerChunkEvents.CHUNK_LOAD.register(
                (level, chunk) -> FabricGameplayHooks.post(new ChunkEvent.Load(level, chunk)));
        ServerEntityEvents.ENTITY_UNLOAD.register(
                (entity, level) ->
                        FabricGameplayHooks.post(new EntityLeaveLevelEvent(entity, level)));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, value) ->
                        FabricGameplayHooks.post(
                                new PlayerEvent.PlayerLoggedInEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, value) ->
                        FabricGameplayHooks.post(
                                new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
    }

    private static void registerReload(String id, PreparableReloadListener listener) {
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(
                        new IdentifiableResourceReloadListener() {
                            @Override
                            public ResourceLocation getFabricId() {
                                return new ResourceLocation(SpiralAbyss.MOD_ID, id);
                            }

                            @Override
                            public CompletableFuture<Void> reload(
                                    PreparationBarrier barrier,
                                    ResourceManager resources,
                                    ProfilerFiller prepare,
                                    ProfilerFiller apply,
                                    Executor prepareExecutor,
                                    Executor applyExecutor) {
                                return listener.reload(
                                        barrier,
                                        resources,
                                        prepare,
                                        apply,
                                        prepareExecutor,
                                        applyExecutor);
                            }
                        });
    }
}
