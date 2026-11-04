package com.pla.spiral_abyss.client.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import net.minecraft.client.KeyMapping;

public final class AbyssClientPlatform {
    @ExpectPlatform
    public static KeyMapping journalKey() {
        throw new AssertionError();
    }
}
