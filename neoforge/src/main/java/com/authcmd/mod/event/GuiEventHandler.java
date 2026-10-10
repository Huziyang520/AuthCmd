package com.authcmd.mod.event;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.Constants;
import com.authcmd.mod.client.ClientJoinNotice;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.PauseScreenButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * AuthCmd NeoForge 客户端：在暂停界面注入设置按钮，打开可视化编辑界面。
 *
 * <p><b>关键约束：本类的字节码绝对不能静态引用任何 Avalon 类</b>（尤其是继承自
 * {@code AvalonConfigScreen} 的 {@code AuthCmdScreen}）。因为本类在 mod 构造阶段就会被
 * 加载（主类构造器调用 {@link #register()}），而 JVM 加载类时会执行字节码验证，
 * 解析常量池里引用的所有类型（含方法体内的局部类）。一旦本类方法体引用了
 * {@code AuthCmdScreen}，验证器就会去加载它及其父类 {@code AvalonConfigScreen}；
 * 若客户端未安装 AvalonBase，将抛出 {@code NoClassDefFoundError} 导致整个模组加载失败。
 *
 * <p>因此这里：
 * <ul>
 *   <li>用 {@code NeoForge.EVENT_BUS.addListener(方法引用)} 注册（不做 ASM 方法体织入）；</li>
 *   <li>创建 {@code AuthCmdScreen} 一律走反射（{@link #openAuthCmdScreen(Minecraft)}），
 *       本类字节码不再引用该 Avalon 继承类；</li>
 *   <li>所有 Avalon 联动点先经 {@link AvalonLink#isAvalonLoaded()} 守卫，未装 AvalonBase
 *       时直接返回，永不触发反射加载 Avalon 类。</li>
 * </ul>
 */
public class GuiEventHandler {

    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation(Constants.MOD_ID, "textures/gui/button.png");

    /** 由主入口在构造期调用，将本类的事件处理注册到 NeoForge 事件总线。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuiEventHandler::onClientPlayerLogin);
        NeoForge.EVENT_BUS.addListener(GuiEventHandler::onScreenInit);
    }

    /**
     * 客户端进入世界（本地玩家生成）时，若未安装 AvalonBase 则显示提示。
     */
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientJoinNotice.onJoinWorld();
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;

        // 编辑界面为「与 AvalonBase 的联动增强」：未安装 AvalonBase 时不注入按钮
        if (!AvalonLink.isAvalonLoaded()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!AuthCmdConfig.showPauseButton) return;

        int screenWidth = event.getScreen().width;
        int btnSize = 20;
        // OnlyTP 按钮在右上角（x = screenWidth-25），AuthCmd 按钮放在其正左侧，留 4px 间距，互不遮挡
        int x = screenWidth - btnSize * 2 - 9;
        int y = 5;

        event.addListener(PauseScreenButton.create(x, y, btnSize, BUTTON_TEXTURE, () -> openAuthCmdScreen(mc)));
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