package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.network.AbyssNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.item.Item;

/** Read-only cycle showcase. Travel is requested separately and always authorized by the server. */
public final class AbyssJournalScreen extends Screen {
    private final BannerArtwork artwork = new BannerArtwork();
    private final AbyssNetwork.OpenJournal state;
    private final BannerBossRenderer bosses;
    private final AbyssHelpPanel help = new AbyssHelpPanel();
    private final long openedAt = Util.getMillis();
    private boolean helpPage;
    private int rewardPage, x, y, w, h;
    private static final int REWARDS_PER_VIEW = 3;
    private final BannerRewardRenderer rewardModels = new BannerRewardRenderer();
    private Button previous, next;
    private final List<RewardGroup> rewardGroups = new ArrayList<>();
    private List<FormattedCharSequence> hoveredTooltip;
    private record RewardSource(AbyssNetwork.BannerBoss boss, ItemStack stack) {}
    private record RewardGroup(ItemStack icon, List<RewardSource> sources) {}

    public AbyssJournalScreen(AbyssNetwork.OpenJournal state) {
        super(Component.translatable("screen.spiral_abyss.floors")); this.state = state;
        bosses = new BannerBossRenderer(state == null ? 0 : state.poseSeed());
    }
    @Override protected void init() {
        w = Math.min(900, Math.max(160, width - 56));
        h = Math.min(w / 2, Math.max(100, height - 96));
        w = Math.min(w, h * 2);
        x = (width - w) / 2; y = 44 + (height - 78 - h) / 2;
        JournalNavigation.add(this::addRenderableWidget, width, helpPage ? 1 : 0,
                () -> changeTab(false),
                () -> changeTab(true), () -> minecraft.setScreen(null));
        previous = addRenderableWidget(arrow(helpPage ? 2 : x - 23, y + h / 2 - 15, false));
        next = addRenderableWidget(arrow(helpPage ? width - 22 : x + w + 3, y + h / 2 - 15, true));
        rebuildRewards();
    }
    private void changeTab(boolean help) { helpPage = help; clearWidgets(); init(); }
    private Button arrow(int left, int top, boolean forward) {
        var button = new Button(left, top, 20, 30, Component.translatable(helpPage ? "screen.spiral_abyss.journal.banner" : "screen.spiral_abyss.journal.help"),
                b -> changeTab(!helpPage), n -> n.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
                int color = isHoveredOrFocused() ? 0xFFFFDF98 : 0xFFF0E9DD;
                for (int i = 0; i < 8; i++) {
                    int dx = forward ? i : 7 - i;
                    g.fill(getX() + 5 + dx, getY() + 7 + i, getX() + 6 + dx, getY() + 23 - i, color);
                }
            }
        };
        button.setTooltip(Tooltip.create(button.getMessage())); return button;
    }
    /** Floor 5 finale plus the floor 6 pair, retaining a finale as the central lead after deduplication. */
    private List<AbyssNetwork.BannerBoss> featured() {
        if (state == null || state.bosses().isEmpty()) return List.of();
        List<AbyssNetwork.BannerBoss> result = new ArrayList<>();
        state.bosses().stream().filter(b -> b.floor() == 5 && b.wave() == 3).findFirst().ifPresent(result::add);
        var finals = state.bosses().stream().filter(b -> b.floor() == 6 && b.wave() == 3).toList();
        if (finals.size() < 2) state.bosses().stream().filter(b -> b.floor() == 6 && b.wave() == 2).findFirst().ifPresent(result::add);
        result.addAll(finals.stream().limit(2).toList());
        Map<ResourceLocation, AbyssNetwork.BannerBoss> unique = new LinkedHashMap<>();
        for (var boss : result) {
            // A repeated species keeps its latest/finale slot, so the lead remains the floor-six boss.
            unique.remove(boss.id());
            unique.put(boss.id(), boss);
        }
        return List.copyOf(unique.values());
    }
    private void rebuildRewards() {
        rewardGroups.clear();
        Map<ResourceLocation, RewardGroup> unique = new LinkedHashMap<>();
        Set<ResourceLocation> displayed = new HashSet<>();
        for (var boss : featured()) displayed.add(boss.id());
        for (var boss : state == null ? List.<AbyssNetwork.BannerBoss>of() : state.bosses()) for (var reward : boss.rewards()) {
            if (!displayed.contains(boss.id())) continue;
            if (!reward.rare() || reward.table() || reward.icon().isEmpty()) continue;
            var group = unique.computeIfAbsent(reward.id(), id -> new RewardGroup(reward.icon().copy(), new ArrayList<>()));
            group.sources().add(new RewardSource(boss, reward.icon().copy()));
        }
        rewardGroups.addAll(unique.values());
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // render() draws the world backdrop first; Screen.render() must not blur the journal afterward.
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoveredTooltip = null;
        if (helpPage) JournalWorldBackdrop.render(g, width, height, 26, 42, width - 52, height - 52);
        else JournalWorldBackdrop.render(g, width, height, x, y, w, h);
        if (helpPage) help.render(g, 26, 42, width - 52, height - 52, mouseX, mouseY);
        else renderBanner(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
        if (hoveredTooltip != null) g.renderTooltip(font, hoveredTooltip, mouseX, mouseY);
    }
    private void renderBanner(GuiGraphics g, int mouseX, int mouseY) {
        float unit = w / 800F;
        artwork.render(g, x, y, w, h);
        int tx = x + Math.round(w * .035F), tw = Math.round(w * .33F);
        long remaining = state == null || state.resetRemainingMillis() < 0 ? -1
                : Math.max(0, state.resetRemainingMillis() - (Util.getMillis() - openedAt));
        Component countdown = remaining < 0 ? tr("reset_disabled") : remaining < 86400000L
                ? tr("journal.reset_today") : tr("journal.reset_days", (remaining + 86399999L) / 86400000L);
        text(g, tr("journal.cycle"), tx, y + h * .06F, tw, 0xFF425577, 1.0F * unit);
        wrapped(g, countdown, tx, y + h * .14F, tw, 2.8F * unit, 0xFF263D65);
        float rulesBottom = wrapped(g, tr("journal.rules"), tx, y + h * .37F, tw, 1.03F * unit, 0xFF344C6C);
        var featured = featured();
        List<BossLabel> labels = new ArrayList<>();
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        if (featured.isEmpty()) text(g, tr("preview_unavailable"), x + w * .43F, y + h * .46F, w * .52F, 0xFFFFFFFF, unit);
        else if (featured.size() == 1) {
            labels.add(renderBoss(g, featured.get(0), .40F, .08F, .54F, .80F));
        } else if (featured.size() == 2) {
            labels.add(renderBoss(g, featured.get(0), .55F, .10F, .34F, .34F));
            labels.add(renderBoss(g, featured.get(1), .36F, .38F, .60F, .55F));
        } else {
            // The floor-six finale is the central lead; companions have separate smaller areas.
            labels.add(renderBoss(g, featured.get(0), .68F, .09F, .28F, .39F));
            labels.add(renderBoss(g, featured.get(1), .68F, .49F, .28F, .39F));
            labels.add(renderBoss(g, featured.get(2), .40F, .12F, .34F, .71F));
        }
        // Names follow each fitted silhouette; the reading layer was already flushed below the models.
        for (var label : labels) {
            var bounds = label.bounds();
            Component name = bosses.name(label.boss().id()).copy().withStyle(ChatFormatting.BOLD);
            float scale = Math.min(Math.max(.55F, label.nameScale() * unit), label.nameWidth() / Math.max(1, font.width(name)));
            float labelWidth = font.width(name) * scale;
            float labelX = bounds.x() + (bounds.width() - labelWidth) / 2;
            float labelY = bounds.y() + bounds.height() * .88F;
            // Keep the name on its own silhouette, never pulled toward another boss.
            text(g, name, labelX, labelY, labelWidth + 1, 0xFFFFFFFF, scale);
            if (mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width()
                    && mouseY >= bounds.y() && mouseY < bounds.y() + bounds.height())
                hoveredTooltip = List.of(bosses.name(label.boss().id()).getVisualOrderText());
        }
        renderRewards(g, mouseX, mouseY, unit, rulesBottom);
        Component bossFooter = tr("journal.featured");
        float footerScale = Math.max(.55F, 1.0F * unit);
        float footerWidth = Math.min(w * .54F, font.width(bossFooter) * footerScale);
        text(g, bossFooter, x + w * .965F - footerWidth, y + h * .90F,
                footerWidth, 0xFFE5EDF3, footerScale);
        g.disableScissor();
    }
    private record BossLabel(AbyssNetwork.BannerBoss boss, BannerBossRenderer.Bounds bounds, float nameWidth, float nameScale) {}
    private BossLabel renderBoss(GuiGraphics g, AbyssNetwork.BannerBoss boss, float bx, float by, float bw, float bh) {
        int left = x + Math.round(w * bx), top = y + Math.round(h * by);
        int modelWidth = Math.round(w * bw), modelHeight = Math.round(h * bh);
        bosses.render(g, boss.id(), boss.animation(), left, top, modelWidth, modelHeight);
        return new BossLabel(boss, bosses.contentBounds(boss.id(), left, top, modelWidth, modelHeight), modelWidth, bw < .3F ? 1.15F : 1.6F);
    }
    private void renderRewards(GuiGraphics g, int mx, int my, float unit, float rulesBottom) {
        int pages = Math.max(1, (rewardGroups.size() + REWARDS_PER_VIEW - 1) / REWARDS_PER_VIEW);
        rewardPage = Math.min(rewardPage, pages - 1);
        int count = Math.min(REWARDS_PER_VIEW, rewardGroups.size() - rewardPage * REWARDS_PER_VIEW);
        float areaLeft = x + w * .04F, areaWidth = w * .285F;
        float headingY = Math.max(y + h * .575F, rulesBottom + 5 * unit);
        float top = headingY + font.lineHeight * Math.max(.55F, 1.15F * unit) + 5 * unit;
        float modelHeight = .70F * Math.max(10, y + h * .845F - top);
        top = Math.max(top, y + h * .69F);
        // Pack visible silhouettes into a centered group with a small, consistent gap.
        float gap = Math.max(1, 6 * unit);
        int maxWidth = Math.max(1, (int) (.70F * (areaWidth - gap * Math.max(0, count - 1)) / Math.max(1, count)));
        int[] itemWidths = new int[count];
        float groupWidth = gap * Math.max(0, count - 1);
        for (int i = 0; i < count; i++) {
            var reward = rewardGroups.get(rewardPage * REWARDS_PER_VIEW + i);
            float tilt = (i - (count - 1) / 2F) * -10;
            itemWidths[i] = rewardModels.fittedWidth(g, reward.icon(), maxWidth, Math.round(modelHeight), tilt);
            groupWidth += itemWidths[i];
        }
        float cursor = areaLeft + (areaWidth - groupWidth) / 2;
        for (int i = 0; i < count; i++) {
            var reward = rewardGroups.get(rewardPage * REWARDS_PER_VIEW + i);
            float left = cursor;
            cursor += itemWidths[i] + gap;
            rewardModels.render(g, reward.icon(), Math.round(left), Math.round(top),
                    itemWidths[i], Math.round(modelHeight), (i - (count - 1) / 2F) * -10);
            if (mx >= left && mx < left + itemWidths[i] && my >= top && my < top + modelHeight) {
                List<Component> lines = new ArrayList<>();
                lines.add(reward.icon().getHoverName());
                Set<String> seen = new HashSet<>();
                for (var source : reward.sources()) {
                    String key = source.boss().id() + ":" + source.stack().getComponents();
                    if (!seen.add(key)) continue;
                    lines.add(bosses.name(source.boss().id()).copy().withStyle(ChatFormatting.GOLD));
                    var details = source.stack().getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.Default.NORMAL);
                    for (int j = 1; j < details.size(); j++) lines.add(details.get(j));
                }
                if (pages > 1) lines.add(tr("journal.scroll_rewards", rewardPage + 1, pages));
                hoveredTooltip = lines.stream().flatMap(c -> font.split(c, Math.min(260, this.width - 24)).stream()).toList();
            }
        }
        int left = Math.round(areaLeft), available = Math.round(areaWidth);
        wrapped(g, tr(count == 0 ? "no_rewards" : "journal.rare").copy().withStyle(ChatFormatting.BOLD),
                x + w * .035F, y + h * .90F, available, 1.0F * unit, 0xFF344C6C);
        if (pages > 1) text(g, tr("journal.scroll_rewards", rewardPage + 1, pages), left,
                y + h * .855F, available, 0xFF425577, .65F * unit);
    }
    private static Component tr(String key, Object... args) { return Component.translatable("screen.spiral_abyss." + key, args); }
    private void text(GuiGraphics g, Component text, float left, float top, float available, int color, float desiredScale) {
        float scale = Math.min(Math.max(.55F, desiredScale), available / Math.max(1, font.width(text)));
        g.pose().pushPose(); g.pose().translate(left, top, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, (color & 0xFF0000) > 0x800000); g.pose().popPose();
    }
    private float wrapped(GuiGraphics g, Component text, float left, float top, float available, float scale, int color) {
        scale = Math.max(.55F, scale);
        g.pose().pushPose(); g.pose().translate(left, top, 0); g.pose().scale(scale, scale, 1);
        int row = 0;
        for (var line : font.split(text, Math.max(1, (int) (available / scale)))) {
            g.drawString(font, line, 0, row, color, (color & 0xFF0000) > 0x800000); row += font.lineHeight + 2;
        }
        g.pose().popPose();
        return top + row * scale;
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double amount) {
        if (!helpPage && mx >= x + w * .035 && mx < x + w * .335 && my >= y + h * .57 && my < y + h * .89) {
            int pages = Math.max(1, (rewardGroups.size() + REWARDS_PER_VIEW - 1) / REWARDS_PER_VIEW);
            rewardPage = Math.floorMod(rewardPage + (amount < 0 ? 1 : -1), pages); return true;
        }
        return super.mouseScrolled(mx, my, horizontal, amount);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if ((key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
            changeTab(!helpPage); return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void removed() { bosses.clear(); rewardModels.clear(); artwork.close(); }
    @Override public boolean isPauseScreen() { return false; }
}
