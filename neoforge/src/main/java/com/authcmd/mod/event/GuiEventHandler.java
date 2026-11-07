package com.authcmd.mod.event;

import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.PauseScreenButton;
import com.camelot.ui.ConfigScreens;
import com.camelot.ui.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** AuthCmd NeoForge 客户端：暂停界面按钮，打开 NeoForge 自带的配置界面。 */
public class GuiEventHandler {

    private static final Identifier BUTTON_TEXTURE =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/button.png");

    /** 由主入口在构造期调用，将本类的事件处理注册到 NeoForge 事件总线。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuiEventHandler::onScreenInit);
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (!AuthCmdConfig.enableConfigEntry()) {
            return;
        }

        int btnSize = 20;
        // OnlyTP 按钮在右上角（x = screenWidth-25），AuthCmd 按钮放在其正左侧，留 4px 间距，互不遮挡
        int x = event.getScreen().width - btnSize * 2 - 9;

        event.addListener(PauseScreenButton.create(x, 5, btnSize, BUTTON_TEXTURE,
                () -> {
                    var screen = ConfigScreens.create(Constants.MOD_ID, null);
                    if (screen != null) {
                        Screens.open(screen);
                    }
                }));
    }
}
