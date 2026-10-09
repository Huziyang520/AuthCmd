package com.authcmd.mod.gui;

import com.avalon.base.gui.GuiCursor;
import com.avalon.base.gui.anim.ScreenAnim;
import com.avalon.base.gui.anim.ScreenAnimType;
import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.util.ModMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * 二级输入弹窗：在主界面点击"新增"后弹出，用于向指令名单或豁免名单连续添加条目。
 *
 * <p>弹窗内输入框常驻，打开时直接聚焦输入框（光标闪烁），可立即输入；
 * 点"添加"只清空输入框而不关闭弹窗，可连续输入多条；点"完成"或按 Esc 关闭弹窗并返回主界面。
 * 目标名单（指令/豁免）在打开弹窗时以参数传入，弹窗只向该名单追加，不会混淆两类条目。
 * 鼠标悬停在按钮/输入框上时显示手形光标。
 */
public class AuthCmdAddDialog extends Screen {

    private static final int DIALOG_W = 220;
    private static final int DIALOG_H = 118;

    private final Screen parent;
    private final boolean command; // true=添加指令，false=添加豁免玩家
    private final List<String> targetList;

    private GuiTheme theme;
    private EditBox input;
    private ThemedButton addButton;
    private ThemedButton doneButton;

    /** 二级弹窗的开/关动画（与主编辑界面同一类型）；「启用动画效果」关闭时完全无动画。 */
    private final ScreenAnim anim;
    private boolean closeDone;

    public AuthCmdAddDialog(Screen parent, boolean command, List<String> targetList) {
        super(Component.translatable(command
                ? "gui.authcmd.add_command_title" : "gui.authcmd.add_exempt_title"));
        this.parent = parent;
        this.command = command;
        this.targetList = targetList;
        this.theme = new ModernTheme();
        this.anim = AuthCmdConfig.enableAnimations
                ? new ScreenAnim(ScreenAnimType.SCALE_BOUNCE, ScreenAnimType.SCALE_BOUNCE)
                : ScreenAnim.disabled();
        this.anim.playOpen();
    }

    @Override
    protected void init() {
        int x = (width - DIALOG_W) / 2;
        int y = (height - DIALOG_H) / 2;

        input = new EditBox(font, x + 14, y + 36, DIALOG_W - 28, 18,
                Component.translatable("gui.authcmd.input_hint"));
        input.setMaxLength(32);
        input.setBordered(true);
        input.setResponder(s -> addButton.active = !s.trim().isEmpty());

        addButton = new ThemedButton(x + DIALOG_W - 68, y + DIALOG_H - 26, 52, 20,
                Component.translatable("gui.authcmd.add"), b -> addEntry(),
                theme, GuiTheme.ButtonRole.PRIMARY, font);
        addButton.active = false;

        doneButton = new ThemedButton(x + DIALOG_W - 126, y + DIALOG_H - 26, 52, 20,
                Component.translatable("gui.authcmd.done"), b -> {
            playClick();
            onClose();
        }, theme, GuiTheme.ButtonRole.NEUTRAL, font);

        addRenderableWidget(input);
        addRenderableWidget(addButton);
        addRenderableWidget(doneButton);

        // 打开弹窗时立即聚焦输入框：确保光标闪烁并可直接键入
        focusInput();
    }

    /**
     * 聚焦输入框。EditBox 光标闪烁依赖 {@link EditBox#isFocused()} 为 true 且 {@link #tick()} 持续刷新。
     * {@code setInitialFocus} 仅登记初始焦点（由 Minecraft 在界面激活时统一处理），直接调用
     * {@code setFocused(true)} 可确保立即生效。
     */
    private void focusInput() {
        setInitialFocus(input);
        input.setFocused(true);
    }

    private void addEntry() {
        String value = input.getValue().trim();
        if (value.isEmpty()) return;
        if (targetList.contains(value)) {
            playerMessage("gui.authcmd.already_in_list");
            input.setValue("");
            focusInput();
            return;
        }
        targetList.add(value);
        input.setValue("");
        playClick();
        focusInput();
    }

    private void playerMessage(String key) {
        if (minecraft != null && minecraft.player != null)
            minecraft.player.displayClientMessage(ModMsg.red(minecraft.player, key), false);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 背景通道交给原版（主菜单 = 全景图 + 模糊 + 菜单背景贴图；世界内 = 模糊 + 半透明暗底）。
        // 它同样位于动画变换之外，动画期间整屏始终是暗的，不会出现"弹窗变小、四周露出一圈更亮"的分层。
        super.renderBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 动画：命中测试用动画坐标系，位姿变换只作用于弹窗本体
        mouseX = (int) anim.localX(mouseX, width);
        mouseY = (int) anim.localY(mouseY, height);
        anim.beginFrame(g, width, height);
        int x = (width - DIALOG_W) / 2;
        int y = (height - DIALOG_H) / 2;
        theme.drawPanel(g, x, y, DIALOG_W, DIALOG_H);
        g.drawString(font, Component.translatable(command
                        ? "gui.authcmd.add_command_title" : "gui.authcmd.add_exempt_title"),
                x + 14, y + 12, theme.labelColor(), false);
        super.render(g, mouseX, mouseY, partialTick);

        // Same one-pixel right-angle button frames as the editor screen.
        if (theme instanceof ModernTheme modern) {
            ButtonFrames.render(g, this, modern.palette());
        }

        // ─── 光标：仅当鼠标真正悬停在按钮或输入框上才显示手形 ───
        // 注意不能用 isHoveredOrFocused()：输入框始终处于聚焦状态（setFocused(true)），
        // isFocused 恒为 true 会导致手形永不消失。这里一律改用"鼠标是否悬停"判断。
        boolean showHand = false;
        if (addButton.isHovered() && addButton.active) showHand = true;
        if (!showHand && doneButton.isHovered()) showHand = true;
        if (!showHand && input.isMouseOver(mouseX, mouseY)) showHand = true;
        setCursor(g, showHand);

        anim.endFrame(g, width, height);
        if (anim.isCloseFinished()) backToParent();
    }

    /**
     * 1.21.8 没有帧末统一 apply 的光标请求队列，光标由 GLFW 直接设置：
     * 只在需要手形时设手形、不设箭头，避免盖掉输入框等原版控件自己设置的光标。
     */
    private void setCursor(GuiGraphics g, boolean showHand) {
        if (showHand) GuiCursor.applyHand();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击输入框时聚焦，确保光标闪烁
        if (input.isMouseOver(mouseX, mouseY)) {
            focusInput();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 1.21.8：主键盘 ENTER=257 / 小键盘 ENTER=335（等价原版的“确认键”）
        if (keyCode == 257 || keyCode == 335) {
            addEntry();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 真正切回父界面（有关闭动画时由动画播完后调用；幂等）。 */
    private void backToParent() {
        if (closeDone) return;
        closeDone = true;
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        // 有关闭动画 → 先播动画，播完再回父界面
        if (anim.beginClose()) return;
        backToParent();
    }

    @Override
    public void tick() {
        super.tick();
        if (anim.isCloseFinished()) backToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
