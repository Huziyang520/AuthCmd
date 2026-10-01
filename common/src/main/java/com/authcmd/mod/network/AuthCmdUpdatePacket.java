package com.authcmd.mod.network;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;

/**
 * 客户端 → 服务端：管理员更新 AuthCmd 配置。
 * 数据编解码继承自 {@link AuthCmdConfigPacket}；服务端处理逻辑见 {@link #applyToServer(ServerPlayer)}。
 */
public class AuthCmdUpdatePacket extends AuthCmdConfigPacket {

    public AuthCmdUpdatePacket(String mode, boolean showPauseButton,
                               List<String> nonOpWhitelist, List<String> opBlacklist,
                               List<String> nonOpExempt, List<String> opExempt) {
        super(mode, showPauseButton, nonOpWhitelist, opBlacklist, nonOpExempt, opExempt);
    }

    public AuthCmdUpdatePacket(FriendlyByteBuf buf) {
        super(buf);
    }

    /**
     * 服务端收到后处理：校验权限、写配置、广播同步、重发命令树。
     */
    public void applyToServer(ServerPlayer player) {
        if (player == null) return;
        // 安全硬闸门：非 OP 伪造的更新包直接被丢弃
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;

        applyToConfig();
        AuthCmdConfig.save();

        var server = player.level().getServer();
        if (server == null) return;

        if (AvalonLink.isAvalonLoaded()) {
            var sync = new AuthCmdSyncPacket(AuthCmdConfig.mode, AuthCmdConfig.showPauseButton,
                    AuthCmdConfig.nonOpWhitelist, AuthCmdConfig.opBlacklist,
                    AuthCmdConfig.nonOpExempt, AuthCmdConfig.opExempt);
            AvalonNetworkBridge.sendToAll(server, NetworkChannels.SYNC, sync);
        }

        // 重发命令树 — 配置变更后客户端补全才会更新（ClientTreeResender 自建树，不依赖反射 requirement patch）
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ClientTreeResender.resend(p);
        }
    }
}
