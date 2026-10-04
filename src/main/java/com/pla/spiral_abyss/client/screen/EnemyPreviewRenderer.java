package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import java.util.*;

/** Client-only, unspawned model instances. The server supplies every species and count. */
final class EnemyPreviewRenderer {
    private final Map<ResourceLocation, LivingEntity> entities = new HashMap<>();
    private final Set<ResourceLocation> unavailable = new HashSet<>();
    Component name(ResourceLocation id) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        return type == null ? Component.literal(id.toString()) : type.getDescription();
    }
    void clear() { entities.clear(); unavailable.clear(); }
    void render(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || unavailable.contains(id)) { fallback(graphics, x, y, width, height); return; }
        LivingEntity entity = entities.get(id);
        if (entity == null) {
            try {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                var created = type == null ? null : type.create(client.level);
                if (!(created instanceof LivingEntity living)) { unavailable.add(id); fallback(graphics, x, y, width, height); return; }
                living.setYRot(180); living.yBodyRot = 180; living.yHeadRot = living.yHeadRotO = 180;
                entity = living; entities.put(id, living);
            } catch (RuntimeException exception) {
                unavailable.add(id); LogUtils.getLogger().warn("Cannot create Abyss enemy preview {}", id, exception);
                fallback(graphics, x, y, width, height); return;
            }
        }
        float modelWidth = Math.max(0.5F, entity.getBbWidth() * 1.35F), modelHeight = Math.max(0.5F, entity.getBbHeight() * 1.15F);
        float scale = Math.min((width - 4) / modelWidth, (height - 4) / modelHeight);
        var dispatcher = client.getEntityRenderDispatcher();
        Quaternionf previousCamera = new Quaternionf(dispatcher.cameraOrientation());
        graphics.pose().pushPose();
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            graphics.pose().translate(x + width / 2F, y + height - 3, 100);
            graphics.pose().scale(scale, scale, -scale);
            graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(15)));
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(new Quaternionf()); dispatcher.setRenderShadow(false);
            LivingEntity model = entity;
            RenderSystem.runAsFancy(() -> dispatcher.render(model, 0, 0, 0, 0, 1, graphics.pose(), graphics.bufferSource(), 15728880));
            graphics.flush();
        } catch (RuntimeException exception) {
            unavailable.add(id); LogUtils.getLogger().warn("Cannot render Abyss enemy preview {}", id, exception);
        } finally {
            dispatcher.overrideCameraOrientation(previousCamera); dispatcher.setRenderShadow(true);
            graphics.pose().popPose(); graphics.disableScissor(); Lighting.setupFor3DItems();
        }
        if (unavailable.contains(id)) fallback(graphics, x, y, width, height);
    }
    private void fallback(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("?"), x + width / 2, y + height / 2, 0xFFD8E9FA);
    }
}
