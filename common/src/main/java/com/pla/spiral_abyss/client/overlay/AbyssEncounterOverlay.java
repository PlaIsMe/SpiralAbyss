package com.pla.spiral_abyss.client.overlay;

import com.pla.spiral_abyss.client.platform.AbyssClientEvents.ClientPlayerNetworkEvent;
import com.pla.spiral_abyss.client.screen.AbyssHudConfigScreen;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import com.pla.spiral_abyss.config.AbyssClientConfig.HudGroup;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.ArrayList;

/** Display server snapshots without locally advancing the clock during wave delays or server pauses. */
public final class AbyssEncounterOverlay {
    private static final ItemStack STAR = new ItemStack(Items.NETHER_STAR);
    private static final float HUD_SCALE = 0.882F;
    private static final float TIMER_SCALE = 1.5F;
    private static final float COUNTDOWN_SCALE = 4F;
    private static final int OBJECTIVE_WIDTH = 240;
    private static AbyssNetwork.EncounterStatus state = AbyssNetwork.EncounterStatus.HIDDEN;

    private AbyssEncounterOverlay() {}

    public static void update(AbyssNetwork.EncounterStatus status) { state = status; }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = AbyssNetwork.EncounterStatus.HIDDEN;
    }

    /** Pixel bounds include the effective scale after fitting oversized groups into the viewport. */
    public record Bounds(int x, int y, int width, int height, float scale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public static Bounds bounds(HudGroup group, int width, int height) {
        return bounds(group, width, height, false);
    }

    public static Bounds bounds(HudGroup group, int width, int height, boolean countdownPreview) {
        Font font = Minecraft.getInstance().font;
        var status = preview(countdownPreview);
        return layout(group, font, width, height, status, challengeLayout(font, status));
    }

    public static void renderPreview(GuiGraphics graphics, int width, int height) {
        renderPreview(graphics, width, height, false);
    }

    public static void renderPreview(GuiGraphics graphics, int width, int height, boolean countdownPreview) {
        render(graphics, Minecraft.getInstance().font, width, height, preview(countdownPreview));
    }

    private static AbyssNetwork.EncounterStatus preview(boolean countdown) {
        return new AbyssNetwork.EncounterStatus(true, false, 6, 3, 12, 24, 185, countdown ? 5 : 0,
                List.of(new AbyssNetwork.ChallengeView(Component.translatable("challenge.spiral_abyss.time", 3), 0, 0, true, false),
                        new AbyssNetwork.ChallengeView(Component.translatable("challenge.spiral_abyss.projectile", 8), 3, 8, false, false),
                        new AbyssNetwork.ChallengeView(Component.translatable("challenge.spiral_abyss.take_damage", 5), 2, 5, false, false)),
                List.of(ResourceLocation.withDefaultNamespace("wither")));
    }

    public static final LayeredDraw.Layer HUD = (graphics, deltaTracker) -> {
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        Minecraft client = Minecraft.getInstance();
        if (!state.active() || client.options.hideGui || client.player == null || client.level == null
                || !client.level.dimension().equals(AbyssDimension.LEVEL)
                || client.screen instanceof AbyssHudConfigScreen) return;
        render(graphics, client.font, width, height, state);
    };

    private static void render(GuiGraphics graphics, Font font, int width, int height,
                               AbyssNetwork.EncounterStatus status) {
        ChallengeLayout challengeLayout = challengeLayout(font, status);
        if (!status.preparing()) {
            Bounds timer = layout(HudGroup.TIMER, font, width, height, status, challengeLayout);
            begin(graphics, timer);
            if (status.countdownSeconds() > 0) {
                graphics.pose().translate(33, 0, 0);
                graphics.pose().scale(COUNTDOWN_SCALE, COUNTDOWN_SCALE, 1);
                graphics.drawCenteredString(font, Integer.toString(status.countdownSeconds()), 0, 0, 0xFFFFFFFF);
            } else AbyssClockRenderer.combat(graphics, font, 33, 0, status.seconds(), TIMER_SCALE);
            graphics.pose().popPose();
        }

        Bounds challenges = layout(HudGroup.CHALLENGES, font, width, height, status, challengeLayout);
        begin(graphics, challenges);
        drawChallenges(graphics, font, status, challengeLayout);
        graphics.pose().popPose();

        if (!status.preparing() && status.floor() != 7 && !status.rejectedBosses().isEmpty()) {
            Bounds rejection = layout(HudGroup.REJECTION, font, width, height, status, challengeLayout);
            // panel supplies its original 90% size; bounds already includes that factor.
            graphics.pose().pushPose();
            graphics.pose().translate(rejection.x(), rejection.y(), 0);
            graphics.pose().scale(rejection.scale() / 0.9F, rejection.scale() / 0.9F, 1);
            AbyssRejectionNotice.panel(graphics, font, 0, 0, 300, Integer.MAX_VALUE, status.rejectedBosses());
            graphics.pose().popPose();
        }
    }

    private static void begin(GuiGraphics graphics, Bounds bounds) {
        graphics.pose().pushPose();
        graphics.pose().translate(bounds.x(), bounds.y(), 0);
        graphics.pose().scale(bounds.scale(), bounds.scale(), 1);
    }

    private static Bounds layout(HudGroup group, Font font, int screenWidth, int screenHeight,
                                 AbyssNetwork.EncounterStatus status, ChallengeLayout challengeLayout) {
        var settings = AbyssClientConfig.hud(group);
        int naturalWidth, naturalHeight;
        float baseScale;
        double defaultX, defaultY;
        if (group == HudGroup.TIMER) {
            naturalWidth = 66;
            naturalHeight = status.countdownSeconds() > 0 ? (int) (font.lineHeight * COUNTDOWN_SCALE) : 22;
            baseScale = 1;
            defaultX = (screenWidth - naturalWidth * settings.scale.get()) / 2D;
            defaultY = status.countdownSeconds() > 0 ? 12 : 36;
        } else if (group == HudGroup.CHALLENGES) {
            naturalWidth = challengeLayout.width();
            naturalHeight = challengeLayout.height();
            baseScale = HUD_SCALE;
            defaultX = 12;
            defaultY = Math.max(44, screenHeight / 4);
        } else {
            var panel = AbyssRejectionNotice.layout(font, 300, status.rejectedBosses());
            naturalWidth = panel.width();
            naturalHeight = panel.height();
            baseScale = HUD_SCALE * 0.9F;
            Bounds challenges = layout(HudGroup.CHALLENGES, font, screenWidth, screenHeight, status, challengeLayout);
            defaultX = challenges.x() - 4 * challenges.scale();
            defaultY = challenges.y() + (challengeLayout.height() + 4) * challenges.scale();
        }
        float scale = (float) (baseScale * settings.scale.get());
        scale = Math.min(scale, Math.min(Math.max(1, screenWidth) / (float) naturalWidth,
                Math.max(1, screenHeight) / (float) naturalHeight));
        int width = Math.min(screenWidth, Math.max(1, Mth.ceil(naturalWidth * scale)));
        int height = Math.min(screenHeight, Math.max(1, Mth.ceil(naturalHeight * scale)));
        int spaceX = Math.max(0, screenWidth - width), spaceY = Math.max(0, screenHeight - height);
        double x = settings.x.get() < 0 ? defaultX : settings.x.get() * spaceX;
        double y = settings.y.get() < 0 ? defaultY : settings.y.get() * spaceY;
        return new Bounds((int) Math.round(Mth.clamp(x, 0, spaceX)),
                (int) Math.round(Mth.clamp(y, 0, spaceY)), width, height, scale);
    }

    private static Component floorWave(AbyssNetwork.EncounterStatus status) {
        return status.floor() == 7 ? Component.translatable("hud.spiral_abyss.endless_wave", status.wave())
                : Component.translatable("hud.spiral_abyss.floor_wave", status.floor(), status.wave());
    }

    private static Component objective(AbyssNetwork.ChallengeView challenge) {
        return challenge.target() > 0
                ? Component.translatable("challenge.spiral_abyss.progress", challenge.description(), challenge.progress(), challenge.target())
                : challenge.description();
    }

    private record ChallengeLayout(int width, int height, List<List<FormattedCharSequence>> rows) {}

    private static ChallengeLayout challengeLayout(Font font, AbyssNetwork.EncounterStatus status) {
        int width = font.width(floorWave(status));
        if (!status.preparing()) width = Math.max(width, font.width(
                Component.translatable("hud.spiral_abyss.defeated", status.defeated(), status.total())));
        int y = status.preparing() ? 0 : font.lineHeight + 6;
        if (status.floor() == 7) return new ChallengeLayout(Math.max(1, width), y + font.lineHeight, List.of());
        width = Math.max(width, font.width(Component.translatable("screen.spiral_abyss.challenge")));
        y += font.lineHeight + 6 + font.lineHeight + 4;
        List<List<FormattedCharSequence>> rows = new ArrayList<>(status.challenges().size());
        for (var challenge : status.challenges()) {
            var lines = font.split(objective(challenge), OBJECTIVE_WIDTH);
            rows.add(lines);
            for (var line : lines) width = Math.max(width, 21 + font.width(line));
            y += Math.max(18, lines.size() * (font.lineHeight + 2) + 4);
        }
        return new ChallengeLayout(Math.max(1, width), y, rows);
    }

    private static void drawChallenges(GuiGraphics graphics, Font font, AbyssNetwork.EncounterStatus status,
                                       ChallengeLayout layout) {
        if (!status.preparing()) graphics.drawString(font,
                Component.translatable("hud.spiral_abyss.defeated", status.defeated(), status.total()), 0, 0, 0xFFFFFFFF, true);
        int y = status.preparing() ? 0 : font.lineHeight + 6;
        graphics.drawString(font, floorWave(status), 0, y, 0xFFFFFFFF, true);
        if (status.floor() == 7) return;
        y += font.lineHeight + 6;
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.challenge"), 0, y, 0xFFFFFFFF, true);
        y += font.lineHeight + 4;
        for (int row = 0; row < status.challenges().size(); row++) {
            var challenge = status.challenges().get(row);
            boolean highlighted = status.preparing() ? !challenge.failed() : challenge.satisfied() && !challenge.failed();
            graphics.renderItem(STAR, 0, y - 3);
            if (!highlighted) graphics.fill(0, y - 3, 16, y + 13, 0x99505050);
            int start = y;
            for (var line : layout.rows().get(row)) {
                graphics.drawString(font, line, 21, y, highlighted ? 0xFFFFFFFF : 0xFF888888, true);
                y += font.lineHeight + 2;
            }
            y = Math.max(start + 18, y + 4);
        }
    }
}
