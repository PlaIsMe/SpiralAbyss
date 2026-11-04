package com.pla.spiral_abyss.fabric.client;

import com.pla.spiral_abyss.client.screen.AbyssClientConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class AbyssModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AbyssClientConfigScreen::new;
    }
}
