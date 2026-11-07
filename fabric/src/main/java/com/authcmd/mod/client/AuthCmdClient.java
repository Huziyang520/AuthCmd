package com.authcmd.mod.client;

import com.authcmd.mod.AvalonLink;
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

    /** 既没有 AvalonBase 也没有 Cloth 时提示玩家：可视化配置界面不可用。 */
    private static void onJoinWorld() {
        if (AvalonLink.isAvalonLoaded() || ConfigScreens.clothAvailable()) {
            return;
        }
        JoinNotice.show(Minecraft.getInstance().player, AuthCmdConfig.CONFIG, "show_tips",
                "message.authcmd.notice_config_ui");
    }
}
