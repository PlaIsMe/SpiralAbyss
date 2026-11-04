package com.pla.spiral_abyss.client.overlay;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.LayeredDraw;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Rare pickup announcements using FancyToasts' StandardAnimation phases and terracraft atlas.
 * Adapted from FancyToasts, Copyright (c) 2026 Bivrik, MIT; see META-INF/licenses/FancyToasts.txt.
 */
public final class RareRewardOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            SpiralAbyss.MOD_ID, "textures/gui/toast/terracraft.png");
    private static final Component TITLE = Component.translatable("toast.spiral_abyss.rare_reward.title");
    private static final int DURATION = 150;
    private static final Deque<Reward> QUEUE = new ArrayDeque<>();
    private static Reward current;
    private static int ticks;

    private RareRewardOverlay() {}

    /** Called on the client game thread by the server-to-client pickup notification. */
    public static void enqueue(Component player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack icon = stack.copyWithCount(1);
        Component item = Component.literal("[").append(singleLine(icon.getHoverName(), ChatFormatting.GOLD))
                .append("]").withStyle(ChatFormatting.GOLD);
        Component description = singleLine(Component.translatable("toast.spiral_abyss.rare_reward.description",
                singleLine(player, ChatFormatting.AQUA), item), null);
        QUEUE.addLast(new Reward(icon, description));
    }

    /** Flatten translated/styled spans while removing explicit line breaks and enforcing name colors. */
    private static Component singleLine(Component text, ChatFormatting color) {
        var result = Component.empty();
        text.visit((style, value) -> {
            result.append(Component.literal(value.replace('\n', ' ').replace('\r', ' '))
                    .setStyle(color == null ? style : style.withColor(color)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public static void tick(ClientTickEvent.Post event) {

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            clear();
            return;
        }
        if (client.isPaused() || client.options.hideGui) return;
        if (current != null && ++ticks >= DURATION) current = null;
        if (current == null && !QUEUE.isEmpty()) {
            current = QUEUE.removeFirst();
            ticks = 0;
        }
    }

    private static void clear() {
        QUEUE.clear();
        current = null;
        ticks = 0;
    }

    public static final LayeredDraw.Layer HUD = (graphics, deltaTracker) -> {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        Minecraft client = Minecraft.getInstance();
        if (current == null || client.options.hideGui || client.player == null || client.level == null) return;
        render(graphics, client.font, width, height, ticks + partialTick, current);
    };

    private static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight,
                               float time, Reward reward) {
        int width = Mth.clamp(font.width(reward.description()) + 24, 240, 360);
        float scale = Math.min(1F, Math.max(1, screenWidth - 16) / (float) width);
        float top = screenHeight * 0.15F;
        float exit = easeIn(progress(time, 110, 40));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate((screenWidth - width * scale) / 2F, top - exit * (top + 80 * scale), 200);
        pose.scale(scale, scale, 1);
        RenderSystem.enableBlend();
        try {
            // The background drops into place, then the gold banner expands behind the item frame.
            if (time > 31) {
                pose.pushPose();
                pose.translate(0, -200 * (1 - easeOut(progress(time, 31, 20))), 0);
                strip(graphics, width, 20, 40, 0, 5);
                graphics.blit(TEXTURE, width - 18, 54, 0, 108, 9, 14, 256, 256);
                pose.popPose();
            }
            if (time > 32) {
                pose.pushPose();
                scaleAround(graphics, easeOut(progress(time, 32, 14)), 1, width / 2F, 0);
                strip(graphics, width, 5, 14, 68, 24);
                pose.popPose();
            }
            // Preserve the reference's elastic arrival and gentle floating icon.
            float iconProgress = progress(time, 0, 40);
            float iconScale = easeOut(iconProgress);
            if (iconScale > 0.001F) {
                pose.pushPose();
                scaleAround(graphics, iconScale, iconScale, width / 2F, 13);
                pose.translate(0, -40 * (1 - elasticOut(iconProgress))
                        + (float) Math.sin(time * 0.1F) * 1.5F - 5, 0);
                graphics.blit(TEXTURE, width / 2 - 13, 0, 52, 82, 26, 26, 256, 256);
                graphics.renderFakeItem(reward.icon(), width / 2 - 8, 5);
                pose.popPose();
            }
            if (time > 40) {
                // Minecraft treats tiny alpha values as opaque; skip those first near-transparent frames.
                int alpha = Math.round(progress(time, 40, 20) * 255);
                if (alpha > 3) {
                    fittedLine(graphics, font, TITLE, width, 25, alpha << 24 | 0xFFAA00);
                    description(graphics, font, reward.description(), width, alpha << 24 | 0xFFFFFF);
                }
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            pose.popPose();
        }
    }

    /** Stretch only the center of each atlas strip, preserving the frame's pixel-sized end caps. */
    private static void strip(GuiGraphics graphics, int width, int y, int height, int v, int edge) {
        graphics.blit(TEXTURE, 0, y, 0, v, edge, height, 256, 256);
        graphics.blit(TEXTURE, edge, y, width - edge * 2, height,
                edge, v, 162 - edge * 2, height, 256, 256);
        graphics.blit(TEXTURE, width - edge, y, 162 - edge, v, edge, height, 256, 256);
    }

    private static void fittedLine(GuiGraphics graphics, Font font, Component text, int width, int y, int color) {
        float scale = Math.min(1F, (width - 20F) / Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private static void description(GuiGraphics graphics, Font font, Component text, int width, int color) {
        float scale = 1F;
        var lines = font.split(text, width - 20);
        // Long custom item/player names still fit in the two available lines without being omitted.
        for (int attempt = 0; lines.size() > 2 && attempt < 12; attempt++) {
            scale *= 0.9F;
            lines = font.split(text, (int) ((width - 20) / scale));
        }
        if (lines.size() > 2) {
            fittedLine(graphics, font, text, width, 42, color);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2F, lines.size() == 1 ? 42 : 38, 0);
        graphics.pose().scale(scale, scale, 1);
        for (int line = 0; line < lines.size(); line++)
            graphics.drawCenteredString(font, lines.get(line), 0, line * font.lineHeight, color);
        graphics.pose().popPose();
    }

    private static void scaleAround(GuiGraphics graphics, float x, float y, float centerX, float centerY) {
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(x, y, 1);
        graphics.pose().translate(-centerX, -centerY, 0);
    }

    private static float progress(float time, int start, int duration) {
        return Mth.clamp((time - start) / duration, 0, 1);
    }

    private static float easeIn(float progress) {
        float squared = progress * progress;
        float fourth = squared * squared;
        return fourth * fourth;
    }

    private static float easeOut(float progress) { return 1 - easeIn(1 - progress); }

    private static float elasticOut(float progress) {
        if (progress == 0 || progress == 1) return progress;
        return (float) (Math.pow(2, -10 * progress)
                * Math.sin((progress * 10 - 0.75F) * Math.PI / 3) + 1);
    }

    private record Reward(ItemStack icon, Component description) {}
}
