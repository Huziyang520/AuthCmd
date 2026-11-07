package com.authcmd.mod.client;

import com.authcmd.mod.Constants;
import com.authcmd.mod.gui.ConfigScreenEntry;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Mod list config extension; the screen itself is NeoForge's ConfigurationScreen.
 */
public final class NeoForgeConfigScreens {

    private NeoForgeConfigScreens() {
    }

    public static void register() {
        ModList.get().getModContainerById(Constants.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                        (modContainer, parent) -> ConfigScreenEntry.create(parent)));
    }
}
