package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.client.overlay.AbyssClockRenderer;
import com.pla.spiral_abyss.client.overlay.AbyssRejectionNotice;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/** One menu and authorization token; detail navigation never submits or cancels a selection. */
public final class FloorSelectionScreen extends Screen {
    private static final ResourceLocation BACKGROUND = texture("background"), EARTH = texture("earth_image");
    private static final ItemStack STAR = new ItemStack(Items.NETHER_STAR);
    private static final float ARTWORK_BUTTON_SCALE = 0.5F;
    private static final float WAVE_BUTTON_SCALE = 0.8F;
    private final AbyssNetwork.OpenFloors state;
    private final EnemyPreviewRenderer previews = new EnemyPreviewRenderer();
    private final long openedAt = net.minecraft.Util.getMillis();
    private boolean replied;
    private int keepOpenTicks;
    private int selectedFloor, selectedWave, resetRowY;
    private int panelX, panelY, panelWidth, panelHeight, gridTop, gridBottom, scroll, maxScroll, waveContentY, maxChallengeLines;
    private RewardPreviewPanel rewardPanel;

    public static void closeForReset() {
        com.pla.spiral_abyss.client.portal.PortalEntryAnimation.closeForReset();
        if (Minecraft.getInstance().screen instanceof FloorSelectionScreen screen) {
            screen.replied = true; screen.minecraft.setScreen(null);
        }
        else if (Minecraft.getInstance().screen instanceof AbyssJournalScreen) Minecraft.getInstance().setScreen(null);
    }
    public FloorSelectionScreen(AbyssNetwork.OpenFloors state) {
        super(Component.translatable("screen.spiral_abyss.floors")); this.state = state;
    }
    public static void open(AbyssNetwork.OpenFloors state) { Minecraft.getInstance().setScreen(new FloorSelectionScreen(state)); }
    private static ResourceLocation texture(String name) { return ResourceLocation.fromNamespaceAndPath(SpiralAbyss.MOD_ID, "textures/gui/floors/" + name + ".png"); }
    private boolean selectable(int floor) { return (state.selectable() & (1 << floor)) != 0; }
    private ResourceLocation floorTexture(int floor) {
        String suffix = (state.claimed() & (1 << floor)) != 0 ? "_looted"
                : (state.unlocked() & (1 << floor)) != 0 ? "" : "_locked";
        return texture("floor_" + floor + suffix);
    }
    private AbyssNetwork.FloorPreview floor() {
        return state.floors().stream().filter(f -> f.floor() == selectedFloor).findFirst().orElse(null);
    }
    private AbyssNetwork.WavePreview wave(int number) {
        var floor = floor();
        return floor == null ? null : floor.waves().stream().filter(w -> w.wave() == number).findFirst().orElse(null);
    }
    private void navigate(int floor, int wave) {
        selectedFloor = floor; selectedWave = wave; scroll = 0; setFocused(null); clearWidgets(); init();
    }
    @Override protected void init() {
        rewardPanel = null;
        if (selectedFloor == 0) initOverview();
        else if (selectedFloor == 7) initEndless();
        else if (selectedWave == 0) initFloor();
        else initWave();
    }
    @Override public void tick() {
        super.tick();
        // All three pages share a token. Reading previews must not expire travel.
        if (!replied && keepOpenTicks-- <= 0) {
            AbyssNetwork.keepFloorsOpen(state.token());
            keepOpenTicks = 199;
        }
    }
    private void initOverview() {
        int gap = Math.max(2, Math.min(12, width / 80));
        int count = Integer.bitCount(state.visible() & 254);
        int cardWidth = Math.max(1, Math.min((width - 24 - 6 * gap) / 7, Math.max(1, height - 132) * 256 / 455));
        int cardHeight = cardWidth * 455 / 256;
        int left = (width - count * cardWidth - Math.max(0, count - 1) * gap) / 2;
        int top = Math.max(48, (height - cardHeight) / 2 - 12), index = 0;
        for (int floor = 1; floor <= 7; floor++) {
            if ((state.visible() & (1 << floor)) == 0) continue;
            final int number = floor;
            int x = left + index++ * (cardWidth + gap);
            var label = Component.translatable(floor == 7 ? "screen.spiral_abyss.endless" : "screen.spiral_abyss.floor", floor);
            var card = floor == 7 ? addRenderableWidget(new Button(x, top, cardWidth, cardHeight, label,
                    pressed -> navigate(7, 0), narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    EndlessArtwork.render(graphics, getX(), getY(), getWidth(), getHeight());
                    if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFF2D58C);
                }
            }) : imageButton(x, top, cardWidth, cardHeight, label, floorTexture(floor), 256, 455, () -> navigate(number, 0));
            card.setTooltip(Tooltip.create(floor == 7 ? label : Component.translatable("screen.spiral_abyss.view_floor", floor)));
        }
        int buttonY = Math.min(height - 60, top + cardHeight + 15);
        resetRowY = buttonY + 37;
        Component exitLabel = Component.translatable(state.inside() ? "screen.spiral_abyss.return_overworld" : "screen.spiral_abyss.close");
        var exit = imageButton(width / 2 - 16, buttonY, 32, 27, exitLabel, EARTH, 564, 468, () -> submit(state.inside() ? -1 : 0));
        exit.setTooltip(Tooltip.create(exitLabel));
    }
    private void layoutPanel() {
        panelWidth = Math.min(660, Math.max(120, width - 24));
        panelHeight = Math.min(360, Math.max(140, height - 24));
        panelX = (width - panelWidth) / 2; panelY = (height - panelHeight) / 2;
    }
    private void initEndless() {
        layoutPanel();
        int right = panelX + panelWidth / 3, rightWidth = panelWidth * 2 / 3 - 20;
        artworkButton(right, panelY + panelHeight - 29, 30, 15,
                Component.translatable("gui.back"), ButtonArtwork.BACK, false, () -> navigate(0, 0));
        var start = artworkButton(panelX + panelWidth - 100, panelY + panelHeight - 29, 80, 15,
                Component.translatable("screen.spiral_abyss.fight"), ButtonArtwork.FIGHT, false, () -> submit(7));
        start.active = selectable(7);
        if (!start.active) start.setTooltip(Tooltip.create(Component.translatable("screen.spiral_abyss.unavailable")));
    }
    private void renderEndless(GuiGraphics graphics) {
        int cardHeight = Math.min(panelHeight - 32, (panelWidth / 3 - 24) * 228 / 128);
        int cardWidth = cardHeight * 128 / 228;
        EndlessArtwork.render(graphics, panelX + 12, panelY + 12, cardWidth, cardHeight);
        int right = panelX + panelWidth / 3, textWidth = panelWidth * 2 / 3 - 20;
        int y = panelY + 18;
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.endless"), right, y, 0xFFF3D7, false);
        y += 22;
        graphics.drawString(font, Component.translatable("hud.spiral_abyss.endless_wave", state.endlessWave()), right, y, 0xFFFFFFFF, false);
        y += 22;
        for (var line : font.split(Component.translatable("screen.spiral_abyss.endless_description"), textWidth)) {
            if (y + font.lineHeight > panelY + panelHeight - 44) break;
            graphics.drawString(font, line, right, y, 0xFFD8E9FA, false); y += font.lineHeight + 3;
        }
    }
    private void initFloor() {
        layoutPanel();
        int cardHeight = Math.min(panelHeight - 48, (panelWidth / 4) * 455 / 256);
        int cardWidth = cardHeight * 256 / 455;
        int right = panelX + cardWidth + 32, rightWidth = panelWidth - cardWidth - 48;
        // Leave space for a full reward tile below the three waves on shorter windows.
        int rowHeight = Math.max(18, Math.min(38, (panelHeight - 148) / 3));
        int rowGap = 4;
        for (int i = 1; i <= 3; i++) {
            final int number = i;
            var row = new Button(right, panelY + 32 + (i - 1) * (rowHeight + rowGap), rightWidth, rowHeight,
                    Component.translatable("screen.spiral_abyss.wave", i), button -> navigate(selectedFloor, number), narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                            isHoveredOrFocused() ? 0xEE526680 : 0xDD394B62);
                    graphics.renderOutline(getX(), getY(), getWidth(), getHeight(),
                            isHoveredOrFocused() ? 0xFFF2D58C : 0xFF8B9AAD);
                    graphics.drawString(font, getMessage(), getX() + 12,
                            getY() + (getHeight() - font.lineHeight) / 2, 0xFFFFFFFF, false);
                    var preview = wave(number);
                    stars(graphics, getX() + getWidth() - 62, getY() + (getHeight() - 16) / 2, preview == null ? 0 : preview.stars());
                }
            };
            var preview = wave(number);
            row.setTooltip(Tooltip.create(Component.translatable("screen.spiral_abyss.wave_stars", preview == null ? 0 : preview.stars())));
            addRenderableWidget(row);
        }
        int rowsBottom = panelY + 32 + 3 * rowHeight + 2 * rowGap;
        int rewardY = rowsBottom + 20;
        rewardPanel = new RewardPreviewPanel(floor() == null ? List.of() : floor().rewards(),
                right, rewardY, rightWidth, Math.max(22, panelY + panelHeight - 40 - rewardY));
        int actionHeight = 15;
        int actionY = panelY + panelHeight - 29;
        artworkButton(right, actionY, Math.max(20, rightWidth / 4), actionHeight,
                Component.translatable("gui.back"), ButtonArtwork.BACK, false, () -> navigate(0, 0));
        int fightWidth = ButtonArtwork.FIGHT.fittedWidth(Math.max(1, Math.round(rightWidth / 2F * ARTWORK_BUTTON_SCALE)), actionHeight);
        var start = artworkButton(right + rightWidth - fightWidth, actionY, fightWidth, actionHeight,
                Component.translatable("screen.spiral_abyss.fight"), ButtonArtwork.FIGHT, false, () -> submit(selectedFloor));
        start.active = selectable(selectedFloor);
        if (!start.active) start.setTooltip(Tooltip.create(Component.translatable("screen.spiral_abyss.unavailable")));
    }
    private void initWave() {
        layoutPanel();
        int tabWidth = (panelWidth - 32) / 3;
        int tabHeight = Math.round(WAVE_BUTTON_SCALE * Math.max(18, Math.min(36, (panelHeight - 120) / 4)));
        for (int i = 1; i <= 3; i++) {
            final int number = i;
            ButtonArtwork artwork = ButtonArtwork.WAVE;
            int imageWidth = artwork.fittedWidth(Math.max(1, Math.round(tabWidth * WAVE_BUTTON_SCALE)), tabHeight);
            var tab = artworkButton(panelX + 12 + (i - 1) * (tabWidth + 4) + (tabWidth - imageWidth) / 2,
                    panelY + 10, imageWidth, tabHeight, Component.translatable("screen.spiral_abyss.wave", i),
                    artwork, i == selectedWave, () -> navigate(selectedFloor, number));
            tab.active = i != selectedWave;
        }
        waveContentY = panelY + 20 + tabHeight;
        int availableRowHeight = Math.max(16, Math.min(34, (panelHeight - tabHeight - 94) / 3));
        maxChallengeLines = Math.max(1, (availableRowHeight - 4) / font.lineHeight);
        gridTop = waveContentY + 2 * font.lineHeight + 22;
        var preview = wave(selectedWave);
        if (preview != null)
            for (Component objective : preview.challenges()) gridTop += challengeRowHeight(objective);
        gridBottom = panelY + panelHeight - 44;
        int backHeight = Math.round(30 * ARTWORK_BUTTON_SCALE);
        artworkButton(panelX + 14, panelY + panelHeight - 38 + (30 - backHeight) / 2,
                Math.round(24 * ARTWORK_BUTTON_SCALE), backHeight,
                Component.translatable("gui.back"), ButtonArtwork.BACK, false, () -> navigate(selectedFloor, 0));
    }
    private Button artworkButton(int x, int y, int maxWidth, int maxHeight, Component label,
                                 ButtonArtwork artwork, boolean selected, Runnable action) {
        int width = artwork.fittedWidth(maxWidth, maxHeight), height = artwork.fittedHeight(width);
        var button = addRenderableWidget(new Button(x, y + (maxHeight - height) / 2, width, height, label,
                pressed -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                artwork.render(graphics, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), selected, active);
                if (artwork == ButtonArtwork.WAVE || artwork == ButtonArtwork.FIGHT) {
                    int textWidth = font.width(getMessage());
                    float scale = Math.min(1F, Math.min(getWidth() * 0.76F / Math.max(1, textWidth),
                            getHeight() * 0.65F / font.lineHeight));
                    graphics.pose().pushPose();
                    graphics.pose().translate(getX() + getWidth() / 2F, getY() + getHeight() / 2F, 0);
                    graphics.pose().scale(scale, scale, 1);
                    graphics.drawString(font, getMessage(), -textWidth / 2, -font.lineHeight / 2,
                            active || selected ? 0xFF1D2633 : 0xFFAAAAAA, false);
                    graphics.pose().popPose();
                }
            }
        });
        button.setTooltip(Tooltip.create(label));
        return button;
    }
    private Button imageButton(int x, int y, int w, int h, Component label, ResourceLocation icon, int sourceW, int sourceH, Runnable action) {
        return addRenderableWidget(new Button(x, y, w, h, label, button -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                RenderSystem.enableBlend();
                graphics.blit(icon, getX(), getY(), getWidth(), getHeight(), 0, 0, sourceW, sourceH, sourceW, sourceH);
                if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFF2D58C);
                RenderSystem.disableBlend();
            }
        });
    }
    private void stars(GuiGraphics graphics, int x, int y, int earned) {
        for (int i = 0; i < 3; i++) {
            graphics.renderItem(STAR, x + i * 19, y);
            if (i >= earned) {
                graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
                graphics.fill(x + i * 19, y, x + i * 19 + 16, y + 16, 0xCC29384D);
                graphics.pose().popPose();
            }
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = Math.max(width / 2560F, height / 1440F);
        int imageWidth = (int) Math.ceil(2560 * scale), imageHeight = (int) Math.ceil(1440 * scale);
        graphics.blit(BACKGROUND, (width - imageWidth) / 2, (height - imageHeight) / 2, imageWidth, imageHeight, 0, 0, 2560, 1440, 2560, 1440);
        graphics.fill(0, 0, width, height, 0x55081529);
        if (selectedFloor == 0) renderOverview(graphics);
        else {
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEE25354D);
            graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF738297);
            if (selectedFloor == 7) renderEndless(graphics);
            else if (selectedWave == 0) renderFloor(graphics);
            else renderWave(graphics, mouseX, mouseY);
        }
        boolean overRewardTooltip = rewardPanel != null && rewardPanel.popupContains(mouseX, mouseY);
        super.render(graphics, overRewardTooltip ? -1 : mouseX, overRewardTooltip ? -1 : mouseY, partialTick);
        if (rewardPanel != null) rewardPanel.render(graphics, mouseX, mouseY, width, height);
    }
    private void renderOverview(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFF3D7);
        long remaining = state.resetRemainingMillis() < 0 ? -1 : Math.max(0, state.resetRemainingMillis() - (net.minecraft.Util.getMillis() - openedAt));
        AbyssClockRenderer.reset(graphics, font, width / 2, resetRowY, width - 24, remaining);
    }
    private void renderFloor(GuiGraphics graphics) {
        int cardHeight = Math.min(panelHeight - 48, (panelWidth / 4) * 455 / 256), cardWidth = cardHeight * 256 / 455;
        RenderSystem.enableBlend();
        graphics.blit(floorTexture(selectedFloor), panelX + 12, panelY + 12, cardWidth, cardHeight, 0, 0, 256, 455, 256, 455);
        RenderSystem.disableBlend();
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.floor", selectedFloor), panelX + cardWidth + 32, panelY + 17, 0xFFFFFFFF, false);
    }
    private int challengeRowHeight(Component objective) {
        int lines = Math.min(maxChallengeLines, font.split(objective, Math.max(1, panelWidth - 52)).size());
        // Pack single-line objectives to the star's height; grow only for wrapped text.
        return Math.max(16, lines * font.lineHeight + 3);
    }
    private void renderWave(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.challenge"), panelX + 15, waveContentY, 0xFFFFFFFF, false);
        List<Component> objectives = wave(selectedWave) == null ? List.of() : wave(selectedWave).challenges();
        Component hoveredObjective = null;
        int challengeY = waveContentY + 2 * font.lineHeight;
        for (Component objective : objectives) {
            int rowHeight = challengeRowHeight(objective);
            graphics.renderItem(STAR, panelX + 14, challengeY - 3);
            var lines = font.split(objective, Math.max(1, panelWidth - 52));
            if (maxChallengeLines == 1) drawFitted(graphics, objective, panelX + 36, challengeY, panelWidth - 50, 0xFFFFFFFF);
            else for (int line = 0; line < Math.min(maxChallengeLines, lines.size()); line++)
                graphics.drawString(font, lines.get(line), panelX + 36, challengeY + line * font.lineHeight, 0xFFFFFFFF, false);
            if (mouseX >= panelX + 14 && mouseX < panelX + panelWidth - 14 && mouseY >= challengeY - 3 && mouseY < challengeY + rowHeight - 3)
                hoveredObjective = objective;
            challengeY += rowHeight;
        }
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.enemies"), panelX + 15, gridTop - 15, 0xFFFFFFFF, false);
        var preview = wave(selectedWave);
        List<AbyssNetwork.EnemyPreview> enemies = preview == null ? List.of() : preview.enemies();
        int columns = Math.max(1, (panelWidth - 30) / 86), tileWidth = (panelWidth - 30) / columns;
        int tileHeight = Math.max(36, Math.min(88, gridBottom - gridTop));
        int rows = (enemies.size() + columns - 1) / columns;
        maxScroll = Math.max(0, rows * tileHeight - Math.max(1, gridBottom - gridTop)); scroll = Math.min(scroll, maxScroll);
        Component hovered = hoveredObjective;
        boolean hoveredRejection = false;
        graphics.enableScissor(panelX + 10, gridTop, panelX + panelWidth - 10, Math.max(gridTop, gridBottom));
        for (int index = 0; index < enemies.size(); index++) {
            int x = panelX + 14 + index % columns * tileWidth, y = gridTop + index / columns * tileHeight - scroll;
            if (y + tileHeight < gridTop || y > gridBottom) continue;
            var enemy = enemies.get(index);
            graphics.fill(x, y, x + tileWidth - 4, y + tileHeight - 5, 0x6034455E);
            previews.render(graphics, enemy.id(), x + 4, y + 3, tileWidth - 12, tileHeight - 24);
            graphics.drawCenteredString(font, Component.translatable("screen.spiral_abyss.enemy_count", enemy.count()), x + (tileWidth - 4) / 2, y + tileHeight - 18, 0xFFFFFFFF);
            if (mouseX >= x && mouseX < x + tileWidth - 4 && mouseY >= Math.max(y, gridTop) && mouseY < Math.min(y + tileHeight - 5, gridBottom)) hovered = previews.name(enemy.id());
            if (enemy.rejected()) {
                int warningX = x + tileWidth - 19, warningY = y + 4;
                graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
                graphics.fill(warningX - 2, warningY - 2, warningX + 13, warningY + 13, 0xE610101B);
                AbyssRejectionNotice.icon(graphics, warningX, warningY);
                graphics.pose().popPose();
                if (mouseX >= warningX - 2 && mouseX < warningX + 13
                        && mouseY >= Math.max(warningY - 2, gridTop) && mouseY < Math.min(warningY + 13, gridBottom)) {
                    hovered = AbyssRejectionNotice.tooltip(previews.name(enemy.id()));
                    hoveredRejection = true;
                }
            }
        }
        graphics.disableScissor();
        if (enemies.isEmpty()) graphics.drawCenteredString(font, Component.translatable("screen.spiral_abyss.preview_unavailable"), width / 2, gridTop + 12, 0xD8E9FA);
        if (maxScroll > 0) {
            int track = Math.max(1, gridBottom - gridTop), thumb = Math.max(12, track * track / Math.max(1, rows * tileHeight));
            int y = gridTop + (track - thumb) * scroll / maxScroll;
            graphics.fill(panelX + panelWidth - 7, gridTop, panelX + panelWidth - 4, gridBottom, 0xFF35455B);
            graphics.fill(panelX + panelWidth - 7, y, panelX + panelWidth - 4, y + thumb, 0xFFB2C1D3);
        }
        if (hovered != null) {
            if (hoveredRejection) graphics.renderTooltip(font, font.split(hovered, Math.max(40, Math.min(280, width - 24))), mouseX, mouseY);
            else graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }
    private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int available, int color) {
        float scale = Math.min(1, (float) available / Math.max(1, font.width(text)));
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0); graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false); graphics.pose().popPose();
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (rewardPanel != null && rewardPanel.scroll(mouseX, mouseY, amount)) return true;
        if (selectedWave != 0 && mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= gridTop && mouseY < gridBottom) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (amount * 30))); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (rewardPanel != null && rewardPanel.popupContains(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void submit(int floor) {
        if (replied) return;
        replied = true; AbyssNetwork.select(state.token(), floor); minecraft.setScreen(null);
    }
    @Override public void onClose() {
        if (selectedWave != 0) navigate(selectedFloor, 0);
        else if (selectedFloor != 0) navigate(0, 0);
        else minecraft.setScreen(null);
    }
    @Override public void removed() {
        if (!replied) { replied = true; AbyssNetwork.select(state.token(), 0); }
        previews.clear();
    }
    @Override public boolean isPauseScreen() { return false; }
}
