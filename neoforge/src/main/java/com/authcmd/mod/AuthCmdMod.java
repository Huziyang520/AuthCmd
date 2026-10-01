package com.authcmd.mod;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandLogic;
import com.authcmd.mod.event.CommandTreeLogic;
import com.authcmd.mod.event.GameModeLogic;
import com.authcmd.mod.event.GuiEventHandler;
import com.authcmd.mod.event.PlayerJoinLogic;
import com.authcmd.mod.network.AvalonNetworkBridge;
import com.authcmd.mod.network.AuthCmdSyncPacket;
import com.authcmd.mod.network.NetworkChannels;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * AuthCmd NeoForge 入口。
 * <p>网络访问点由前置模组 AvalonBase 注入（AvalonNetwork），此处仅注册业务事件并初始化配置。
 * 26.x 无需 Dist，直接注册 GuiEventHandler（客户端事件在服务端不会触发）。
 */
@Mod(Constants.MOD_ID)
public class AuthCmdMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public AuthCmdMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);

        // 客户端事件（暂停按钮注入、进入世界提示）只在客户端注册。
        // 不能用 @Mod.EventBusSubscriber 注解，否则 mod 构造阶段会强制加载 GuiEventHandler，
        // 进而解析其引用的 Avalon GUI 类（未装 AvalonBase 时触发 NoClassDefFoundError）。
        // 26.x 已移除 Dist，直接注册（服务端不会触发客户端事件，无影响）。
        GuiEventHandler.register();
        // 模组列表 Config 按钮（原生 NeoForge 扩展点，Catalogue/Configured 同样认它）
        com.authcmd.mod.client.NeoForgeConfigScreens.register();

        CommonClass.init();
        LOGGER.info("AuthCmd loaded!");
    }

    @SubscribeEvent
    public void onServerStarting(final ServerStartingEvent event) {
        AuthCmdConfig.init();
        MinecraftServer server = event.getServer();
        // 注入命令树重发回调：手改 TOML 热加载生效时，让已在线玩家的补全树反映最新白名单
        AuthCmdConfig.setCommandTreeResender(() ->
                server.getPlayerList().getPlayers().forEach(ClientTreeResender::resend));
        // 注入配置重播回调：手改 TOML 热加载生效时，重播 SYNC 包让 show_pause_button 等开关也即时生效
        AuthCmdConfig.setConfigBroadcaster(() ->
                AvalonNetworkBridge.sendToAll(server, NetworkChannels.SYNC,
                        new AuthCmdSyncPacket(AuthCmdConfig.mode, AuthCmdConfig.showPauseButton,
                                AuthCmdConfig.nonOpWhitelist, AuthCmdConfig.opBlacklist,
                                AuthCmdConfig.nonOpExempt, AuthCmdConfig.opExempt)));
        LOGGER.info("AuthCmd config initialized");
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
            PlayerJoinLogic.onPlayerJoin(player);
            // 重发命令树，使客户端基于最新白名单获得正确的补全树
            ClientTreeResender.resend(player);
        }
    }
}