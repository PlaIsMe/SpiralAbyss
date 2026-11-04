package com.pla.spiral_abyss.client.platform.fabric;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public final class AbyssClientPlatformImpl {
    public static KeyMapping journalKey() {
        return new KeyMapping(
                "key.spiral_abyss.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.categories.spiral_abyss");
    }
}
