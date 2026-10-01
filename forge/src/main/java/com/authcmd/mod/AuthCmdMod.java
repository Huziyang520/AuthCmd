package com.authcmd.mod;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandLogic;
import com.authcmd.mod.event.CommandTreeLogic;
import com.authcmd.mod.event.GameModeLogic;
import com.authcmd.mod.event.GuiEventHandler;
import com.authcmd.mod.event.PlayerJoinLogic;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * AuthCmd Forge 入口。
 * <p>网络访问点由前置模组 AvalonBase 注入（AvalonNetwork），此处仅注册业务事件并初始化配置。
 */
@Mod(Constants.MOD_ID)
public class AuthCmdMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public AuthCmdMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        // 客户端事件（暂停按钮注入、进入世界提示）只在客户端注册。
        // 不能用 @Mod.EventBusSubscriber 注解，否则 mod 构造阶段会强制加载 GuiEventHandler，
        // 进而解析其引用的 Avalon GUI 类（未装 AvalonBase 时触发 NoClassDefFoundError）。
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GuiEventHandler.register();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GameModeLogic.onChangeGameMode(player, event.getCurrentGameMode());
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
