package com.authcmd.mod;

/**
 * AuthCmd 的 Common 侧入口，由各平台入口在初始化时调用。
 *
 * <p><b>关键约束：本类字节码不得静态引用任何 {@code com.avalon.*} 类</b>（尤其
 * {@code AvalonNetwork}）。本类在 mod 初始化（Forge commonSetup / Fabric onInitialize）
 * 时必然被加载，而 JVM 加载类时会执行字节码验证并解析常量池中引用的所有类型；
 * 一旦本类引用了 {@code AvalonNetwork}，在未安装 AvalonBase 的环境下就会抛出
 * {@code NoClassDefFoundError}，导致整个模组加载失败（运行时守卫无效）。
 *
 * <p>因此网络注册逻辑被隔离到 {@link com.authcmd.mod.network.NetworkRegistration}
 * （它引用 {@code AvalonNetwork}），本类仅在 {@link AvalonLink#isAvalonLoaded()}
 * 为 true 时经反射加载并调用它。未安装 AvalonBase 时本类不加载任何 Avalon 类，
 * 核心服务端功能仍可仅靠配置驱动。
 */
public class CommonClass {

    /**
     * 注册网络消息。各平台在对应时机（如 Forge commonSetup / Fabric onInitialize）调用一次。
     * <p>若 AvalonBase 未安装，则跳过网络注册（核心服务端功能不受影响）。
     */
    public static void init() {
        if (!AvalonLink.isAvalonLoaded()) {
            Constants.LOG.info("[AuthCmd] AvalonBase not detected, network layer skipped (config-driven only)");
            return;
        }

        try {
            Class.forName("com.authcmd.mod.network.NetworkRegistration")
                    .getMethod("register")
                    .invoke(null);
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to register network messages via reflection", e);
        }
    }
}
