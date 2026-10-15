package com.pla.spiral_abyss.client.overlay;

import com.pla.spiral_abyss.SpiralAbyss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Display server snapshots without locally advancing the clock during wave delays or server pauses. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class AbyssEncounterOverlay {
    private static final ItemStack STAR = new ItemStack(Items.NETHER_STAR);
    private static final float TIMER_SCALE = 1.5F;
    private static final float COUNTDOWN_SCALE = 4F;
    private static AbyssNetwork.EncounterStatus state = AbyssNetwork.EncounterStatus.HIDDEN;
    private AbyssEncounterOverlay() {}
    public static void update(AbyssNetwork.EncounterStatus status) { state = status; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = AbyssNetwork.EncounterStatus.HIDDEN;
    }
    public static final IGuiOverlay HUD = (gui, graphics, partialTick, width, height) -> {
        Minecraft client = Minecraft.getInstance();
        if (!state.active() || client.options.hideGui || client.player == null || client.level == null
                || !client.level.dimension().equals(AbyssDimension.LEVEL)) return;
        var font = client.font;
        if (!state.preparing()) {
            if (state.countdownSeconds() > 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(width / 2F, 12, 0);
                graphics.pose().scale(COUNTDOWN_SCALE, COUNTDOWN_SCALE, 1);
                graphics.drawCenteredString(font, Integer.toString(state.countdownSeconds()), 0, 0, 0xFFFFFFFF);
                graphics.pose().popPose();
            } else AbyssClockRenderer.combat(graphics, font, width / 2, 12, state.seconds(), TIMER_SCALE);
        }
        int left = 12, top = Math.max(44, height / 4);
        if (!state.preparing()) graphics.drawString(font,
                Component.translatable("hud.spiral_abyss.defeated", state.defeated(), state.total()),
                left, top, 0xFFFFFFFF, true);
        int floorWaveY = state.preparing() ? top : top + font.lineHeight + 6;
        graphics.drawString(font, Component.translatable("hud.spiral_abyss.floor_wave", state.floor(), state.wave()),
                left, floorWaveY, 0xFFFFFFFF, true);
        int challengeY = floorWaveY + font.lineHeight + 6;
        graphics.drawString(font, Component.translatable("screen.spiral_abyss.challenge"),
                left, challengeY, 0xFFFFFFFF, true);
        challengeY += font.lineHeight + 4;
        int objectiveWidth = Math.max(40, Math.min(340, width - left - 38));
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
    };
}
