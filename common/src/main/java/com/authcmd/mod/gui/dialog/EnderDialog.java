package com.authcmd.mod.gui.dialog;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.gui.ButtonFrames;
import com.avalon.base.gui.anim.ScreenAnim;
import com.avalon.base.gui.anim.ScreenAnimType;
import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import com.avalon.base.gui.util.TextFit;
import com.camelot.ui.Screens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

abstract class EnderDialog extends Screen {

    protected static final int PANEL_W = 260;
    protected static final int BTN_H = 20;
    protected static final int SIDE = 12;
    protected static final int ROW_GAP = 8;
    protected static final int BOTTOM_PAD = 12;

    private static final int TITLE_Y = 9;
    private static final int TEXT_Y = 27;
    private static final int LINE_H = 11;

    protected final Screen parentScreen;
    protected final GuiTheme theme;
    protected int left;
    protected int top;
    protected int panelW;
    protected int panelH;
    protected List<String> lines = List.of();

    private final Component bodyText;

    private final ScreenAnim anim;
    private boolean closeDone;

    protected EnderDialog(Screen parent, Component title, Component body, GuiTheme theme) {
        super(title);
        this.parentScreen = parent;
        this.bodyText = body;
        this.theme = theme;
        this.anim = AuthCmdConfig.enableAnimations()
                ? new ScreenAnim(ScreenAnimType.SCALE_BOUNCE, ScreenAnimType.SCALE_BOUNCE)
                : ScreenAnim.disabled();
        this.anim.playOpen();
    }

    protected abstract int minPanelH();

    protected abstract int bottomHeight();

    protected abstract void addDialogWidgets();

    @Override
    protected void init() {
        this.lines = TextFit.wrap(font, bodyText.getString(), PANEL_W - 2 * SIDE);
        this.panelW = PANEL_W;
        this.panelH = Math.max(minPanelH(), TEXT_Y + lines.size() * LINE_H + bottomHeight());
        this.left = (width - panelW) / 2;
        this.top = Math.max(4, (height - panelH) / 2);
        addDialogWidgets();
    }

    protected int buttonsY() {
        return top + panelH - BOTTOM_PAD - BTN_H;
    }

    protected ThemedButton addDialogButton(int x, int w, Component label,
                                           GuiTheme.ButtonRole role, Button.OnPress onPress) {
        ThemedButton btn = new ThemedButton(x, buttonsY(), w, BTN_H, label, onPress, theme, role, font);
        addRenderableWidget(btn);
        return btn;
    }

    protected void renderDialogContents(GuiGraphics g, int mouseX, int mouseY) {
    }

    protected void backToParent() {
        if (closeDone) return;
        closeDone = true;
        if (minecraft != null) {
            Screens.open(parentScreen);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        anim.tick();
        if (anim.isClosing() && anim.isCloseFinished()) {
            backToParent();
            return;
        }
        super.renderBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);

        mouseX = (int) anim.localX(mouseX, width);
        mouseY = (int) anim.localY(mouseY, height);
        anim.beginFrame(g, width, height);
        theme.drawPanel(g, left, top, panelW, panelH);
        g.drawCenteredString(font, getTitle(), left + panelW / 2, top + TITLE_Y, theme.titleColor());
        int y = top + TEXT_Y;
        for (String line : lines) {
            g.drawString(font, line, left + SIDE, y, theme.textColor(), false);
            y += LINE_H;
        }
        renderDialogContents(g, mouseX, mouseY);
        for (var child : children()) {
            if (child instanceof Renderable childRenderable) {
                childRenderable.render(g, mouseX, mouseY, delta);
            }
        }

        if (theme instanceof ModernTheme modern) {
            ButtonFrames.render(g, this, modern.palette());
        }
        anim.endFrame(g, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {

        if (anim.beginClose()) return;
        backToParent();
    }

    @Override
    public void tick() {
        super.tick();
        if (anim.isCloseFinished()) backToParent();
    }
}
