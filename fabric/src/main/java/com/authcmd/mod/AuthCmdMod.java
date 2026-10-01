package com.authcmd.mod;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandTreeLogic;
import com.authcmd.mod.event.PlayerJoinLogic;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * AuthCmd Fabric 入口。
 * <p>网络访问点由前置模组 AvalonBase 注入（AvalonNetwork）；
 * 命令执行拦截与游戏模式切换拦截由 Fabric mixin 完成（见 mixin 包）。
 */
public class AuthCmdMod implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        CommonClass.init();
        LOGGER.info("AuthCmd loaded!");

        // 服务端启动时初始化配置
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            AuthCmdConfig.init();
            // 注入命令树重发回调：手改 TOML 热加载生效时，让已在线玩家的补全树反映最新白名单
            AuthCmdConfig.setCommandTreeResender(() ->
                    server.getPlayerList().getPlayers().forEach(ClientTreeResender::resend));
            LOGGER.info("AuthCmd config initialized");
        });

        // 命令注册阶段即 patch 命令树，放行非OP白名单指令的权限（使其可补全、可执行）
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandTreeLogic.patch(dispatcher.getRoot());
        });

        // 玩家加入时同步配置
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            PlayerJoinLogic.onPlayerJoin(player);
            // 重发命令树，使客户端基于最新白名单获得正确的补全树
            ClientTreeResender.resend(player);
        });
    }
}