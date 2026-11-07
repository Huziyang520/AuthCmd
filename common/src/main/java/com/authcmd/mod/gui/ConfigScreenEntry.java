package com.authcmd.mod.gui;

import com.camelot.ui.ConfigScreens;
import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;

/**
 * 可视化配置入口：模组列表的「配置」按钮与暂停界面按钮都从这里进。
 *
 * <p><b>字节码约束</b>：本类不得静态引用 {@code AuthCmdScreen}（它继承 AvalonBase 的屏幕基类），
 * 一律在 {@link AvalonLink#isAvalonLoaded()} 守卫后反射创建，未装 AvalonBase 时返回 {@code null}。
 */
public final class ConfigScreenEntry {

    private static final String AVALON_SCREEN = "com.authcmd.mod.gui.AuthCmdScreen";

    private ConfigScreenEntry() {
    }

    /**
     * 模组列表按钮进入的界面：世界内且入口已关闭时给二级提示；否则优先自动生成的界面，
     * 没装 Cloth 时退回 AvalonBase 界面；两者都没有则返回 {@code null}（按钮置灰）。
     */
    public static Screen create(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !AuthCmdConfig.enableConfigEntry()) {
            return ConfigScreens.entryClosed(parent,
                    Component.translatable("gui.authcmd.title"),
                    Component.translatable("gui.authcmd.entry_closed"),
                    Component.translatable("gui.done"));
        }
        // 主菜单（不在世界内）：以本机配置文件为准，不读服务端下发的值
        if (mc.player == null) {
            AuthCmdConfig.CONFIG.load();
        }
        Screen generated = ConfigScreens.cloth(parent, AuthCmdConfig.CONFIG);
        return generated != null ? generated : avalonScreen(parent);
    }

    /** 暂停界面按钮：AvalonBase 界面的唯一入口。未装 AvalonBase 时什么也不做。 */
    public static void openAvalonScreen() {
        Screen screen = avalonScreen(null);
        if (screen != null) {
            Minecraft.getInstance().setScreenAndShow(screen);
        }
    }

    private static Screen avalonScreen(Screen parent) {
        if (!AvalonLink.isAvalonLoaded()) {
            return null;
        }
        try {
            Class<?> type = Class.forName(AVALON_SCREEN);
            Constructor<?> ctor = type.getConstructor(Screen.class);
            return (Screen) ctor.newInstance(parent);
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to open the AvalonBase config screen", e);
            return null;
        }
    }
}
