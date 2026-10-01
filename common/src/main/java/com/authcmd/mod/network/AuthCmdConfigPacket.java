package com.authcmd.mod.network;

import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * AuthCmd 配置网络包的公共基类。
 *
 * <p>配置数据结构（{@code mode}、{@code showPauseButton}、四份名单）在客户端→服务端
 * （{@link AuthCmdUpdatePacket}）与服务端→客户端（{@link AuthCmdSyncPacket}）两个方向上
 * 完全一致，字段定义与编解码逻辑也完全相同。把它们收敛到本类后，两个方向的子类只需
 * 各自实现应用逻辑（写配置 / 广播 + 重发命令树），消除重复。
 */
public abstract class AuthCmdConfigPacket {

    protected final String mode;
    protected final boolean showPauseButton;
    protected final List<String> nonOpWhitelist;
    protected final List<String> opBlacklist;
    protected final List<String> nonOpExempt;
    protected final List<String> opExempt;

    protected AuthCmdConfigPacket(String mode, boolean showPauseButton,
                                  List<String> nonOpWhitelist, List<String> opBlacklist,
                                  List<String> nonOpExempt, List<String> opExempt) {
        this.mode = mode;
        this.showPauseButton = showPauseButton;
        this.nonOpWhitelist = new ArrayList<>(nonOpWhitelist);
        this.opBlacklist = new ArrayList<>(opBlacklist);
        this.nonOpExempt = new ArrayList<>(nonOpExempt);
        this.opExempt = new ArrayList<>(opExempt);
    }

    protected AuthCmdConfigPacket(FriendlyByteBuf buf) {
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

    /** 将本包携带的配置写入 {@link AuthCmdConfig} 内存字段。 */
    protected void applyToConfig() {
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