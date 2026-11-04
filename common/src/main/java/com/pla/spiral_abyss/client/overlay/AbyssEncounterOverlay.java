package com.pla.spiral_abyss.client.overlay;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents.*;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.LayeredDraw;

/** Display server snapshots without locally advancing the clock during wave delays or server pauses. */
public final class AbyssEncounterOverlay {
    private static final ItemStack STAR = new ItemStack(Items.NETHER_STAR);
    private static final float HUD_SCALE = 0.882F;
    private static final float TIMER_SCALE = 1.5F;
    private static final float COUNTDOWN_SCALE = 4F;
    private static AbyssNetwork.EncounterStatus state = AbyssNetwork.EncounterStatus.HIDDEN;
    private AbyssEncounterOverlay() {}
    public static void update(AbyssNetwork.EncounterStatus status) { state = status; }
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = AbyssNetwork.EncounterStatus.HIDDEN;
    }
    public static final LayeredDraw.Layer HUD = (graphics, deltaTracker) -> {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        Minecraft client = Minecraft.getInstance();
        if (!state.active() || client.options.hideGui || client.player == null || client.level == null
                || !client.level.dimension().equals(AbyssDimension.LEVEL)) return;
        var font = client.font;
        if (!state.preparing() && state.countdownSeconds() > 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2F, 12, 0);
            graphics.pose().scale(COUNTDOWN_SCALE, COUNTDOWN_SCALE, 1);
            graphics.drawCenteredString(font, Integer.toString(state.countdownSeconds()), 0, 0, 0xFFFFFFFF);
            graphics.pose().popPose();
        } else if (!state.preparing()) {
            AbyssClockRenderer.combat(graphics, font, width / 2, 36, state.seconds(), TIMER_SCALE);
        }
        // Keep the encounter counters and objectives in one compact left column.
        int anchorX = 12, anchorY = Math.max(44, height / 4);
        graphics.pose().pushPose();
        graphics.pose().translate(anchorX, anchorY, 0);
        graphics.pose().scale(HUD_SCALE, HUD_SCALE, 1);
        int availableWidth = (int) ((width - anchorX - 8) / HUD_SCALE);
        int availableHeight = (int) ((height - anchorY - 8) / HUD_SCALE);
        int left = 0, top = 0;
        if (!state.preparing()) graphics.drawString(font,
                Component.translatable("hud.spiral_abyss.defeated", state.defeated(), state.total()),
                left, top, 0xFFFFFFFF, true);
        int floorWaveY = state.preparing() ? top : top + font.lineHeight + 6;
        graphics.drawString(font, state.floor() == 7 ? Component.translatable("hud.spiral_abyss.endless_wave", state.wave())
                : Component.translatable("hud.spiral_abyss.floor_wave", state.floor(), state.wave()),
                left, floorWaveY, 0xFFFFFFFF, true);
        if (state.floor() == 7) { graphics.pose().popPose(); return; }
        int challengeY = floorWaveY + font.lineHeight + 6;
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.challenge"),
                left, challengeY, 0xFFFFFFFF, true);
        challengeY += font.lineHeight + 4;
        int objectiveWidth = Math.max(40, Math.min(240, availableWidth - 21));
        for (var challenge : state.challenges()) {
            boolean highlighted = state.preparing() ? !challenge.failed() : challenge.satisfied() && !challenge.failed();
            int color = highlighted ? 0xFFFFFFFF : 0xFF888888;
            // The icon belongs to the server's rolled objective, not an elapsed-time tier.
            graphics.renderItem(STAR, left, challengeY - 3);
            if (!highlighted) graphics.fill(left, challengeY - 3, left + 16, challengeY + 13, 0x99505050);
            Component objective = challenge.target() > 0
                    ? Component.translatable("challenge.spiral_abyss.progress", challenge.description(), challenge.progress(), challenge.target())
                    : challenge.description();
            int rowStart = challengeY;
            for (var line : font.split(objective, objectiveWidth)) {
                graphics.drawString(font, line, left + 21, challengeY, color, true);
                challengeY += font.lineHeight + 2;
            }
            challengeY = Math.max(rowStart + 18, challengeY + 4);
        }
        if (!state.preparing()) AbyssRejectionNotice.panel(graphics, font, left - 4, challengeY + 4,
                availableWidth, availableHeight - challengeY - 4, state.rejectedBosses());
        graphics.pose().popPose();
    };
}
