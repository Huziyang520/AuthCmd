package com.authcmd.mod.event;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class ClientTreeResender {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientTreeResender() {
    }

    public static void resend(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            LOGGER.warn("[AuthCmd] ClientTreeResender.resend: null server, skipping");
            return;
        }
        server.getCommands().sendCommands(player);
        LOGGER.debug("[AuthCmd] Command tree resent to {}", player.getName().getString());
    }

    public static void resendAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resend(player);
        }
        LOGGER.info("[AuthCmd] Command tree resent to {} player(s)", server.getPlayerList().getPlayers().size());
    }
}