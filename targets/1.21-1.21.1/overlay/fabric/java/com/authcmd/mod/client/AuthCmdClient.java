package com.authcmd.mod.client;

import com.authcmd.mod.event.GuiEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * AuthCmd Fabric 客户端入口。注册暂停界面按钮注入。
 * <p>客户端网络接收器由前置模组 AvalonBase 的 FabricClient 统一注册。
 */
public class AuthCmdClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        GuiEventHandler.register();

        // 客户端进入世界（本地玩家生成）时，若未安装 AvalonBase 则显示提示
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientJoinNotice.onJoinWorld()
        );
    }
}
