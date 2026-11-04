package com.pla.spiral_abyss.client.screen;

import net.minecraft.core.registries.BuiltInRegistries;
import com.pla.spiral_abyss.network.AbyssNetwork.RewardPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Scrollable reward icons and a paged chest-content tooltip. No client loot rolls. */
final class RewardPreviewPanel {
    private static final int TILE_SIZE = 40, GAP = 6, PER_PAGE = 32;
    private final List<RewardPreview> rewards;
    private final int x, y, width, height, tileSize, cellSize;
    private int scroll, hovered = -1, page, popupX, popupY, popupWidth, popupHeight;
    private boolean inspecting;
    RewardPreviewPanel(List<RewardPreview> rewards, int x, int y, int width, int height) {
        this.rewards = rewards; this.x = x; this.y = y; this.width = width; this.height = height;
        tileSize = Math.max(20, Math.min(TILE_SIZE, Math.min(width - 6, height)));
        cellSize = tileSize + GAP;
    }
    private int columns() { return Math.max(1, (width - 6 + GAP) / cellSize); }
    private int maxScroll() {
        int rows = (rewards.size() + columns() - 1) / columns();
        return Math.max(0, rows * cellSize - GAP - height);
    }
    boolean popupContains(double mouseX, double mouseY) {
        return inspecting && Screen.hasShiftDown() && hovered >= 0 && rewards.get(hovered).table()
                && within(mouseX, mouseY, popupX, popupY, popupWidth, popupHeight);
    }
    private int iconAt(double mouseX, double mouseY) {
        if (!within(mouseX, mouseY, x, y, columns() * cellSize - GAP, height)) return -1;
        int localX = (int) mouseX - x, localY = (int) mouseY - y + scroll;
        if (localX % cellSize >= tileSize || localY % cellSize >= tileSize) return -1;
        int index = localY / cellSize * columns() + localX / cellSize;
        return index < rewards.size() ? index : -1;
    }
    private static boolean within(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
    private static ItemStack stack(ResourceLocation id) {
        var item = BuiltInRegistries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
    private static ItemStack iconStack(RewardPreview reward) {
        return reward.icon().isEmpty() ? stack(reward.id()) : reward.icon();
    }
    private static Component tableName(ResourceLocation id) {
        String path = id.getPath().substring(id.getPath().lastIndexOf('/') + 1).replace('_', ' ');
        StringBuilder name = new StringBuilder();
        for (String word : path.split(" ")) if (!word.isEmpty()) {
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.translatable("screen.spiral_abyss.chest_loot", name.toString());
    }
    void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.rewards"), x, y - 13, 0xFFFFFFFF, false);
        if (!Screen.hasShiftDown()) inspecting = false;
        // Only Shift pins a preview. Ordinary previews never intercept adjacent icons.
        int hit = iconAt(mouseX, mouseY);
        if (!inspecting) {
            if (hit != hovered) page = 0;
            hovered = hit;
            inspecting = hit >= 0 && rewards.get(hit).table() && Screen.hasShiftDown();
        }
        if (hovered >= 0) {
            popupWidth = Math.min(232, screenWidth - 12);
            int noteHeight = font.split(Component.translatable("screen.spiral_abyss.loot_chance_basis"), popupWidth - 16).size()
                    * font.lineHeight + 4;
            popupHeight = Math.min(146 + noteHeight, screenHeight - 12);
            int anchorX = x + hovered % columns() * cellSize;
            int anchorY = y + hovered / columns() * cellSize - scroll;
            popupX = Mth.clamp(anchorX + cellSize, 6, screenWidth - popupWidth - 6);
            popupY = Mth.clamp(anchorY - 8, 6, screenHeight - popupHeight - 6);
        }
        graphics.enableScissor(x, y, x + width, y + height);
        for (int i = 0; i < rewards.size(); i++) {
            int sx = x + i % columns() * cellSize, sy = y + i / columns() * cellSize - scroll;
            if (sy + tileSize <= y || sy >= y + height) continue;
            RewardPreview reward = rewards.get(i);
            int color = inspecting && i == hovered ? 0xBD78FF : reward.rare() ? 0xFFDD49 : 0x32E7F4;
            boolean showChance = reward.table() || reward.rare();
            icon(graphics, reward.table() ? new ItemStack(Items.CHEST) : iconStack(reward), sx, sy, color);
            if (showChance) percentage(graphics, reward.displayChance(), sx, sy);
        }
        graphics.disableScissor();
        if (rewards.isEmpty()) graphics.drawString(font, Component.translatable("screen.spiral_abyss.no_rewards"), x + 2, y + 6, 0xB8C7DA, false);
        if (maxScroll() > 0) {
            int thumb = Math.max(8, height * height / (height + maxScroll()));
            int top = y + (height - thumb) * scroll / maxScroll();
            graphics.fill(x + width - 3, y, x + width, y + height, 0xFF35455B);
            graphics.fill(x + width - 3, top, x + width, top + thumb, 0xFF9BD7E6);
        }
        if (hovered < 0) return;
        RewardPreview reward = rewards.get(hovered);
        if (!reward.table()) {
            graphics.renderTooltip(font, iconStack(reward), mouseX, mouseY);
            return;
        }
        renderContents(graphics, reward, mouseX, mouseY);
    }
    private void renderContents(GuiGraphics graphics, RewardPreview reward, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int pages = Math.max(1, (reward.contents().size() + PER_PAGE - 1) / PER_PAGE);
        page = Mth.clamp(page, 0, pages - 1);
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 450);
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xF5101420);
        graphics.renderOutline(popupX, popupY, popupWidth, popupHeight, 0xFF47DBEB);
        Component title = tableName(reward.id());
        float scale = Math.min(1, (popupWidth - 16f) / Math.max(1, font.width(title)));
        graphics.pose().pushPose(); graphics.pose().translate(popupX + 8, popupY + 7, 0); graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, title, 0, 0, 0xFFFFFFFF, false); graphics.pose().popPose();
        graphics.drawString(font, Component.translatable(reward.incomplete() ? "screen.spiral_abyss.known_loot"
                : "screen.spiral_abyss.possible_loot"), popupX + 8, popupY + 21, 0xAEBED2, false);
        var chanceNote = font.split(Component.translatable("screen.spiral_abyss.loot_chance_basis"), popupWidth - 16);
        for (int line = 0; line < chanceNote.size(); line++)
            graphics.drawString(font, chanceNote.get(line), popupX + 8, popupY + 33 + line * font.lineHeight, 0xAEBED2, false);
        int noteHeight = chanceNote.size() * font.lineHeight + 4;
        ItemStack underMouse = ItemStack.EMPTY;
        int end = Math.min(reward.contents().size(), (page + 1) * PER_PAGE);
        for (int i = page * PER_PAGE; i < end; i++) {
            int index = i % PER_PAGE, sx = popupX + 8 + index % 8 * 26, sy = popupY + 36 + noteHeight + index / 8 * 22;
            ItemStack item = stack(reward.contents().get(i));
            graphics.renderItem(item, sx, sy);
            if (inspecting && within(mouseX, mouseY, sx - 1, sy - 1, 18, 18)) underMouse = item;
        }
        if (reward.contents().isEmpty()) graphics.drawString(font,
                Component.translatable("screen.spiral_abyss.loot_contents_unavailable"), popupX + 8, popupY + 40 + noteHeight, 0xB8C7DA, false);
        Component footer = pages > 1 ? Component.translatable("screen.spiral_abyss.loot_page", page + 1, pages)
                : Component.translatable(reward.incomplete() ? "screen.spiral_abyss.loot_partial" : "screen.spiral_abyss.loot_varies");
        graphics.drawString(font, footer, popupX + 8, popupY + popupHeight - 15, 0xAEBED2, false);
        graphics.pose().popPose();
        if (!underMouse.isEmpty()) {
            graphics.pose().pushPose(); graphics.pose().translate(0, 0, 600);
            graphics.renderTooltip(font, underMouse, mouseX, mouseY); graphics.pose().popPose();
        }
    }
    /** Overlays chance text inside the bottom-right corner of the reward square. */
    private void percentage(GuiGraphics graphics, double chance, int x, int y) {
        String label = chance > 0 && chance < 0.001 ? "<0.1%"
                : BigDecimal.valueOf(chance * 100).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, (tileSize - 6F) / Math.max(1, font.width(label)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + tileSize - 3 - font.width(label) * scale,
                y + tileSize - 3 - font.lineHeight * scale, 250);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }
    /** ItemBorders-inspired bottom-weighted gradient with a subtle inner glow. */
    private void icon(GuiGraphics graphics, ItemStack stack, int x, int y, int rgb) {
        graphics.fill(x, y, x + tileSize, y + tileSize, 0x99212D3C);
        int iconSize = Math.min(24, tileSize - 8);
        graphics.pose().pushPose();
        graphics.pose().translate(x + (tileSize - iconSize) / 2F, y + (tileSize - iconSize) / 2F, 0);
        graphics.pose().scale(iconSize / 16F, iconSize / 16F, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
        graphics.drawManaged(() -> {
            graphics.fillGradient(x, y + 1, x + 1, y + tileSize - 1, 0x22000000 | rgb, 0xFF000000 | rgb);
            graphics.fillGradient(x + tileSize - 1, y + 1, x + tileSize, y + tileSize - 1, 0x22000000 | rgb, 0xFF000000 | rgb);
            graphics.fill(x + 1, y + tileSize - 1, x + tileSize - 1, y + tileSize, 0xFF000000 | rgb);
            graphics.fillGradient(x + 1, y + tileSize - 6, x + tileSize - 1, y + tileSize - 1, rgb, 0x55000000 | rgb);
        });
        graphics.pose().popPose();
    }
    boolean scroll(double mouseX, double mouseY, double amount) {
        if (!Screen.hasShiftDown()) inspecting = false;
        if (hovered >= 0 && rewards.get(hovered).table()
                && (popupContains(mouseX, mouseY) || iconAt(mouseX, mouseY) == hovered)) {
            int pages = Math.max(1, (rewards.get(hovered).contents().size() + PER_PAGE - 1) / PER_PAGE);
            if (pages > 1) { page = Math.floorMod(page + (amount < 0 ? 1 : -1), pages); return true; }
        }
        if (!within(mouseX, mouseY, x, y, width, height)) return false;
        if (inspecting) return true;
        scroll = Mth.clamp(scroll - (int) (amount * cellSize), 0, maxScroll()); hovered = -1;
        return true;
    }
}
