package com.authcmd.mod.client;

import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.GuiEventHandler;
import com.camelot.notice.JoinNotice;
import com.camelot.ui.ConfigScreens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/** AuthCmd Fabric client entry: pause button and join notice. */
public class AuthCmdClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        GuiEventHandler.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoinWorld());
    }

    /** Shows a notice once when this machine has no config screen (Cloth Config API missing). */
    private static void onJoinWorld() {
        if (ConfigScreens.available(Constants.MOD_ID)) {
            return;
        }
        JoinNotice.show(Minecraft.getInstance().player, AuthCmdConfig.CLIENT, "show_tips",
                "message.authcmd.notice_config_ui");
    }
}
