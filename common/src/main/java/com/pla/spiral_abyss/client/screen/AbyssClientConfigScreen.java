package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.config.AbyssClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Drag the real-size menu icon around a read-only, centered inventory reference. */
public final class AbyssClientConfigScreen extends Screen {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/gui/container/inventory.png");
    private static final int INVENTORY_WIDTH = 176, INVENTORY_HEIGHT = 166, BUTTON_SIZE = 22;
    private final Screen parent;
    private EditBox xField, yField;
    private Button menuButton;
    private int left, footerY;
    private boolean draggingButton, syncingFields, pendingSave;
    private double grabX, grabY;

    public AbyssClientConfigScreen(Screen parent) {
        super(Component.translatable("screen.spiral_abyss.config.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        // Resizing can rebuild widgets in the middle of a drag.
        saveChanges();
        draggingButton = false;
        setDragging(false);
        left = width / 2 - 150;
        footerY = height - 26;
        xField = coordinateField(AbyssClientConfig.INVENTORY_BUTTON_X, "x", left + 12);
        yField = coordinateField(AbyssClientConfig.INVENTORY_BUTTON_Y, "y", left + 86);
        addRenderableWidget(Button.builder(Component.translatable("screen.spiral_abyss.config.reset"), button -> {
            moveButton(AbyssClientConfig.INVENTORY_BUTTON_X.getDefault(), AbyssClientConfig.INVENTORY_BUTTON_Y.getDefault());
            saveChanges();
        }).bounds(left + 148, footerY, 86, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + 242, footerY, 58, 20).build());
        // This preview never requests the journal or interacts with inventory slots.
        menuButton = addRenderableWidget(JournalNavigation.icon(0, 0, BUTTON_SIZE, BUTTON_SIZE,
                JournalNavigation.MENU_ICON, 256, "screen.spiral_abyss.config.drag", false, () -> {}));
        positionButton();
    }

    private EditBox coordinateField(ModConfigSpec.IntValue setting, String axis, int x) {
        EditBox field = new EditBox(font, x, footerY, 54, 20,
                Component.translatable("screen.spiral_abyss.config." + axis));
        field.setMaxLength(10);
        field.setFilter(value -> {
            if (value.isEmpty()) return true;
            try { return Integer.parseInt(value) >= 0; }
            catch (NumberFormatException ex) { return false; }
        });
        field.setValue(Integer.toString(setting.get()));
        field.setResponder(value -> {
            // Clearing the field while replacing a number keeps the last valid setting.
            if (syncingFields || value.isEmpty()) return;
            int coordinate = Integer.parseInt(value);
            if (coordinate == setting.get()) return;
            setting.set(coordinate);
            pendingSave = true;
            positionButton();
            saveChanges();
        });
        return addRenderableWidget(field);
    }

    private void positionButton() {
        menuButton.setX(Math.min(AbyssClientConfig.INVENTORY_BUTTON_X.get(), Math.max(0, width - BUTTON_SIZE)));
        menuButton.setY(Math.min(AbyssClientConfig.INVENTORY_BUTTON_Y.get(), Math.max(0, height - BUTTON_SIZE)));
    }

    private void syncFields(boolean force) {
        syncingFields = true;
        if (force || !xField.isFocused()) xField.setValue(Integer.toString(AbyssClientConfig.INVENTORY_BUTTON_X.get()));
        if (force || !yField.isFocused()) yField.setValue(Integer.toString(AbyssClientConfig.INVENTORY_BUTTON_Y.get()));
        syncingFields = false;
    }

    private void moveButton(int x, int y) {
        if (x != AbyssClientConfig.INVENTORY_BUTTON_X.get() || y != AbyssClientConfig.INVENTORY_BUTTON_Y.get()) {
            AbyssClientConfig.INVENTORY_BUTTON_X.set(x);
            AbyssClientConfig.INVENTORY_BUTTON_Y.set(y);
            pendingSave = true;
        }
        positionButton();
        syncFields(true);
    }

    private void saveChanges() {
        if (!pendingSave) return;
        AbyssClientConfig.SPEC.save();
        pendingSave = false;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Give the drawn-on-top icon priority even if it overlaps an editor control.
        if (button == 0 && menuButton.isMouseOver(mouseX, mouseY)) {
            draggingButton = true;
            grabX = mouseX - menuButton.getX();
            grabY = mouseY - menuButton.getY();
            setFocused(menuButton);
            setDragging(true);
            menuButton.playDownSound(minecraft.getSoundManager());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingButton) {
            int x = (int) Math.round(Mth.clamp(mouseX - grabX, 0, Math.max(0, width - BUTTON_SIZE)));
            int y = (int) Math.round(Mth.clamp(mouseY - grabY, 0, Math.max(0, height - BUTTON_SIZE)));
            moveButton(x, y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingButton) {
            draggingButton = false;
            setDragging(false);
            // Apply each mouse movement live, but write the file only once per drag.
            saveChanges();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        positionButton();
        syncFields(false);
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int inventoryX = (width - INVENTORY_WIDTH) / 2, inventoryY = (height - INVENTORY_HEIGHT) / 2;
        renderInventory(graphics, inventoryX, inventoryY, mouseX, mouseY);
        // Item previews use positive GUI depth; keep the draggable icon above their models/counts.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        boolean overlaps = menuButton.getX() < inventoryX + INVENTORY_WIDTH && menuButton.getX() + BUTTON_SIZE > inventoryX
                && menuButton.getY() < inventoryY + INVENTORY_HEIGHT && menuButton.getY() + BUTTON_SIZE > inventoryY;
        if (overlaps) graphics.renderOutline(inventoryX - 1, inventoryY - 1, INVENTORY_WIDTH + 2, INVENTORY_HEIGHT + 2, 0xFFFFAA55);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(overlaps
                ? "screen.spiral_abyss.config.overlap" : "screen.spiral_abyss.config.description"), width / 2, 22,
                overlaps ? 0xFFAA55 : 0xAAAAAA);
        graphics.drawString(font, "X", left, footerY + 6, 0xFFFFFF);
        graphics.drawString(font, "Y", left + 74, footerY + 6, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    private void renderInventory(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.blit(INVENTORY_TEXTURE, x, y, 0, 0, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        graphics.drawString(font, Component.translatable("container.crafting"), x + 97, y + 8, 0x404040, false);
        // The texture alone remains a useful reference when Config is opened from the title screen.
        if (minecraft.player == null) return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78,
                30, 0.0625F, mouseX, mouseY, minecraft.player);
        for (Slot slot : minecraft.player.inventoryMenu.slots) if (slot.hasItem()) {
            graphics.renderItem(slot.getItem(), x + slot.x, y + slot.y);
            graphics.renderItemDecorations(font, slot.getItem(), x + slot.x, y + slot.y);
        }
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The background is drawn before the preview, not again by Screen.render after it.
    }

    @Override public void onClose() {
        saveChanges();
        minecraft.setScreen(parent);
    }

    @Override public void removed() {
        saveChanges();
        draggingButton = false;
        setDragging(false);
    }
}
