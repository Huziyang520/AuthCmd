package com.authcmd.mod;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import com.authcmd.mod.event.CommandLogic;
import com.authcmd.mod.event.CommandTreeLogic;
import com.authcmd.mod.event.GameModeLogic;
import com.authcmd.mod.event.GuiEventHandler;
import com.camelot.forge.ForgeConfigs;
import com.camelot.net.CamelotNet;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Constants.MOD_ID)
public class AuthCmdMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public AuthCmdMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeConfigs.register(AuthCmdConfig.all());
        CamelotNet.register(AuthCmdConfig.CONFIG, ClientTreeResender::resendAll);

        MinecraftForge.EVENT_BUS.register(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            GuiEventHandler.register();
        }

        LOGGER.info("AuthCmd loaded!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {

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

            ClientTreeResender.resend(player);
        }
    }
}
