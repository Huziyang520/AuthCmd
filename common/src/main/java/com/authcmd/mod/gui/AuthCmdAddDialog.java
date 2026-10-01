package com.authcmd.mod.gui;

import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

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

    private long handCursor;
    private long arrowCursor;

    public AuthCmdAddDialog(Screen parent, boolean command, List<String> targetList) {
        super(Component.translatable(command
                ? "gui.authcmd.add_command_title" : "gui.authcmd.add_exempt_title"));
        this.parent = parent;
        this.command = command;
        this.targetList = targetList;
        this.theme = new ModernTheme();
    }

    @Override
    protected void init() {
        handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

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
            minecraft.player.displayClientMessage(Component.translatable(key), false);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no-op：抑制 1.21.1 四参背景。该背景在 super.render 内会被再次调用，否则会模糊已自绘内容。
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x55000000);
        int x = (width - DIALOG_W) / 2;
        int y = (height - DIALOG_H) / 2;
        theme.drawPanel(g, x, y, DIALOG_W, DIALOG_H);
        g.drawString(font, Component.translatable(command
                        ? "gui.authcmd.add_command_title" : "gui.authcmd.add_exempt_title"),
                x + 14, y + 12, theme.labelColor(), false);
        super.render(g, mouseX, mouseY, partialTick);

        // ─── 光标：仅当鼠标真正悬停在按钮或输入框上才显示手形 ───
        // 注意不能用 isHoveredOrFocused()：输入框始终处于聚焦状态（setFocused(true)），
        // isFocused 恒为 true 会导致手形永不消失。这里一律改用“鼠标是否悬停”判断。
        boolean showHand = false;
        if (addButton.isHovered() && addButton.active) showHand = true;
        if (!showHand && doneButton.isHovered()) showHand = true;
        if (!showHand && input.isMouseOver(mouseX, mouseY)) showHand = true;
        setCursor(showHand);
    }

    private void setCursor(boolean showHand) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetCursor(window, showHand ? handCursor : arrowCursor);
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
        if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
            addEntry();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (handCursor != 0) GLFW.glfwDestroyCursor(handCursor);
        if (arrowCursor != 0) GLFW.glfwDestroyCursor(arrowCursor);
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
