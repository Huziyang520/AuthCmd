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
 * AuthCmd Fabric entry point.
 * <p>Command and game mode blocking lives in the mixin package; config IO, screens,
 * syncing and hot reload belong to Camelot, so this class only registers configs and resends the command tree.
 */
public class AuthCmdMod implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        FabricConfigs.register(AuthCmdConfig.all());
        CamelotNet.register(AuthCmdConfig.CONFIG, ClientTreeResender::resendAll);

        // Patches the command tree during registration so whitelisted commands complete and run for non-operators
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommandTreeLogic.patch(dispatcher.getRoot()));

        // Resends the command tree on join so suggestions follow the current whitelist
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ClientTreeResender.resend(handler.player));

        LOGGER.info("AuthCmd loaded!");
    }
}
