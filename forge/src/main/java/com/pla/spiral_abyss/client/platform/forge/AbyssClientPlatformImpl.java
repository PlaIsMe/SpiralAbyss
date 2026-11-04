package com.pla.spiral_abyss.client.platform.forge;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

import org.lwjgl.glfw.GLFW;

public final class AbyssClientPlatformImpl {
    public static KeyMapping journalKey() {
        return new KeyMapping(
                "key.spiral_abyss.menu",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.categories.spiral_abyss");
    }
}
