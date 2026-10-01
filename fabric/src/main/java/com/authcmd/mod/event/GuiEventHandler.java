package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.PauseScreenButton;
import com.authcmd.mod.mixin.ScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;

/**
 * AuthCmd Fabric 客户端：在暂停界面注入设置按钮，打开可视化编辑界面。
 *
 * <p><b>关键约束：本类的字节码不得静态引用任何 Avalon 类</b>（尤其继承自
 * {@code AvalonConfigScreen} 的 {@code AuthCmdScreen}）。本类在客户端初始化时会被加载，
 * 而 JVM 加载类时会执行字节码验证并解析常量池里引用的所有类型（含方法体内的局部类）；
 * 一旦引用 {@code AuthCmdScreen}，在未安装 AvalonBase 的环境下会触发
 * {@code NoClassDefFoundError}。
 * 因此创建 {@code AuthCmdScreen} 一律走反射（{@link #openAuthCmdScreen(Minecraft)}），
 * 并先经 {@link AvalonLink#isAvalonLoaded()} 守卫，未装 AvalonBase 时直接返回。
 */
public class GuiEventHandler {

    private static final ResourceLocation BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/button.png");

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PauseScreen)) return;

            // 编辑界面为「与 AvalonBase 的联动增强」：未安装 AvalonBase 时不注入按钮
            if (!AvalonLink.isAvalonLoaded()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (!AuthCmdConfig.showPauseButton) return;

            int screenWidth = screen.width;
            int btnSize = 20;
            // OnlyTP 按钮在右上角（x = screenWidth-25），AuthCmd 按钮放在其正左侧，留 4px 间距，互不遮挡
            int x = screenWidth - btnSize * 2 - 9;
            int y = 5;

            ((ScreenAccessor) screen).authcmd$invokeAddRenderableWidget(
                    PauseScreenButton.create(x, y, btnSize, BUTTON_TEXTURE, () -> openAuthCmdScreen(mc)));
        });
    }

    /**
     * 通过反射打开 {@code AuthCmdScreen}（继承 {@code AvalonConfigScreen}）。
     * 仅应在 {@code AvalonLink.isAvalonLoaded()} 为 true 时调用。
     */
    private static void openAuthCmdScreen(Minecraft mc) {
        try {
            Class<?> screenClass = Class.forName("com.authcmd.mod.gui.AuthCmdScreen");
            mc.setScreen((net.minecraft.client.gui.screens.Screen) screenClass.getConstructor().newInstance());
        } catch (Exception e) {
            Constants.LOG.error("[AuthCmd] Failed to open AuthCmdScreen via reflection", e);
        }
    }
}
