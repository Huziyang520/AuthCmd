package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.ConfigScreenEntry;
import com.authcmd.mod.gui.PauseScreenButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class GuiEventHandler {

    private static final ResourceLocation BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/button.png");

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuiEventHandler::onScreenInit);
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }
        if (!AvalonLink.isAvalonLoaded()) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (!AuthCmdConfig.enableConfigEntry()) {
            return;
        }

        int btnSize = 20;

        int x = event.getScreen().width - btnSize * 2 - 9;

        event.addListener(PauseScreenButton.create(x, 5, btnSize, BUTTON_TEXTURE,
                ConfigScreenEntry::openAvalonScreen));
    }
}
