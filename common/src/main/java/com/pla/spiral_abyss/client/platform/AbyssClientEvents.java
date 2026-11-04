package com.pla.spiral_abyss.client.platform;

import com.pla.spiral_abyss.platform.event.AbyssEvents;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.*;

public final class AbyssClientEvents {
    public static class TickEvent {
        public enum Phase {
            START,
            END
        }

        public static class ClientTickEvent extends AbyssEvents.Event {
            public final TickEvent.Phase phase;

            public ClientTickEvent(TickEvent.Phase phase) {
                this.phase = phase;
            }
        }
    }

    public static class ClientPlayerNetworkEvent {
        public static class LoggingOut extends AbyssEvents.Event {
            public LoggingOut() {}
        }
    }

    public static class RenderGuiEvent {
        public static class Pre extends AbyssEvents.Event {
            public Pre() {}
        }
    }

    public static class RenderHandEvent extends AbyssEvents.Event {
        public RenderHandEvent() {}
    }

    public static class RenderHighlightEvent {
        public static class Block extends AbyssEvents.Event {
            public BlockHitResult target;

            public Block(BlockHitResult target) {
                this.target = target;
            }

            public BlockHitResult getTarget() {
                return target;
            }
        }
    }

    public static class ScreenEvent {
        public static class Render {
            public static class Pre extends AbyssEvents.Event {
                public Screen screen;

                public Pre(Screen screen) {
                    this.screen = screen;
                }

                public Screen getScreen() {
                    return screen;
                }
            }
        }

        public static class KeyPressed {
            public static class Pre extends AbyssEvents.Event {
                public Screen screen;
                public int keyCode;
                public int scanCode;

                public Pre(Screen screen, int keyCode, int scanCode) {
                    this.screen = screen;
                    this.keyCode = keyCode;
                    this.scanCode = scanCode;
                }

                public Screen getScreen() {
                    return screen;
                }

                public int getKeyCode() {
                    return keyCode;
                }

                public int getScanCode() {
                    return scanCode;
                }
            }
        }
    }

    public static class ScreenInit {
        private final Screen screen;
        private final Consumer<net.minecraft.client.gui.components.AbstractWidget> add;

        public ScreenInit(
                Screen screen, Consumer<net.minecraft.client.gui.components.AbstractWidget> add) {
            this.screen = screen;
            this.add = add;
        }

        public Screen getScreen() {
            return screen;
        }

        public void addListener(net.minecraft.client.gui.components.AbstractWidget widget) {
            add.accept(widget);
        }
    }

    @FunctionalInterface
    public interface Overlay {
        void render(
                Object gui,
                net.minecraft.client.gui.GuiGraphics graphics,
                float partialTick,
                int width,
                int height);
    }
}
