package com.pla.spiral_abyss.client.portal;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.init.SoundInit;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** A local presentation of an already-issued floor token; never moves or admits a player. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID, value = Dist.CLIENT)
public final class PortalEntryAnimation {
    private static final long DURATION_MILLIS = 4200;
    private static EntryScreen current;
    private PortalEntryAnimation() {}

    public static void open(AbyssNetwork.OpenFloors state) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (state.inside() || state.entryPortal() == null) {
            FloorSelectionScreen.open(state);
            return;
        }
        if (!client.level.dimension().equals(Level.OVERWORLD)) return;
        if (!(client.level.getBlockEntity(state.entryPortal()) instanceof AbyssPortalBlockEntity portal)
                || !portal.frame().active(client.level)) {
            AbyssNetwork.select(state.token(), 0);
            return;
        }
        EntryScreen screen = new EntryScreen(state, portal);
        client.setScreen(screen);
        current = screen;
        screen.playSound();
    }

    public static boolean active() {
        return current != null && Minecraft.getInstance().screen == current
                && Minecraft.getInstance().level == current.level && Minecraft.getInstance().player == current.player;
    }

    public static float intensity(AbyssPortalBlockEntity portal, float partialTick) {
        if (!active() || !portal.getBlockPos().equals(current.state.entryPortal())) return 0;
        return smooth(current.elapsed() / 850F);
    }

    /** Called only from Camera.setup's client mixin; vanilla initializes every field again next frame. */
    public static CameraPose camera() {
        return active() ? current.cameraPose() : null;
    }

    public record CameraPose(Vec3 position, float yaw, float pitch) {}

    public static void closeForReset() {
        EntryScreen screen = current;
        if (screen == null) return;
        screen.handedOff = true;
        screen.stopSound();
        current = null;
        if (Minecraft.getInstance().screen == screen) Minecraft.getInstance().setScreen(null);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { closeForReset(); }
    @SubscribeEvent public static void hud(RenderGuiEvent.Pre event) { if (active()) event.setCanceled(true); }
    @SubscribeEvent public static void hand(RenderHandEvent event) { if (active()) event.setCanceled(true); }
    @SubscribeEvent public static void outline(RenderHighlightEvent.Block event) {
        Minecraft client = Minecraft.getInstance();
        if (active() || client.level != null && client.level.getBlockState(event.getTarget().getBlockPos())
                .is(com.pla.spiral_abyss.init.BlockInit.ABYSS_PORTAL.get())) event.setCanceled(true);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static final class EntryScreen extends Screen {
        private final AbyssNetwork.OpenFloors state;
        private final ClientLevel level;
        private final LocalPlayer player;
        private final java.util.UUID portalId;
        private final long started = Util.getMillis();
        private final Vec3 initialCamera, eye, target, outward;
        private final float initialYaw, initialPitch;
        private boolean handedOff;
        private SoundInstance entrySound;

        private EntryScreen(AbyssNetwork.OpenFloors state, AbyssPortalBlockEntity portal) {
            super(Component.translatable("screen.spiral_abyss.entering"));
            this.state = state;
            Minecraft client = Minecraft.getInstance();
            level = client.level; player = client.player; portalId = portal.portalId();
            var camera = client.gameRenderer.getMainCamera();
            initialCamera = camera.getPosition(); initialYaw = camera.getYRot(); initialPitch = camera.getXRot();
            eye = player.getEyePosition();
            target = Vec3.atLowerCornerOf(portal.frame().base()).add(.5, 2.8, .5);
            Vec3 normal = Vec3.atLowerCornerOf(portal.frame().front().getNormal());
            // Favor the approach direction, even if network latency carried the player slightly through the plane.
            double side = -player.getLookAngle().dot(normal);
            if (Math.abs(side) < .35) side = player.position().subtract(target).dot(normal);
            outward = normal.scale(side < 0 ? -1 : 1);
        }

        private long elapsed() { return Math.max(0, Util.getMillis() - started); }

        private void playSound() {
            // Local cinematic audio follows the Blocks slider, without fading as the camera pulls back.
            entrySound = new SimpleSoundInstance(SoundInit.PORTAL_ENTRY.get().getLocation(), SoundSource.BLOCKS,
                    .8F, 1F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
                    0, 0, 0, true);
            Minecraft.getInstance().getSoundManager().play(entrySound);
        }

        private void stopSound() {
            if (entrySound == null) return;
            Minecraft.getInstance().getSoundManager().stop(entrySound);
            entrySound = null;
        }

        private boolean valid() {
            Minecraft client = Minecraft.getInstance();
            return client.level == level && client.player == player && player.isAlive() && !player.isSpectator()
                    && player.hurtTime == 0 && player.distanceToSqr(Vec3.atCenterOf(state.entryPortal())) <= 100
                    && level.hasChunkAt(state.entryPortal())
                    && level.getBlockEntity(state.entryPortal()) instanceof AbyssPortalBlockEntity portal
                    && portal.portalId().equals(portalId) && portal.frame().active(level);
        }

        @Override public void tick() {
            if (!valid()) { onClose(); return; }
            if (elapsed() >= DURATION_MILLIS) finish();
        }

        private CameraPose cameraPose() {
            float pullBack = smooth(elapsed() / 650F);
            float approach = smooth((elapsed() - 650) / 3200F);
            double distance = Mth.lerp(approach, 8.0, 3.8);
            Vec3 side = new Vec3(outward.z, 0, -outward.x).scale(.35);
            Vec3 desired = eye.add(outward.scale(distance)).add(side).add(0, Mth.lerp(approach, .55, .15), 0);
            desired = initialCamera.lerp(desired, pullBack);
            Vec3 position = clipCamera(desired);
            Vec3 look = target.subtract(position);
            float yaw = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90);
            float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
            return new CameraPose(position, initialYaw + Mth.wrapDegrees(yaw - initialYaw) * pullBack,
                    Mth.lerp(pullBack, initialPitch, pitch));
        }

        private Vec3 clipCamera(Vec3 desired) {
            Vec3 ray = desired.subtract(eye);
            double length = ray.length();
            if (length < .001) return eye;
            double allowed = length;
            // A small camera volume, rather than one center ray, keeps the near plane out of walls.
            for (int corner = 0; corner < 8; corner++) {
                Vec3 offset = new Vec3((corner & 1) == 0 ? -.15 : .15,
                        (corner & 2) == 0 ? -.15 : .15, (corner & 4) == 0 ? -.15 : .15);
                var hit = level.clip(new ClipContext(eye.add(offset), desired.add(offset),
                        ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.MISS)
                    allowed = Math.min(allowed, Math.max(0, hit.getLocation().distanceTo(eye.add(offset)) - .2));
            }
            return eye.add(ray.scale(allowed / length));
        }

        private void finish() {
            if (!valid()) { onClose(); return; }
            handedOff = true;
            stopSound();
            current = null;
            long remaining = state.resetRemainingMillis() < 0 ? state.resetRemainingMillis()
                    : Math.max(0, state.resetRemainingMillis() - elapsed());
            FloorSelectionScreen.open(new AbyssNetwork.OpenFloors(state.token(), state.claimed(), state.unlocked(),
                    state.selectable(), state.visible(), state.inside(), remaining, state.floors()));
        }

        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                finish(); return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override public void onClose() { minecraft.setScreen(null); }
        @Override public void removed() {
            stopSound();
            if (current == this) current = null;
            if (!handedOff && Minecraft.getInstance().getConnection() != null) AbyssNetwork.select(state.token(), 0);
        }
        @Override public boolean isPauseScreen() { return false; }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bar = Math.round(height * .085F * smooth(elapsed() / 350F));
            graphics.fill(0, 0, width, bar, 0xFF050B18);
            graphics.fill(0, height - bar, width, height, 0xFF050B18);
            graphics.drawCenteredString(font, Component.translatable("screen.spiral_abyss.entry_controls"),
                    width / 2, height - Math.max(12, bar / 2 + 4), 0xB7CEE3);
            int fade = Math.round(255 * smooth((elapsed() - (DURATION_MILLIS - 350)) / 350F));
            if (fade > 0) graphics.fill(0, 0, width, height, fade << 24 | 0x030817);
        }
    }
}

