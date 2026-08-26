package com.authcmd.mod.network;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.event.ClientTreeResender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 → 服务端：管理员更新 AuthCmd 配置。
 * 数据编解码逻辑独立于平台；服务端处理逻辑保留在 {@link #applyToServer(ServerPlayer)}。
 */
public class AuthCmdUpdatePacket {
    private final String mode;
    private final boolean showPauseButton;
    private final List<String> nonOpWhitelist;
    private final List<String> opBlacklist;
    private final List<String> nonOpExempt;
    private final List<String> opExempt;

    public AuthCmdUpdatePacket(String mode, boolean showPauseButton,
                               List<String> nonOpWhitelist, List<String> opBlacklist,
                               List<String> nonOpExempt, List<String> opExempt) {
        this.mode = mode;
        this.showPauseButton = showPauseButton;
        this.nonOpWhitelist = new ArrayList<>(nonOpWhitelist);
        this.opBlacklist = new ArrayList<>(opBlacklist);
        this.nonOpExempt = new ArrayList<>(nonOpExempt);
        this.opExempt = new ArrayList<>(opExempt);
    }

    public AuthCmdUpdatePacket(FriendlyByteBuf buf) {
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
     * 服务端收到后处理：校验权限、写配置、广播同步。
     */
    public void applyToServer(ServerPlayer player) {
        if (player == null) return;
        // 安全硬闸门：非 OP 伪造的更新包直接被丢弃
        if (!player.hasPermissions(2)) return;

        AuthCmdConfig.mode = this.mode;
        AuthCmdConfig.showPauseButton = this.showPauseButton;
        AuthCmdConfig.nonOpWhitelist = new ArrayList<>(this.nonOpWhitelist);
        AuthCmdConfig.opBlacklist = new ArrayList<>(this.opBlacklist);
        AuthCmdConfig.nonOpExempt = new ArrayList<>(this.nonOpExempt);
        AuthCmdConfig.opExempt = new ArrayList<>(this.opExempt);
        AuthCmdConfig.save();

        var server = player.getServer();
        if (server == null) return;

        var sync = new AuthCmdSyncPacket(AuthCmdConfig.mode, AuthCmdConfig.showPauseButton,
                AuthCmdConfig.nonOpWhitelist, AuthCmdConfig.opBlacklist,
                AuthCmdConfig.nonOpExempt, AuthCmdConfig.opExempt);
        if (AvalonLink.isAvalonLoaded()) {
            sendSyncToAll(server, sync);
        }

        // 重发命令树 — 配置变更后客户端补全才会更新（ClientTreeResender 自建树，不依赖反射 requirement patch）
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ClientTreeResender.resend(p);
        }
    }

    /**
     * 反射调用 {@code AvalonNetwork.sendToAll}，避免本类字节码静态引用 Avalon 类。
     */
    private static void sendSyncToAll(MinecraftServer server, Object sync) {
        try {
            Class<?> clazz = Class.forName("com.avalon.base.network.AvalonNetwork");
            clazz.getMethod("sendToAll", MinecraftServer.class, ResourceLocation.class, Object.class)
                    .invoke(null, server, NetworkChannels.SYNC, sync);
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to broadcast sync via reflection", e);
        }
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
