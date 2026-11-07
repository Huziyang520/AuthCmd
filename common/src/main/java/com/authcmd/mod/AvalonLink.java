package com.authcmd.mod;

/**
 * AvalonBase 联动探测门面。
 * <p>AuthCmd 的核心功能（指令权限控制 / 命令树 patch / 配置）可脱离 AvalonBase 独立运行，
 * 仅当同时安装 AvalonBase 时才启用「暂停页面按钮 + 编辑界面 + 网络配置同步」等联动增强。
 */
public final class AvalonLink {

    /** AvalonBase 的标志类（用于判断其是否已加载）。 */
    private static final String MARKER = "com.avalon.base.Constants";

    /** 结果缓存：AvalonBase 是否已加载。 */
    private static final boolean AVALON_PRESENT = isPresent();

    private AvalonLink() {
    }

    private static boolean isPresent() {
        try {
            Class.forName(MARKER);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * /**
     */
    public static boolean isAvalonLoaded() {
        return AVALON_PRESENT;
    }
}