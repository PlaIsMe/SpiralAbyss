package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

/** Six centered texture cards; selectable floors come exclusively from the server. */
public final class FloorSelectionScreen extends Screen {
    private static final ResourceLocation BACKGROUND = texture("background");
    private static final ResourceLocation EARTH = texture("earth_image");
    private final AbyssNetwork.OpenFloors state;
    private boolean replied;
    private final long openedAt = net.minecraft.Util.getMillis();
    private int resetRowY;
    public static void closeForReset() {
        if (Minecraft.getInstance().screen instanceof FloorSelectionScreen screen) {
            screen.replied = true; screen.onClose();
        }
    }
    public FloorSelectionScreen(AbyssNetwork.OpenFloors state) {
        super(Component.translatable("screen.spiral_abyss.floors")); this.state = state;
    }
    public static void open(AbyssNetwork.OpenFloors state) { Minecraft.getInstance().setScreen(new FloorSelectionScreen(state)); }
    private static ResourceLocation texture(String name) { return new ResourceLocation(SpiralAbyss.MOD_ID, "textures/gui/floors/" + name + ".png"); }
    @Override protected void init() {
        int gap = Math.max(2, Math.min(12, width / 80));
        int count = Integer.bitCount(state.visible() & 126);
        int cardWidth = Math.max(1, Math.min((width - 24 - 5 * gap) / 6, (height - 132) * 256 / 455));
        int cardHeight = cardWidth * 455 / 256;
        int left = (width - count * cardWidth - Math.max(0, count - 1) * gap) / 2;
        int top = (height - cardHeight) / 2 - 12;
        int index = 0;
        for (int floor = 1; floor <= 6; floor++) {
            if ((state.visible() & (1 << floor)) == 0) continue;
            final int number = floor;
            boolean claimed = (state.claimed() & (1 << floor)) != 0;
            boolean selectable = (state.selectable() & (1 << floor)) != 0;
            String suffix = claimed ? "_looted" : selectable || floor == 1 ? "" : "_locked";
            Component label = Component.translatable("screen.spiral_abyss.floor", floor);
            ResourceLocation icon = texture("floor_" + floor + suffix);
            var card = new Button(left + index++ * (cardWidth + gap), top, cardWidth, cardHeight,
                    label, button -> submit(number), narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    RenderSystem.enableBlend();
                    graphics.blit(icon, getX(), getY(), getWidth(), getHeight(), 0, 0, 256, 455, 256, 455);
                    if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), active ? 0xFFF2D58C : 0xFF718292);
                    RenderSystem.disableBlend();
                }
            };
            card.active = selectable;
            card.setTooltip(Tooltip.create(Component.translatable(claimed ? "screen.spiral_abyss.claimed"
                    : selectable ? "screen.spiral_abyss.available" : "screen.spiral_abyss.locked")));
            addRenderableWidget(card);
        }
        int buttonY = Math.min(height - 60, top + cardHeight + 15);
        resetRowY = buttonY + 37;
        Component exitLabel = Component.translatable(state.inside()
                ? "screen.spiral_abyss.return_overworld" : "screen.spiral_abyss.close");
        var exit = new Button(width / 2 - 16, buttonY, 32, 32,
                exitLabel, button -> submit(state.inside() ? -1 : 0), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                RenderSystem.enableBlend();
                graphics.blit(EARTH, getX(), getY() + 3, getWidth(), 26, 0, 0, 564, 468, 564, 468);
                if (isHoveredOrFocused()) graphics.renderOutline(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 0xFFF2D58C);
                RenderSystem.disableBlend();
            }
        };
        exit.setTooltip(Tooltip.create(exitLabel));
        addRenderableWidget(exit);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = Math.max(width / 2560F, height / 1440F);
        int imageWidth = (int) Math.ceil(2560 * scale), imageHeight = (int) Math.ceil(1440 * scale);
        graphics.blit(BACKGROUND, (width - imageWidth) / 2, (height - imageHeight) / 2, imageWidth, imageHeight, 0, 0, 2560, 1440, 2560, 1440);
        graphics.fill(0, 0, width, height, 0x55081529);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFF3D7);
        int hintY = 27;
        for (var line : font.split(state.hint(), Math.max(1, width - 24))) {
            graphics.drawCenteredString(font, line, width / 2, hintY, 0xD8E9FA); hintY += 10;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        long remaining = state.resetRemainingMillis() < 0 ? -1
                : Math.max(0, state.resetRemainingMillis() - (net.minecraft.Util.getMillis() - openedAt));
        com.pla.spiral_abyss.client.overlay.AbyssClockRenderer.reset(graphics, font, width / 2, resetRowY, width - 24, remaining);
    }
    private void submit(int floor) {
        if (replied) return;
        replied = true; AbyssNetwork.select(state.token(), floor); onClose();
    }
    @Override public void removed() {
        if (!replied) { replied = true; AbyssNetwork.select(state.token(), 0); }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }
}
