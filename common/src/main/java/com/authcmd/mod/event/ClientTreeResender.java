package com.authcmd.mod.event;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * 向客户端重发命令树（平台无关，纯 common 模块）。
 * <p>补全树（{@code ClientboundCommandsPacket}）由服务端在发送那一刻按各节点
 * {@code canUse(source)} 过滤生成。{@link CommandTreeLogic} 已把白名单命令节点的
 */
public final class ClientTreeResender {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientTreeResender() {
    }

    /** 为指定玩家重发客户端命令树（含白名单指令的补全）。 */
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

    /** 为全部在线玩家重发客户端命令树。 */
    public static void resendAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resend(player);
        }
        LOGGER.info("[AuthCmd] Command tree resent to {} player(s)", server.getPlayerList().getPlayers().size());
    }
}