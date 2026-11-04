package com.pla.spiral_abyss.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Dim only the visible world around the opaque journal page. */
final class JournalWorldBackdrop {
    private JournalWorldBackdrop() {}

    static void render(GuiGraphics graphics, int width, int height, int panelX, int panelY, int panelWidth, int panelHeight) {
        int color = Minecraft.getInstance().level == null ? 0xFF101A2A : 0x8010192B;
        // The world stays live. Avoid blending a fullscreen layer that is then
        // overwritten by the opaque page: only its four margins are visible.
        graphics.drawManaged(() -> {
            graphics.fill(0, 0, width, panelY, color);
            graphics.fill(0, panelY, panelX, panelY + panelHeight, color);
            graphics.fill(panelX + panelWidth, panelY, width, panelY + panelHeight, color);
            graphics.fill(0, panelY + panelHeight, width, height, color);
        });
    }
}
