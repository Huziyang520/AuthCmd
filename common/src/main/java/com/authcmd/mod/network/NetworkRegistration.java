package com.authcmd.mod.network;

import com.authcmd.mod.Constants;
import com.avalon.base.network.AvalonNetwork;

/**
 * AuthCmd 网络消息注册（与 AvalonBase 联动）。
 *
 * <p><b>注意：本类静态引用 {@code AvalonNetwork}，因此只能</b>在确认 AvalonBase
 * 已加载（{@code AvalonLink.isAvalonLoaded() == true}）后被反射加载，绝不能由
 * {@code CommonClass} 直接静态引用——否则在未安装 AvalonBase 的环境下，加载
 * {@code CommonClass} 时 JVM 字节码验证会解析本类（进而解析 {@code AvalonNetwork}），
 * 触发 {@code NoClassDefFoundError}。
 */
public final class NetworkRegistration {

    private NetworkRegistration() {
    }

    /**
     * 注册网络消息。由 {@link CommonClass#init()} 在检测到 AvalonBase 后经反射调用。
     */
    public static void register() {
        // 客户端 → 服务端：配置更新（仅服务端处理）
        AvalonNetwork.registerMessage(NetworkChannels.UPDATE, AuthCmdUpdatePacket.class,
                AuthCmdUpdatePacket::encode, AuthCmdUpdatePacket::new,
                ctx -> ctx.enqueueWork(() -> {
                    if (ctx.getSender() != null) {
                        ctx.getMessage().applyToServer(ctx.getSender());
                    }
                }));

        // 服务端 → 客户端：配置同步（仅客户端处理）
        AvalonNetwork.registerMessage(NetworkChannels.SYNC, AuthCmdSyncPacket.class,
                AuthCmdSyncPacket::encode, AuthCmdSyncPacket::new,
                ctx -> ctx.enqueueWork(() -> {
                    if (ctx.isClientSide()) {
                        ctx.getMessage().applyToClient();
                    }
                }));

        Constants.LOG.info("[AuthCmd] Network messages registered");
    }
}