package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.ConfigScreenEntry;
import com.authcmd.mod.gui.PauseScreenButton;
import com.authcmd.mod.mixin.ScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;

/**
 * AuthCmd Fabric 客户端：在暂停界面注入配置按钮（AvalonBase 界面的唯一入口）。
 * <p>未安装 AvalonBase 时按钮不存在；界面类名一律在 {@link ConfigScreenEntry} 内反射创建，
 * 本类不引用任何 Avalon 类型。
 */
public class GuiEventHandler {

    private static final Identifier BUTTON_TEXTURE =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/button.png");

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PauseScreen)) return;
            if (!AvalonLink.isAvalonLoaded()) return;
            if (Minecraft.getInstance().player == null) return;

            // 本机是主机时按本机文件刷新，手改配置后无需重启即可反映到按钮
            if (Minecraft.getInstance().hasSingleplayerServer()) AuthCmdConfig.CONFIG.reload();
            if (!AuthCmdConfig.enableConfigEntry()) return;

            int btnSize = 20;
            // OnlyTP 按钮在右上角（x = screenWidth-25），AuthCmd 按钮放在其正左侧，留 4px 间距，互不遮挡
            int x = screen.width - btnSize * 2 - 9;

            ((ScreenAccessor) screen).authcmd$invokeAddRenderableWidget(
                    PauseScreenButton.create(x, 5, btnSize, BUTTON_TEXTURE, ConfigScreenEntry::openAvalonScreen));
        });
    }
}
