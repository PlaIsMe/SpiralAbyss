package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay.Bounds;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.config.AbyssClientConfig.HudGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Local HUD editor. Sample content never replaces the server's encounter snapshot. */
public final class AbyssHudConfigScreen extends Screen {
    private static final int HANDLE_RADIUS = 6;
    private final Screen parent;
    private HudGroup selected = HudGroup.TIMER;
    private Button groupButton, countdownButton;
    private boolean countdownPreview, moving, scaling, pendingSave;
    private double grabX, grabY, initialScale, resizeX, resizeY;
    private int initialWidth, initialHeight, resizeLeft, resizeTop, footerY;

    public AbyssHudConfigScreen(Screen parent) {
        super(Component.translatable("screen.spiral_abyss.config.hud"));
        this.parent = parent;
    }

    @Override protected void init() {
        finishDrag();
        footerY = height - 26;
        int left = width / 2 - 150;
        groupButton = addRenderableWidget(Button.builder(groupName(), button -> {
            selected = HudGroup.values()[(selected.ordinal() + 1) % HudGroup.values().length];
            groupButton.setMessage(groupName());
        }).bounds(left, footerY, 134, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.spiral_abyss.config.hud_reset"), button -> {
            AbyssClientConfig.hud(selected).reset();
            pendingSave = true;
            saveChanges();
        }).bounds(left + 138, footerY, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + 242, footerY, 58, 20).build());
        countdownButton = addRenderableWidget(Button.builder(countdownLabel(), button -> {
            countdownPreview = !countdownPreview;
            countdownButton.setMessage(countdownLabel());
        }).bounds(left, footerY - 24, 160, 20).build());
    }

    private Component groupName() {
        return Component.translatable("screen.spiral_abyss.config.hud_" + selected.name().toLowerCase(java.util.Locale.ROOT));
    }

    private Component countdownLabel() {
        return Component.translatable(countdownPreview ? "screen.spiral_abyss.config.preview_countdown"
                : "screen.spiral_abyss.config.preview_timer");
    }

    private Bounds bounds(HudGroup group) {
        return AbyssEncounterOverlay.bounds(group, width, height, countdownPreview);
    }

    private static boolean contains(Bounds bounds, double x, double y) {
        return x >= bounds.x() && x < bounds.x() + bounds.width()
                && y >= bounds.y() && y < bounds.y() + bounds.height();
    }

    private static int handleX(Bounds bounds) { return bounds.x() + bounds.width() - HANDLE_RADIUS; }
    private static int handleY(Bounds bounds) { return bounds.y() + bounds.height() - HANDLE_RADIUS; }

    private static boolean onHandle(Bounds bounds, double x, double y) {
        double dx = x - handleX(bounds), dy = y - handleY(bounds);
        return dx * dx + dy * dy <= (HANDLE_RADIUS + 2) * (HANDLE_RADIUS + 2);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        Bounds current = bounds(selected);
        if (onHandle(current, mouseX, mouseY)) {
            scaling = true;
            initialScale = AbyssClientConfig.hud(selected).scale.get();
            initialWidth = current.width();
            initialHeight = current.height();
            resizeLeft = current.x();
            resizeTop = current.y();
            resizeX = mouseX;
            resizeY = mouseY;
            // Freeze an automatic default anchor while its dimensions change.
            place(resizeLeft, resizeTop, current);
        } else {
            // Controls stay accessible, and the selection button can reach overlapping groups.
            if (mouseY >= footerY - 27) return super.mouseClicked(mouseX, mouseY, button);
            HudGroup hit = contains(current, mouseX, mouseY) ? selected : null;
            if (hit == null) for (HudGroup group : HudGroup.values()) {
                if (contains(bounds(group), mouseX, mouseY)) hit = group;
            }
            if (hit == null) return super.mouseClicked(mouseX, mouseY, button);
            selected = hit;
            groupButton.setMessage(groupName());
            current = bounds(selected);
            moving = true;
            grabX = mouseX - current.x();
            grabY = mouseY - current.y();
        }
        setFocused(null);
        setDragging(true);
        return true;
    }

    private void place(double x, double y, Bounds bounds) {
        var setting = AbyssClientConfig.hud(selected);
        double normalizedX = Mth.clamp(x / Math.max(1, width - bounds.width()), 0, 1);
        double normalizedY = Mth.clamp(y / Math.max(1, height - bounds.height()), 0, 1);
        if (setting.x.get() != normalizedX || setting.y.get() != normalizedY) {
            setting.x.set(normalizedX);
            setting.y.set(normalizedY);
            pendingSave = true;
        }
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && moving) {
            place(mouseX - grabX, mouseY - grabY, bounds(selected));
            return true;
        }
        if (button == 0 && scaling) {
            // Project the drag onto the rectangle's diagonal for smooth uniform scaling.
            double diagonal = (double) initialWidth * initialWidth + (double) initialHeight * initialHeight;
            double ratio = 1 + ((mouseX - resizeX) * initialWidth + (mouseY - resizeY) * initialHeight) / Math.max(1, diagonal);
            double scale = Mth.clamp(initialScale * ratio, AbyssClientConfig.MIN_HUD_SCALE, AbyssClientConfig.MAX_HUD_SCALE);
            AbyssClientConfig.hud(selected).scale.set(scale);
            pendingSave = true;
            place(resizeLeft, resizeTop, bounds(selected));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (moving || scaling)) {
            finishDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void saveChanges() {
        if (!pendingSave) return;
        AbyssClientConfig.SPEC.save();
        pendingSave = false;
    }

    private void finishDrag() {
        saveChanges();
        moving = scaling = false;
        setDragging(false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        AbyssEncounterOverlay.renderPreview(graphics, width, height, countdownPreview);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        for (HudGroup group : HudGroup.values()) {
            if (group != selected) outline(graphics, bounds(group), 0xFF777788);
        }
        Bounds current = bounds(selected);
        outline(graphics, current, 0xFFFFCC66);
        graphics.fill(0, 0, width, 34, 0xD010101B);
        graphics.drawCenteredString(font, title, width / 2, 5, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.spiral_abyss.config.hud_drag"), width / 2, 19, 0xBBBBCC);
        graphics.fill(0, footerY - 28, width, height, 0xE010101B);
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.config.hud_scale",
                Math.round(AbyssClientConfig.hud(selected).scale.get() * 100)), width / 2 + 16, footerY - 18, 0xFFFFCC66);
        super.render(graphics, mouseX, mouseY, partialTick);
        // Keep the resize circle reachable even for groups anchored at a screen edge.
        graphics.pose().translate(0, 0, 100);
        drawHandle(graphics, handleX(current), handleY(current), onHandle(current, mouseX, mouseY) || scaling);
        graphics.pose().popPose();
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw the background before the HUD preview, not again from Screen.render.
    }

    private static void outline(GuiGraphics graphics, Bounds bounds, int color) {
        graphics.renderOutline(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, color);
    }

    private static void drawHandle(GuiGraphics graphics, int x, int y, boolean hovered) {
        int color = hovered ? 0xFFFFFFFF : 0xFFFFCC66;
        for (int row = -HANDLE_RADIUS; row <= HANDLE_RADIUS; row++) {
            int halfWidth = (int) Math.sqrt(HANDLE_RADIUS * HANDLE_RADIUS - row * row);
            graphics.fill(x - halfWidth, y + row, x + halfWidth + 1, y + row + 1, color);
        }
        graphics.fill(x - 3, y, x + 4, y + 1, 0xFF20202D);
        graphics.fill(x, y - 3, x + 1, y + 4, 0xFF20202D);
    }

    @Override public void onClose() {
        finishDrag();
        minecraft.setScreen(parent);
    }

    @Override public void removed() { finishDrag(); }
    @Override public boolean isPauseScreen() { return false; }
}
