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
import net.minecraft.resources.Identifier;

/**
 * AuthCmd Fabric client: injects the config button into the pause screen (the only AvalonBase entry).
 * <p>The button is absent without AvalonBase; screen classes are created reflectively in ConfigScreenEntry,
 * so this class never references Avalon types.
 */
public class GuiEventHandler {

    private static final Identifier BUTTON_TEXTURE =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/button.png");

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PauseScreen)) return;
            if (!AvalonLink.isAvalonLoaded()) return;
            if (Minecraft.getInstance().player == null) return;

            // Reloads from the local file when hosting, so manual edits reach the button without a restart
            if (Minecraft.getInstance().hasSingleplayerServer()) AuthCmdConfig.CONFIG.reload();
            if (!AuthCmdConfig.enableConfigEntry()) return;

            int btnSize = 20;
            // Only TP sits at the top right (x = screenWidth-25); this button goes 4px to its left so both fit
            int x = screen.width - btnSize * 2 - 9;

            ((ScreenAccessor) screen).authcmd$invokeAddRenderableWidget(
                    PauseScreenButton.create(x, 5, btnSize, BUTTON_TEXTURE, ConfigScreenEntry::openAvalonScreen));
        });
    }
}
