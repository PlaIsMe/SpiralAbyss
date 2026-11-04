package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;

/** Keyboard-focusable navigation for the Overworld journal only. */
public final class JournalNavigation {
    public static final ResourceLocation LOGO = new ResourceLocation(SpiralAbyss.MOD_ID, "textures/item/creative_tab_icon.png");
    private static final ResourceLocation HELP = texture("help");
    public static final ResourceLocation MENU_ICON = texture("menu_icon");
    private JournalNavigation() {}
    public static ResourceLocation texture(String name) {
        return new ResourceLocation(SpiralAbyss.MOD_ID, "textures/gui/journal/" + name + ".png");
    }
    public static void add(Consumer<Button> add, int width, int selected,
                           Runnable banner, Runnable help, Runnable close) {
        int count = 2, size = 44, gap = 8;
        int left = (width - count * size - (count - 1) * gap) / 2;
        add.accept(icon(left, 6, size, 28, LOGO, 32, "screen.spiral_abyss.journal.banner", selected == 0, banner));
        add.accept(icon(left + size + gap, 6, size, 28, HELP, 513, "screen.spiral_abyss.journal.help", selected == 1, help));
        Button exit = new Button(width - 28, 8, 20, 20, Component.literal("×"), b -> close.run(), n -> n.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                g.fill(getX(), getY(), getX() + 20, getY() + 20, isHoveredOrFocused() ? 0xDD746178 : 0xAA182036);
                g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + 10, getY() + 6, 0xFFF3E5C9);
            }
        };
        exit.setTooltip(Tooltip.create(Component.translatable("screen.spiral_abyss.close")));
        add.accept(exit);
    }
    public static Button icon(int x, int y, int w, int h, ResourceLocation texture, int sourceSize,
                              String label, boolean selected, Runnable action) {
        Button result = new Button(x, y, w, h, Component.translatable(label), b -> action.run(), n -> n.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        selected ? 0xDD695C85 : isHoveredOrFocused() ? 0xDD414466 : 0xC021273C);
                g.renderOutline(getX(), getY(), getWidth(), getHeight(), selected || isHoveredOrFocused() ? 0xFFE5C98D : 0xFF6C718C);
                int size = getHeight() - 4;
                RenderSystem.enableBlend();
                g.blit(texture, getX() + (getWidth() - size) / 2, getY() + 2, size, size,
                        0, 0, sourceSize, sourceSize, sourceSize, sourceSize);
                RenderSystem.disableBlend();
            }
        };
        result.setTooltip(Tooltip.create(result.getMessage()));
        return result;
    }
}
