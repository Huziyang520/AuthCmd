package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.ConfigScreenEntry;
import com.authcmd.mod.gui.PauseScreenButton;
import com.camelot.notice.JoinNotice;
import com.camelot.ui.ConfigScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * AuthCmd NeoForge 客户端：暂停界面按钮 + 进世界提示。
 *
 * <p>未安装 AvalonBase 时按钮不存在；界面类名一律在 {@link ConfigScreenEntry} 内反射创建，
 * 本类不引用任何 Avalon 类型。
 */
public class GuiEventHandler {

    private static final Identifier BUTTON_TEXTURE =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/button.png");

    /** 由主入口在构造期调用，将本类的事件处理注册到 NeoForge 事件总线。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuiEventHandler::onClientPlayerLogin);
        NeoForge.EVENT_BUS.addListener(GuiEventHandler::onScreenInit);
    }

    /** 既没有 AvalonBase 也没有 Cloth 时提示玩家：可视化配置界面不可用。 */
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (AvalonLink.isAvalonLoaded() || ConfigScreens.clothAvailable()) {
            return;
        }
        JoinNotice.show(Minecraft.getInstance().player, AuthCmdConfig.CONFIG, "show_tips",
                "message.authcmd.notice_config_ui");
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        if (!AvalonLink.isAvalonLoaded()) return;
        if (Minecraft.getInstance().player == null) return;

        if (Minecraft.getInstance().hasSingleplayerServer()) AuthCmdConfig.CONFIG.reloadIfChanged();
        if (!AuthCmdConfig.enableConfigEntry()) return;

        int btnSize = 20;
        // OnlyTP 按钮在右上角（x = screenWidth-25），AuthCmd 按钮放在其正左侧，留 4px 间距，互不遮挡
        int x = event.getScreen().width - btnSize * 2 - 9;

        event.addListener(PauseScreenButton.create(x, 5, btnSize, BUTTON_TEXTURE,
                ConfigScreenEntry::openAvalonScreen));
    }
}
