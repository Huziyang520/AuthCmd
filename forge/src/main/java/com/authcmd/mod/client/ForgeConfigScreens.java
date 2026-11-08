package com.authcmd.mod.client;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.gui.ConfigScreenEntry;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

public final class ForgeConfigScreens {

    private ForgeConfigScreens() {
    }

    public static void register() {
        if (!AvalonLink.isAvalonLoaded()) {
            return;
        }
        ModList.get().getModContainerById(Constants.MOD_ID).ifPresent(container ->
                ModLoadingContext.get().registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory(
                                (mc, parent) -> ConfigScreenEntry.create(parent))));
    }
}
