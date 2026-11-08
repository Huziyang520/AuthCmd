package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.ConfigScreenEntry;
import com.authcmd.mod.gui.PauseScreenButton;
import com.authcmd.mod.mixin.ScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;

public class GuiEventHandler {

    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation(Constants.MOD_ID, "textures/gui/button.png");

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PauseScreen)) return;
            if (!AvalonLink.isAvalonLoaded()) return;
            if (Minecraft.getInstance().player == null) return;

            if (Minecraft.getInstance().hasSingleplayerServer()) AuthCmdConfig.CONFIG.reload();
            if (!AuthCmdConfig.enableConfigEntry()) return;

            int btnSize = 20;

            int x = screen.width - btnSize * 2 - 9;

            ((ScreenAccessor) screen).authcmd$invokeAddRenderableWidget(
                    PauseScreenButton.create(x, 5, btnSize, BUTTON_TEXTURE, ConfigScreenEntry::openAvalonScreen));
        });
    }
}
