package com.authcmd.mod.gui;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.camelot.ui.ConfigScreens;
import com.camelot.ui.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;

public final class ConfigScreenEntry {

    private static final String AVALON_SCREEN = "com.authcmd.mod.gui.AuthCmdScreen";

    private ConfigScreenEntry() {
    }

    public static Screen create(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !AuthCmdConfig.enableConfigEntry()) {
            return ConfigScreens.entryClosed(parent,
                    Component.translatable("gui.authcmd.title"),
                    Component.translatable("gui.authcmd.entry_closed"),
                    Component.translatable("gui.done"));
        }
        return ConfigScreens.create(Constants.MOD_ID, parent);
    }

    public static void openAvalonScreen() {
        Screen screen = avalonScreen(null);
        if (screen != null) {
            Screens.open(screen);
        }
    }

    private static Screen avalonScreen(Screen parent) {
        if (!AvalonLink.isAvalonLoaded()) {
            return null;
        }
        try {
            Class<?> type = Class.forName(AVALON_SCREEN);
            Constructor<?> ctor = type.getConstructor(Screen.class);
            return (Screen) ctor.newInstance(parent);
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to open the AvalonBase config screen", e);
            return null;
        }
    }
}
