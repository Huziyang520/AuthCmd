package com.authcmd.mod.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * 服务端 → 客户端：同步 AuthCmd 配置。
 * 客户端收到后应用配置，供 GUI 显示与热加载。
 * 字段与编解码继承自 {@link AuthCmdConfigPacket}。
 */
public class AuthCmdSyncPacket extends AuthCmdConfigPacket {

    public AuthCmdSyncPacket(String mode, boolean showPauseButton,
                             List<String> nonOpWhitelist, List<String> opBlacklist,
                             List<String> nonOpExempt, List<String> opExempt) {
        super(mode, showPauseButton, nonOpWhitelist, opBlacklist, nonOpExempt, opExempt);
    }

    public AuthCmdSyncPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    /** 客户端收到后应用配置。 */
    public void applyToClient() {
        applyToConfig();
    }
}
