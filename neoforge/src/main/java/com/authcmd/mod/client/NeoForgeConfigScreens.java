package com.authcmd.mod.client;

import com.authcmd.mod.Constants;
import com.authcmd.mod.gui.ConfigScreenEntry;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class NeoForgeConfigScreens {

    private NeoForgeConfigScreens() {
    }

    public static void register() {
        ModList.get().getModContainerById(Constants.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                        (modContainer, parent) -> ConfigScreenEntry.create(parent)));
    }
}
