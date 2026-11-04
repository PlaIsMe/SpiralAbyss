package com.pla.spiral_abyss.fabric.client;

import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.overlay.*;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents.*;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import com.pla.spiral_abyss.client.renderer.*;
import com.pla.spiral_abyss.init.*;
import com.pla.spiral_abyss.world.AbyssDimension;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.BlockHitResult;

public final class SpiralAbyssFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(JournalClientHandler.OPEN);
        EntityRendererRegistry.register(EntityInit.KEY_ENTITY.get(), KeyEntityRenderer::new);
        BlockEntityRendererRegistry.register(
                BlockEntityInit.ABYSS_PORTAL.get(), AbyssPortalRenderer::new);
        EntityRendererRegistry.register(EntityInit.COPPER_CHEST.get(), LootChestRenderer::copper);
        EntityRendererRegistry.register(EntityInit.IRON_CHEST.get(), LootChestRenderer::iron);
        EntityRendererRegistry.register(EntityInit.GOLDEN_CHEST.get(), LootChestRenderer::golden);
        EntityRendererRegistry.register(EntityInit.CHEST_LOOT.get(), ChestLootRenderer::new);
        EntityRendererRegistry.register(
                EntityInit.EXPERIENCE_CHEST.get(), ExperienceChestRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.KEY_ENTITY_LAYER, KeyEntityModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(
                ExperienceChestModel.LAYER_LOCATION, ExperienceChestModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(
                CopperChestModel.LAYER_LOCATION, CopperChestModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(
                IronChestModel.LAYER_LOCATION, IronChestModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(
                GoldenChestModel.LAYER_LOCATION, GoldenChestModel::createBodyLayer);
        BlockRenderLayerMap.INSTANCE.putBlocks(
                RenderType.cutout(),
                BlockInit.ABYSS_GATE_VERTICAL.get(),
                BlockInit.ABYSS_GATE_HORIZONTAL.get(),
                BlockInit.ABYSS_GATE_CURVE.get(),
                BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get());
        AbyssSkyEffects sky = new AbyssSkyEffects();
        DimensionRenderingRegistry.registerDimensionEffects(AbyssSkyEffects.ID, sky);
        DimensionRenderingRegistry.registerSkyRenderer(
                AbyssDimension.LEVEL,
                context ->
                        sky.renderSky(
                                context.world(),
                                0,
                                context.tickDelta(),
                                context.matrixStack(),
                                context.camera(),
                                context.projectionMatrix(),
                                false,
                                () -> {}));
        DimensionRenderingRegistry.registerCloudRenderer(AbyssDimension.LEVEL, context -> {});
        DimensionRenderingRegistry.registerWeatherRenderer(AbyssDimension.LEVEL, context -> {});
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    var event = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
                    JournalClientHandler.tick(event);
                    RareRewardOverlay.tick(event);
                });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    var event = new ClientPlayerNetworkEvent.LoggingOut();
                    AbyssEncounterOverlay.logout(event);
                    RareRewardOverlay.logout(event);
                    PortalEntryAnimation.logout(event);
                });
        ScreenEvents.AFTER_INIT.register(
                (client, screen, width, height) -> {
                    JournalClientHandler.inventory(
                            new ScreenInit(
                                    screen, widget -> Screens.getButtons(screen).add(widget)));
                    ScreenEvents.beforeRender(screen)
                            .register(
                                    (current, graphics, x, y, partialTick) ->
                                            JournalClientHandler.renderInventory(
                                                    new ScreenEvent.Render.Pre(current)));
                    ScreenKeyboardEvents.allowKeyPress(screen)
                            .register(
                                    (current, key, scan, modifiers) -> {
                                        var event =
                                                new ScreenEvent.KeyPressed.Pre(current, key, scan);
                                        JournalClientHandler.key(event);
                                        return !event.isCanceled();
                                    });
                });
        HudRenderCallback.EVENT.register(
                (graphics, partialTick) -> {
                    if (PortalEntryAnimation.active()) return;
                    var window = Minecraft.getInstance().getWindow();
                    AbyssEncounterOverlay.HUD.render(
                            null,
                            graphics,
                            partialTick,
                            window.getGuiScaledWidth(),
                            window.getGuiScaledHeight());
                    RareRewardOverlay.HUD.render(
                            null,
                            graphics,
                            partialTick,
                            window.getGuiScaledWidth(),
                            window.getGuiScaledHeight());
                });
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(
                (context, hit) -> {
                    if (!(hit instanceof BlockHitResult block)) return true;
                    var event = new RenderHighlightEvent.Block(block);
                    PortalEntryAnimation.outline(event);
                    return !event.isCanceled();
                });
    }
}
