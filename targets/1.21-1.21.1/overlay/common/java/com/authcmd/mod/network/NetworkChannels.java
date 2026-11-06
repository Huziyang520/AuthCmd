package com.authcmd.mod.network;

import com.authcmd.mod.Constants;
import net.minecraft.resources.ResourceLocation;

/**
 * AuthCmd 网络通道定义。
 *
 * <p>Forge 的 SimpleChannel 按消息类分发，channel 仅作为占位（忽略）；
 * Fabric 则以 channel 作为实际通道注册，故每个业务方向使用独立 channel。
 */
public final class NetworkChannels {

    /** 客户端 → 服务端：配置更新。 */
    public static final ResourceLocation UPDATE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update");

    /** 服务端 → 客户端：配置同步。 */
    public static final ResourceLocation SYNC = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync");

    private NetworkChannels() {
    }
}
