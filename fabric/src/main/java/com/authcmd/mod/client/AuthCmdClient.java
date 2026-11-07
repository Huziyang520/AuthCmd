package com.authcmd.mod.client;

import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.GuiEventHandler;
import com.camelot.notice.JoinNotice;
import com.camelot.ui.ConfigScreens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/** AuthCmd Fabric 客户端入口：暂停界面按钮 + 进世界提示。 */
public class AuthCmdClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        GuiEventHandler.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoinWorld());
    }

    /** 本机没有任何配置界面（未装 Cloth Config API）时提示一次。 */
    private static void onJoinWorld() {
        if (ConfigScreens.available(Constants.MOD_ID)) {
            return;
        }
        JoinNotice.show(Minecraft.getInstance().player, AuthCmdConfig.CLIENT, "show_tips",
                "message.authcmd.notice_config_ui");
    }
}
