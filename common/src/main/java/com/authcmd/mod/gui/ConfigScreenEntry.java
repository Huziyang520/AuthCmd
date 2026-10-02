package com.authcmd.mod.gui;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

/**
 * Single entry point used by every mod-list config button (Mod Menu, Catalogue,
 * NeoForge mod list) to route into the AuthCmd visual editor.
 *
 * <p><b>Bytecode rule:</b> this class must never statically reference an
 * AvalonBase type (dialogs) or {@link AuthCmdScreen} (which extends
 * AvalonConfigScreen). Everything is loaded reflectively after the
 * {@link AvalonLink#isAvalonLoaded()} guard, so the mod list stays healthy when
 * AvalonBase is absent (the factory then returns {@code null}).</p>
 */
public final class ConfigScreenEntry {

    private static final String EDITOR_CLASS = "com.authcmd.mod.gui.AuthCmdScreen";
    private static final String ACCESS_DENIED_CLASS = "com.authcmd.mod.gui.dialog.AccessDeniedDialog";
    private static final String LOCAL_NOTICE_CLASS = "com.authcmd.mod.gui.dialog.LocalConfigNoticeDialog";

    private ConfigScreenEntry() {
    }

    /**
     * Creates the screen the mod list should show, or {@code null} when
     * AvalonBase is not installed.
     */
    public static Screen create(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (!AvalonLink.isAvalonLoaded()) {
            return null;
        }
        try {
            if (mc.player != null) {
                // 世界内入口同样先热加载（仅主机场景），保证手改 toml 后入口立即反映
                if (mc.hasSingleplayerServer()) AuthCmdConfig.reloadIfChanged();
                // 世界内：入口由 show_pause_button 判定（服务端下发的值），false 时对**所有人（含 OP）**
                // 都出无权限小窗——保持与服务端"关闭编辑入口"的口径一致。
                // 恢复途径是改 config/authcmd.toml 触发热加载（整表重读 + SYNC 重播）或重启服务端。
                if (!AuthCmdConfig.showPauseButton) {
                    return newAccessDenied(parent);
                }
                return newEditor(parent, false);
            }
            // Main menu: reload the local file first so static fields cannot keep
            // values left over from the last server session.
            AuthCmdConfig.read();
            if (AuthCmdConfig.showLocalConfigNotice == 0) {
                return newEditor(parent, true);
            }
            return newLocalNotice(parent);
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to create config screen entry", e);
            return null;
        }
    }

    private static Screen newEditor(Screen parent, boolean localEdit) throws Exception {
        Class<?> clazz = Class.forName(EDITOR_CLASS);
        Constructor<?> ctor = clazz.getConstructor(Screen.class, boolean.class);
        return (Screen) ctor.newInstance(parent, localEdit);
    }

    private static Screen newAccessDenied(Screen parent) throws Exception {
        Class<?> clazz = Class.forName(ACCESS_DENIED_CLASS);
        Constructor<?> ctor = clazz.getConstructor(Screen.class, Component.class, Component.class, Component.class);
        return (Screen) ctor.newInstance(parent,
                Component.translatable("gui.authcmd.notice_title"),
                Component.translatable("gui.authcmd.entry_closed"),
                Component.translatable("gui.authcmd.cancel"));
    }

    private static Screen newLocalNotice(Screen parent) throws Exception {
        Class<?> clazz = Class.forName(LOCAL_NOTICE_CLASS);
        Constructor<?> ctor = clazz.getConstructor(Screen.class, Component.class, Component.class,
                Component.class, Component.class, Component.class, Consumer.class);
        Consumer<Boolean> onConfirm = checked -> {
            try {
                // The checkbox choice is persisted ONLY when the player confirms.
                if (checked) {
                    AuthCmdConfig.showLocalConfigNotice = 0;
                    AuthCmdConfig.save();
                }
                Minecraft.getInstance().setScreenAndShow(newEditor(parent, true));
            } catch (Exception e) {
                Constants.LOG.error("[AuthCmd] Failed to open local editor", e);
            }
        };
        return (Screen) ctor.newInstance(parent,
                Component.translatable("gui.authcmd.notice_title"),
                Component.translatable("gui.authcmd.local_config_notice"),
                Component.translatable("gui.authcmd.dont_show_again"),
                Component.translatable("gui.authcmd.cancel"),
                Component.translatable("gui.authcmd.confirm"),
                onConfirm);
    }
}
