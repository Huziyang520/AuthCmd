package com.authcmd.mod;

import com.authcmd.mod.client.NeoForgeConfigScreens;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandLogic;
import com.authcmd.mod.event.CommandTreeLogic;
import com.authcmd.mod.event.GameModeLogic;
import com.authcmd.mod.event.GuiEventHandler;
import com.camelot.config.ConfigEnums;
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
 * AuthCmd NeoForge entry point.
 * <p>Config IO, screens, syncing and hot reload belong to Camelot; 26.x has no Dist, so client
 * events are registered directly and simply never fire on a server.
 */
@Mod(Constants.MOD_ID)
public class AuthCmdMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public AuthCmdMod(IEventBus modEventBus) {
        ConfigEnums.register(Constants.MOD_ID, "mode", com.authcmd.mod.config.NeoMode.class);
        NeoConfigs.register(AuthCmdConfig.all());
        CamelotNet.register(AuthCmdConfig.CONFIG, ClientTreeResender::resendAll);

        NeoForge.EVENT_BUS.register(this);
        // Avoids @Mod.EventBusSubscriber: the mod constructor would load GuiEventHandler and resolve
        // its client GUI types, failing with NoClassDefFoundError without AvalonBase.
        GuiEventHandler.register();
        NeoForgeConfigScreens.register();

        LOGGER.info("AuthCmd loaded!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Patches the command tree during registration so whitelisted commands complete and run for non-operators
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
            // Resends the command tree so clients follow the current whitelist
            ClientTreeResender.resend(player);
        }
    }
}
