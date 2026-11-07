package com.authcmd.mod;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandTreeLogic;
import com.camelot.fabric.FabricConfigs;
import com.camelot.net.CamelotNet;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;

/**
 * AuthCmd Fabric 入口。
 * <p>命令执行拦截与游戏模式切换拦截由 mixin 完成（见 mixin 包）；配置读写、界面、
 * 服务端下发与热加载由 Camelot 处理，这里只登记本模组的配置与"配置变化后重发命令树"。
 */
public class AuthCmdMod implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        FabricConfigs.register(AuthCmdConfig.all());
        CamelotNet.register(AuthCmdConfig.CONFIG, ClientTreeResender::resendAll);

        // 命令注册阶段即 patch 命令树，放行非OP白名单指令的权限（使其可补全、可执行）
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommandTreeLogic.patch(dispatcher.getRoot()));

        // 玩家加入时重发命令树，使其基于最新白名单获得正确的补全树
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ClientTreeResender.resend(handler.player));

        LOGGER.info("AuthCmd loaded!");
    }
}
