package com.authcmd.mod.network;

import com.authcmd.mod.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * AvalonNetwork 反射桥。
 *
 * <p>统一以字符串反射调用 {@code com.avalon.base.network.AvalonNetwork}，避免业务类字节码
 * 静态引用 Avalon 类（否则未装 AvalonBase 的环境加载该业务类时会触发
 * {@code NoClassDefFoundError}）。调用方应先经 {@link com.authcmd.mod.AvalonLink#isAvalonLoaded()}
 * 守卫，本桥内部不再重复判断。
 */
public final class AvalonNetworkBridge {

    private static final String AVALON_NETWORK = "com.avalon.base.network.AvalonNetwork";

    private AvalonNetworkBridge() {
    }

    /** 反射调用 {@code AvalonNetwork.sendToPlayer(player, channel, payload)}。 */
    public static void sendToPlayer(ServerPlayer player, ResourceLocation channel, Object payload) {
        invoke("sendToPlayer",
                new Class<?>[]{ServerPlayer.class, ResourceLocation.class, Object.class},
                player, channel, payload);
    }

    /** 反射调用 {@code AvalonNetwork.sendToAll(server, channel, payload)}。 */
    public static void sendToAll(MinecraftServer server, ResourceLocation channel, Object payload) {
        invoke("sendToAll",
                new Class<?>[]{MinecraftServer.class, ResourceLocation.class, Object.class},
                server, channel, payload);
    }

    private static void invoke(String method, Class<?>[] paramTypes, Object... args) {
        try {
            Class.forName(AVALON_NETWORK).getMethod(method, paramTypes).invoke(null, args);
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to invoke AvalonNetwork.{} via reflection", method, e);
        }
    }
}
