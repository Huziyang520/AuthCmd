package com.authcmd.mod.gui;

import com.avalon.base.gui.anim.ScreenAnim;
import com.avalon.base.gui.anim.ScreenAnimType;
import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.util.ModMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import java.util.List;

public class AuthCmdAddDialog extends Screen {

    private static final int DIALOG_W = 220;
    private static final int DIALOG_H = 118;

    private final Screen parent;
    private final boolean command;
    private final List<String> targetList;

    private GuiTheme theme;
    private EditBox input;
    private ThemedButton addButton;
    private ThemedButton doneButton;

    private final ScreenAnim anim;
    private boolean closeDone;

    public AuthCmdAddDialog(Screen parent, boolean command, List<String> targetList) {
        super(Component.translatable(command
                ? "gui.authcmd.add_command_title" : "gui.authcmd.add_exempt_title"));
        this.parent = parent;
        this.command = command;
        this.targetList = targetList;
        this.theme = new ModernTheme();
        this.anim = AuthCmdConfig.enableAnimations()
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

        focusInput();
    }

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
            minecraft.player.sendSystemMessage(ModMsg.red(minecraft.player, key));
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {

        anim.tick();
        if (anim.isClosing() && anim.isCloseFinished()) {
            backToParent();
            return;
        }

        super.extractBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {

        mouseX = (int) anim.localX(mouseX, width);
        mouseY = (int) anim.localY(mouseY, height);
        anim.beginFrame(g, width, height);
        int x = (width - DIALOG_W) / 2;
        int y = (height - DIALOG_H) / 2;
        theme.drawPanel(g, x, y, DIALOG_W, DIALOG_H);
        g.text(font, Component.translatable(command
                        ? "gui.authcmd.add_command_title" : "gui.authcmd.add_exempt_title"),
                x + 14, y + 12, theme.labelColor(), false);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        if (theme instanceof ModernTheme modern) {
            ButtonFrames.render(g, this, modern.palette());
        }

        boolean showHand = false;
        if (addButton.isHovered() && addButton.active) showHand = true;
        if (!showHand && doneButton.isHovered()) showHand = true;
        if (!showHand && input.isMouseOver(mouseX, mouseY)) showHand = true;
        setCursor(g, showHand);

        anim.endFrame(g, width, height);

    }

    private void setCursor(GuiGraphicsExtractor g, boolean showHand) {
        if (showHand) g.requestCursor(CursorTypes.POINTING_HAND);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean param) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (input.isMouseOver(mouseX, mouseY)) {
            focusInput();
        }
        return super.mouseClicked(event, param);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation()) {
            addEntry();
            return true;
        }
        return super.keyPressed(event);
    }

    private void backToParent() {
        if (closeDone) return;
        closeDone = true;
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void onClose() {

        if (anim.beginClose()) return;
        backToParent();
    }

    @Override
    public void tick() {
        super.tick();
        anim.tick();
        if (anim.isCloseFinished()) backToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}