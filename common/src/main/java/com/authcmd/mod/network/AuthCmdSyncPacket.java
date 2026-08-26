package com.authcmd.mod.network;

import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：同步 AuthCmd 配置。
 * 客户端收到后应用配置，供 GUI 显示与热加载。
 */
public class AuthCmdSyncPacket {
    private final String mode;
    private final boolean showPauseButton;
    private final List<String> nonOpWhitelist;
    private final List<String> opBlacklist;
    private final List<String> nonOpExempt;
    private final List<String> opExempt;

    public AuthCmdSyncPacket(String mode, boolean showPauseButton,
                             List<String> nonOpWhitelist, List<String> opBlacklist,
                             List<String> nonOpExempt, List<String> opExempt) {
        this.mode = mode;
        this.showPauseButton = showPauseButton;
        this.nonOpWhitelist = new ArrayList<>(nonOpWhitelist);
        this.opBlacklist = new ArrayList<>(opBlacklist);
        this.nonOpExempt = new ArrayList<>(nonOpExempt);
        this.opExempt = new ArrayList<>(opExempt);
    }

    public AuthCmdSyncPacket(FriendlyByteBuf buf) {
        this.mode = buf.readUtf();
        this.showPauseButton = buf.readBoolean();
        this.nonOpWhitelist = readList(buf);
        this.opBlacklist = readList(buf);
        this.nonOpExempt = readList(buf);
        this.opExempt = readList(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(mode);
        buf.writeBoolean(showPauseButton);
        writeList(buf, nonOpWhitelist);
        writeList(buf, opBlacklist);
        writeList(buf, nonOpExempt);
        writeList(buf, opExempt);
    }

    /**
     * 客户端收到后应用配置。
     */
    public void applyToClient() {
        AuthCmdConfig.mode = this.mode;
        AuthCmdConfig.showPauseButton = this.showPauseButton;
        AuthCmdConfig.nonOpWhitelist = new ArrayList<>(this.nonOpWhitelist);
        AuthCmdConfig.opBlacklist = new ArrayList<>(this.opBlacklist);
        AuthCmdConfig.nonOpExempt = new ArrayList<>(this.nonOpExempt);
        AuthCmdConfig.opExempt = new ArrayList<>(this.opExempt);
    }

    private static List<String> readList(FriendlyByteBuf buf) {
        int s = buf.readVarInt();
        List<String> l = new ArrayList<>();
        for (int i = 0; i < s; i++) l.add(buf.readUtf());
        return l;
    }

    private static void writeList(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s);
    }
}
