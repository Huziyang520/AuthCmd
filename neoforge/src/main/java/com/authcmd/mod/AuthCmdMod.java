package com.authcmd.mod;

import com.authcmd.mod.client.NeoForgeConfigScreens;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandLogic;
import com.authcmd.mod.event.CommandTreeLogic;
import com.authcmd.mod.event.GameModeLogic;
import com.authcmd.mod.event.GuiEventHandler;
import com.camelot.net.CamelotNet;
import com.camelot.neoforge.NeoConfigs;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * AuthCmd NeoForge 入口。
 * <p>配置读写、界面、服务端下发与热加载由 Camelot 处理；26.x 无需 Dist，客户端事件直接注册
 * （服务端不会触发，无影响）。
 */
@Mod(Constants.MOD_ID)
public class AuthCmdMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public AuthCmdMod(IEventBus modEventBus) {
        NeoConfigs.register(AuthCmdConfig.all());
        CamelotNet.register(AuthCmdConfig.CONFIG, ClientTreeResender::resendAll);

        NeoForge.EVENT_BUS.register(this);
        // 不能用 @Mod.EventBusSubscriber：mod 构造阶段会强制加载 GuiEventHandler，
        // 进而解析其引用的客户端 GUI 类型（未装 AvalonBase 时 NoClassDefFoundError）。
        GuiEventHandler.register();
        NeoForgeConfigScreens.register();

        LOGGER.info("AuthCmd loaded!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // 命令注册阶段即 patch 命令树，放行非OP白名单指令的权限（使其可补全、可执行）
        CommandTreeLogic.patch(event.getDispatcher().getRoot());
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (CommandLogic.shouldCancel(event.getParseResults())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && GameModeLogic.shouldBlock(player, event.getCurrentGameMode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 重发命令树，使客户端基于最新白名单获得正确的补全树
            ClientTreeResender.resend(player);
        }
    }
}
