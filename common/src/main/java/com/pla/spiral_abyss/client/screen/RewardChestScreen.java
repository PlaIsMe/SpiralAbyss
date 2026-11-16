package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.menu.RewardChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Original renderer; visual inspiration: Anthony Hilyard's Item Borders (see THIRD_PARTY_NOTICES.md). */
public final class RewardChestScreen extends AbstractContainerScreen<RewardChestMenu> {
    public RewardChestScreen(RewardChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 240; inventoryLabelY = 146;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 8, topPos + 7, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
                .bounds(leftPos + 148, topPos + 7, 20, 16).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF25354D);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF8B9AAD);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF101B2B);
            graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFF718399);
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("screen.spiral_abyss.loot_page", menu.page() + 1, menu.pages()), imageWidth / 2, 11, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < RewardChestMenu.PAGE_SIZE; i++) if (menu.rare(i) && menu.getSlot(i).hasItem()) {
            var slot = menu.getSlot(i);
            graphics.renderOutline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xFFFFD83D);
        }
        graphics.pose().popPose();
        renderTooltip(graphics, mouseX, mouseY);
    }
}
