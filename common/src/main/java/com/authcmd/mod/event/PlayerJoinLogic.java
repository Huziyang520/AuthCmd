package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.network.AuthCmdSyncPacket;
import com.authcmd.mod.network.AvalonNetworkBridge;
import com.authcmd.mod.network.NetworkChannels;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家登录时同步配置到客户端。
 *
 * <p><b>关键约束：本类字节码不得静态引用 {@code com.avalon.base.network.AvalonNetwork}</b>，
 * 网络调用统一走 {@link AvalonNetworkBridge} 反射桥，并经 {@link AvalonLink#isAvalonLoaded()}
 * 守卫，未装 AvalonBase 时直接返回。
 */
public class PlayerJoinLogic {

    public static void onPlayerJoin(ServerPlayer player) {
        if (!AvalonLink.isAvalonLoaded()) return;
        Object sync = new AuthCmdSyncPacket(AuthCmdConfig.mode, AuthCmdConfig.showPauseButton,
                AuthCmdConfig.nonOpWhitelist, AuthCmdConfig.opBlacklist,
                AuthCmdConfig.nonOpExempt, AuthCmdConfig.opExempt);
        AvalonNetworkBridge.sendToPlayer(player, NetworkChannels.SYNC, sync);
    }
}
