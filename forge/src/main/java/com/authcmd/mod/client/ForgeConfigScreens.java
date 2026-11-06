package com.authcmd.mod.client;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.gui.ConfigScreenEntry;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Registers the native Forge config-screen extension point so the vanilla
 * mod list (as well as Catalogue and Configured, which both query the same
 * extension point) shows a Config button routing to the AuthCmd visual editor.
 *
 * <p>When AvalonBase is absent this registers nothing: the button is hidden
 * rather than broken, because the editor cannot be loaded.</p>
 */
public final class ForgeConfigScreens {

    private ForgeConfigScreens() {
    }

    public static void register() {
        if (!AvalonLink.isAvalonLoaded()) {
            return;
        }
        ModList.get().getModContainerById(Constants.MOD_ID).ifPresent(container -> {
            // Forge 1.20.1 的扩展点是 ConfigScreenHandler.ConfigScreenFactory；
            // registerExtensionPoint 收 (Class, Supplier)，故传 lambda。
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> ConfigScreenEntry.create(parent)));
        });
    }
}
