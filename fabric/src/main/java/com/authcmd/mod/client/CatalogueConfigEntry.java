package com.authcmd.mod.client;

import com.authcmd.mod.gui.ConfigScreenEntry;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screens.Screen;

public final class CatalogueConfigEntry {

    private CatalogueConfigEntry() {
    }

    public static Screen createConfigScreen(Screen parent, ModContainer container) {
        return ConfigScreenEntry.create(parent);
    }
}
