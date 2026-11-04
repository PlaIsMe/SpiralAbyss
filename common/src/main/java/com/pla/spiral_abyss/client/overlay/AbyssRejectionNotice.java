package com.pla.spiral_abyss.client.overlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.pla.spiral_abyss.platform.AbyssRegistries;

import java.util.List;

/** Small native pixel warning shared by the wave portraits and encounter HUD. */
public final class AbyssRejectionNotice {
    public static final int ICON_SIZE = 11;
    private static final int RED = 0xFFFF655E;
    private AbyssRejectionNotice() {}

    public static void icon(GuiGraphics graphics, int x, int y) {
        // A stepped outline triangle; integer pixels stay crisp at every GUI scale.
        for (int row = 0; row < 9; row++) {
            int spread = row / 2;
            graphics.fill(x + 5 - spread, y + row + 1, x + 6 + spread, y + row + 2, RED);
            if (row >= 3 && row < 8)
                graphics.fill(x + 6 - spread, y + row + 1, x + 5 + spread, y + row + 2, 0xFF171724);
        }
        graphics.fill(x + 5, y + 4, x + 6, y + 6, RED);
        graphics.fill(x + 5, y + 7, x + 6, y + 8, RED);
    }
    public static Component tooltip(Component boss) {
        return Component.translatable("rejection.spiral_abyss.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal("\n").withStyle(ChatFormatting.RESET))
                .append(Component.translatable("rejection.spiral_abyss.tooltip", boss)
                        .withStyle(ChatFormatting.GRAY).withStyle(style -> style.withBold(false)));
    }
    public static void panel(GuiGraphics graphics, Font font, int x, int y, int availableWidth, int availableHeight,
                             List<ResourceLocation> bosses) {
        if (bosses.isEmpty() || availableHeight <= 0) return;
        Component title = Component.translatable("rejection.spiral_abyss.title").withStyle(ChatFormatting.BOLD);
        Component quote = Component.translatable("rejection.spiral_abyss.quote").withStyle(ChatFormatting.ITALIC);
        Component damage = Component.translatable("rejection.spiral_abyss.damage");
        var names = Component.empty();
        for (ResourceLocation id : bosses) {
            var type = AbyssRegistries.ENTITY_TYPES.getValue(id);
            if (!names.getString().isEmpty()) names.append(Component.literal(", "));
            names.append(type == null ? Component.literal(id.toString()) : type.getDescription());
        }
        // Fit the longest visible line, retaining only the text/icon padding.
        int contentWidth = Math.max(font.width(title) + 31,
                Math.max(font.width(quote), Math.max(font.width(damage), font.width(names))) + 16);
        int width = Math.min(Math.min(300, availableWidth), contentWidth);
        var quoteLines = font.split(quote, width - 16);
        var nameLines = font.split(names, width - 16);
        int height = 28 + (quoteLines.size() + nameLines.size() + 1) * (font.lineHeight + 2);
        float scale = Math.min(0.9F, (float) availableHeight / height);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0); graphics.pose().scale(scale, scale, 1);
        // Disjoint strips keep the whole background at 31% opacity, including the center.
        graphics.fill(2, 0, width - 2, 2, 0x5010101B);
        graphics.fill(0, 2, width, height - 2, 0x5010101B);
        graphics.fill(2, height - 2, width - 2, height, 0x5010101B);
        icon(graphics, 7, 6);
        graphics.drawString(font, title, 23, 8, RED, false);
        int lineY = 24;
        for (var line : quoteLines) { graphics.drawString(font, line, 8, lineY, 0xFFE1E4F5, false); lineY += font.lineHeight + 2; }
        graphics.drawString(font, damage, 8, lineY + 2, 0xFFFFAC87, false);
        lineY += font.lineHeight + 6;
        for (var line : nameLines) { graphics.drawString(font, line, 8, lineY, 0xFFAAA5B8, false); lineY += font.lineHeight + 2; }
        graphics.pose().popPose();
    }
}
