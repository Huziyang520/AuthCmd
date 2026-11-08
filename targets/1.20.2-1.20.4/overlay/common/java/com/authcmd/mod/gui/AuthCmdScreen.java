package com.authcmd.mod.gui;

import com.authcmd.mod.config.AuthCmdConfig;
import com.avalon.base.gui.anim.ScreenAnimType;
import com.avalon.base.gui.anim.ScrollAnim;
import com.avalon.base.gui.panel.PanelHover;
import com.avalon.base.gui.screen.AvalonConfigScreen;
import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import com.avalon.base.gui.theme.ThemedRadio;
import com.avalon.base.gui.theme.ThemedToggle;
import com.avalon.base.gui.util.MouseButtons;
import com.avalon.base.gui.util.TextFit;
import com.camelot.net.CamelotNet;
import com.camelot.ui.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class AuthCmdScreen extends AvalonConfigScreen {

    private static final String[] MODES =
            {AuthCmdConfig.MODE_NON_OP, AuthCmdConfig.MODE_OP, AuthCmdConfig.MODE_BOTH, AuthCmdConfig.MODE_DISABLED};

    private static final int LIST_CARD_Y = 92;
    private static final int LIST_LEFT_X = 8;
    private static final int LIST_RIGHT_X = 154;
    private static final int LIST_CARD_W = 138;
    private static final int LIST_CARD_H = 92;
    private static final int LIST_VISIBLE_ROWS = 3;

    private static final int LABEL_MAX_W = LIST_CARD_W - 34;

    private String mode;
    private boolean showPauseButton;
    private final List<String> nonOpWhitelist = new ArrayList<>();
    private final List<String> opBlacklist = new ArrayList<>();
    private final List<String> nonOpExempt = new ArrayList<>();
    private final List<String> opExempt = new ArrayList<>();

    private final ThemedRadio[] modeRadios = new ThemedRadio[4];
    private ThemedToggle showPauseToggle;
    private ThemedToggle enableAnimationsToggle;
    private ThemedToggle allowSelectorsToggle;

    private boolean enableAnimations;

    private boolean allowEntitySelectors;
    private ThemedButton addCommandButton;
    private ThemedButton addExemptButton;
    private int hoveredCommandRow = -1;
    private int hoveredExemptRow = -1;
    private int exemptScroll;

    private final ScrollAnim blAnim = new ScrollAnim(0);
    private final ScrollAnim exemptAnim = new ScrollAnim(0);

    private String rowRemoveHint;

    public AuthCmdScreen(Screen parent) {

        super(Component.translatable("gui.authcmd.title"), parent, Minecraft.getInstance().player == null);

        setTheme(new ModernTheme());
        this.mode = AuthCmdConfig.mode();
        this.showPauseButton = AuthCmdConfig.enableConfigEntry();
        this.enableAnimations = AuthCmdConfig.enableAnimations();
        this.allowEntitySelectors = AuthCmdConfig.allowEntitySelectors();
        this.nonOpWhitelist.addAll(AuthCmdConfig.nonOpWhitelist());
        this.opBlacklist.addAll(AuthCmdConfig.opBlacklist());
        this.nonOpExempt.addAll(AuthCmdConfig.nonOpExempt());
        this.opExempt.addAll(AuthCmdConfig.opExempt());

        configureAnimations(enableAnimations, ScreenAnimType.SCALE_BOUNCE, ScreenAnimType.SCALE_BOUNCE);
        playOpenAnimation();
    }

    private boolean modeHasCommand() {
        return AuthCmdConfig.MODE_NON_OP.equals(mode) || AuthCmdConfig.MODE_OP.equals(mode);
    }

    private boolean modeIsNonOp() {
        return AuthCmdConfig.MODE_NON_OP.equals(mode);
    }

    private List<String> currentCommandList() {
        return modeIsNonOp() ? nonOpWhitelist : opBlacklist;
    }

    private List<String> currentExemptList() {
        return modeIsNonOp() ? nonOpExempt : opExempt;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < 4; i++) {
            modeRadios[i] = new ThemedRadio(guiLeft + LIST_LEFT_X + 6, 30 + i * 12,
                    Component.translatable("gui.authcmd.mode_" + MODES[i]),
                    MODES[i].equals(mode), canEdit, LABEL_MAX_W);
        }

        showPauseToggle = new ThemedToggle(guiLeft + LIST_RIGHT_X + 6, 30,
                Component.translatable("gui.authcmd.show_pause_button"), showPauseButton, canEdit, false, LABEL_MAX_W);

        enableAnimationsToggle = new ThemedToggle(guiLeft + LIST_RIGHT_X + 6, 42,
                Component.translatable("gui.authcmd.enable_animations"), enableAnimations, canEdit, false, LABEL_MAX_W);

        allowSelectorsToggle = new ThemedToggle(guiLeft + LIST_RIGHT_X + 6, 54,
                Component.translatable("gui.authcmd.allow_entity_selectors"), allowEntitySelectors, canEdit, false, LABEL_MAX_W);

        addCommandButton = null;
        addExemptButton = null;
        if (canEdit && modeHasCommand()) {
            int btnY = LIST_CARD_Y + LIST_CARD_H - 26;
            addCommandButton = new ThemedButton(guiLeft + LIST_LEFT_X + LIST_CARD_W - 60, yo(btnY), 52, 16,
                    Component.translatable("gui.authcmd.new_entry"), b -> openAddDialog(true),
                    theme, GuiTheme.ButtonRole.PRIMARY, font);
            addRenderableWidget(addCommandButton);

            addExemptButton = new ThemedButton(guiLeft + LIST_RIGHT_X + LIST_CARD_W - 60, yo(btnY), 52, 16,
                    Component.translatable("gui.authcmd.new_entry"), b -> openAddDialog(false),
                    theme, GuiTheme.ButtonRole.PRIMARY, font);
            addRenderableWidget(addExemptButton);
        }

        if (canEdit) {
            ThemedButton save = new ThemedButton(guiLeft + GUI_WIDTH - 128, yo(194), 55, 20,
                    saveButtonLabel(), b -> onSavePressed(),
                    theme, GuiTheme.ButtonRole.PRIMARY, font);
            addRenderableWidget(save);
        }
        ThemedButton cancel = new ThemedButton(guiLeft + GUI_WIDTH - 68, yo(194), 55, 20,
                cancelButtonLabel(), b -> onClose(),
                theme, GuiTheme.ButtonRole.NEUTRAL, font);
        addRenderableWidget(cancel);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        mouseX = (int) animation().localX(mouseX, width);
        mouseY = (int) animation().localY(mouseY, height);

        beginAnimatedRender(graphics);

        int panelH = yo(CONTENT_MAX_Y) + 14 - (yo(CONTENT_MIN_Y) - 4);
        theme.drawPanel(graphics, guiLeft, yo(CONTENT_MIN_Y) - 4, GUI_WIDTH, panelH);

        int cardLeftX = LIST_LEFT_X, cardLeftW = LIST_CARD_W;
        theme.drawCard(graphics, guiLeft + cardLeftX, yo(14), cardLeftW, 70);
        graphics.drawString(font, Component.translatable("gui.authcmd.mode_label"),
                guiLeft + cardLeftX + 6, yo(18), theme.labelColor(), false);
        for (ThemedRadio r : modeRadios) r.render(graphics, font, theme, yo(r.relY), mouseX, mouseY);

        int cardRightX = LIST_RIGHT_X, cardRightW = LIST_CARD_W;
        theme.drawCard(graphics, guiLeft + cardRightX, yo(14), cardRightW, 70);
        graphics.drawString(font, Component.translatable("gui.authcmd.toggle_label"),
                guiLeft + cardRightX + 6, yo(18), theme.labelColor(), false);
        showPauseToggle.render(graphics, font, theme, yo(showPauseToggle.relY), mouseX, mouseY);
        enableAnimationsToggle.render(graphics, font, theme, yo(enableAnimationsToggle.relY), mouseX, mouseY);
        allowSelectorsToggle.render(graphics, font, theme, yo(allowSelectorsToggle.relY), mouseX, mouseY);

        hoveredCommandRow = -1;
        hoveredExemptRow = -1;
        rowRemoveHint = null;

        if (modeHasCommand()) {
            renderList(graphics, mouseX, mouseY,
                    Component.translatable("gui.authcmd.command_list_label"),
                    currentCommandList(), LIST_LEFT_X, LIST_CARD_Y, true);

            renderList(graphics, mouseX, mouseY,
                    Component.translatable("gui.authcmd.exempt_list_label"),
                    currentExemptList(), LIST_RIGHT_X, LIST_CARD_Y, false);
        }

        renderStatus(graphics, 200);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (theme instanceof ModernTheme modern) {
            ButtonFrames.render(graphics, this, modern.palette());
        }

        boolean showHand = false;
        if (canEdit) {
            for (var w : children()) {
                if (w instanceof ThemedButton btn && btn.active && btn.isHoveredOrFocused()) {
                    showHand = true;
                    break;
                }
            }
            if (!showHand) {
                for (int i = 0; i < 4; i++) {
                    if (modeRadios[i].isClicked(mouseX, mouseY, font, yo(modeRadios[i].relY))) {
                        showHand = true;
                        break;
                    }
                }
            }
            if (!showHand && (showPauseToggle.isClicked(mouseX, mouseY, font, yo(showPauseToggle.relY))
                    || enableAnimationsToggle.isClicked(mouseX, mouseY, font, yo(enableAnimationsToggle.relY))
                    || allowSelectorsToggle.isClicked(mouseX, mouseY, font, yo(allowSelectorsToggle.relY)))) {
                showHand = true;
            }
            if (!showHand && (hoveredCommandRow >= 0 || hoveredExemptRow >= 0)) showHand = true;
        }
        updateCursor(showHand);

        String tip = null;
        for (ThemedRadio r : modeRadios) {
            tip = r.truncatedTooltip(font, mouseX, mouseY, yo(r.relY));
            if (tip != null) break;
        }
        if (tip == null) {
            tip = showPauseToggle.truncatedTooltip(font, mouseX, mouseY, yo(showPauseToggle.relY));
        }
        if (tip == null) {
            tip = enableAnimationsToggle.truncatedTooltip(font, mouseX, mouseY, yo(enableAnimationsToggle.relY));
        }
        if (tip == null) {
            tip = allowSelectorsToggle.truncatedTooltip(font, mouseX, mouseY, yo(allowSelectorsToggle.relY));
        }
        if (tip != null) {
            PanelHover.render(graphics, font, tip, mouseX, mouseY, this.width, this.height);
        }

        if (rowRemoveHint != null) {
            PanelHover.render(graphics, font, rowRemoveHint, mouseX, mouseY, this.width, this.height, 0xFFFF5555);
        }

        endAnimatedRender(graphics);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY, Component label,
                            List<String> list, int cardX, int cardY, boolean isCommand) {
        int cx = guiLeft + cardX;
        theme.drawCard(graphics, cx, yo(cardY), LIST_CARD_W, LIST_CARD_H);
        graphics.drawString(font, label, cx + 8, yo(cardY + 4), theme.labelColor(), false);

        int listStartY = cardY + 22;
        int scroll = isCommand ? blAnim.displayValue() : exemptAnim.displayValue();
        int visible = Math.min(LIST_VISIBLE_ROWS, list.size());

        int rowTextW = LIST_CARD_W - 16;
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            int y = yo(listStartY + i * LIST_ITEM_H) - 6;
            boolean rowHover = canEdit && mouseX >= cx + 4 && mouseX <= cx + LIST_CARD_W - 6
                    && mouseY >= y - 2 && mouseY < y + LIST_ITEM_H - 2;
            if (rowHover) {
                if (isCommand) hoveredCommandRow = idx;
                else hoveredExemptRow = idx;
                graphics.fill(cx + 4, y - 2, cx + LIST_CARD_W - 6, y + LIST_ITEM_H - 2, 0x2AB47AE8);

                rowRemoveHint = Component.translatable("gui.authcmd.click_to_remove").getString();
                }
            String text = TextFit.fit(font, list.get(idx), rowTextW);
            graphics.drawString(font, text, cx + 8, y, canEdit ? theme.textColor() : theme.disabledColor(), false);
        }

        if (list.size() > LIST_VISIBLE_ROWS) {
            int tx = cx + LIST_CARD_W - 5;
            int tTop = yo(listStartY) - 6;
            int tBot = yo(listStartY + LIST_VISIBLE_ROWS * LIST_ITEM_H) - 8;
            theme.drawScrollTrack(graphics, tx, tTop, 4, tBot - tTop);
            int trackH = tBot - tTop;
            int thumbH = Math.max(8, trackH * LIST_VISIBLE_ROWS / list.size());
            float p = (isCommand ? blAnim.value() : exemptAnim.value()) / Math.max(1, list.size() - LIST_VISIBLE_ROWS);
            theme.drawScrollThumb(graphics, tx, tTop + Math.round((trackH - thumbH) * p), 4, thumbH);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseButtons.isLeft(button) && canEdit) {

            for (int i = 0; i < 4; i++) {
                if (modeRadios[i].isClicked(mouseX, mouseY, font, yo(modeRadios[i].relY))) {
                    if (!MODES[i].equals(mode)) {
                        playClickSound();
                        mode = MODES[i];
                        blScroll = 0;
                        exemptScroll = 0;
                        blAnim.snapTo(0);
                        exemptAnim.snapTo(0);
                        reloadWidgets();
                    }
                    return true;
                }
            }

            if (showPauseToggle.isClicked(mouseX, mouseY, font, yo(showPauseToggle.relY))) {
                playClickSound();
                showPauseButton = !showPauseButton;
                showPauseToggle.setChecked(showPauseButton);
                return true;
            }

            if (enableAnimationsToggle.isClicked(mouseX, mouseY, font, yo(enableAnimationsToggle.relY))) {
                playClickSound();
                enableAnimations = !enableAnimations;
                enableAnimationsToggle.setChecked(enableAnimations);
                configureAnimations(enableAnimations, ScreenAnimType.SCALE_BOUNCE, ScreenAnimType.SCALE_BOUNCE);
                return true;
            }

            if (allowSelectorsToggle.isClicked(mouseX, mouseY, font, yo(allowSelectorsToggle.relY))) {
                playClickSound();
                allowEntitySelectors = !allowEntitySelectors;
                allowSelectorsToggle.setChecked(allowEntitySelectors);
                return true;
            }

            if (hoveredCommandRow >= 0 && modeHasCommand()) {
                List<String> list = currentCommandList();
                if (hoveredCommandRow < list.size()) {
                    playClickSound();
                    list.remove(hoveredCommandRow);
                    blScroll = Mth.clamp(blScroll, 0, Math.max(0, list.size() - LIST_VISIBLE_ROWS));
                    blAnim.setTarget(blScroll);
                    hoveredCommandRow = -1;
                    return true;
                }
            }

            if (hoveredExemptRow >= 0 && modeHasCommand()) {
                List<String> list = currentExemptList();
                if (hoveredExemptRow < list.size()) {
                    playClickSound();
                    list.remove(hoveredExemptRow);
                    exemptScroll = Mth.clamp(exemptScroll, 0, Math.max(0, list.size() - LIST_VISIBLE_ROWS));
                    exemptAnim.setTarget(exemptScroll);
                    hoveredExemptRow = -1;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!modeHasCommand()) return false;

        int listTop = yo(LIST_CARD_Y + 22) - 6;
        int listBot = yo(LIST_CARD_Y + 22 + LIST_VISIBLE_ROWS * LIST_ITEM_H) - 8;
        if (mouseY < listTop || mouseY >= listBot) return false;
        int dir = -(int) Math.signum(scrollY);

        int lx = guiLeft + LIST_LEFT_X;
        if (mouseX >= lx && mouseX <= lx + LIST_CARD_W && currentCommandList().size() > LIST_VISIBLE_ROWS) {
            blScroll = Mth.clamp(blScroll + dir, 0, currentCommandList().size() - LIST_VISIBLE_ROWS);
            blAnim.setTarget(blScroll);
            return true;
        }

        int rx = guiLeft + LIST_RIGHT_X;
        if (mouseX >= rx && mouseX <= rx + LIST_CARD_W && currentExemptList().size() > LIST_VISIBLE_ROWS) {
            exemptScroll = Mth.clamp(exemptScroll + dir, 0, currentExemptList().size() - LIST_VISIBLE_ROWS);
            exemptAnim.setTarget(exemptScroll);
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        blAnim.tick();
        exemptAnim.tick();
    }

    private void reloadWidgets() {
        clearWidgets();
        init();
    }

    private void openAddDialog(boolean command) {
        if (minecraft == null) return;
        playClickSound();
        List<String> target = command ? currentCommandList() : currentExemptList();
        Screens.open(new AuthCmdAddDialog(this, command, target));
    }

    @Override
    protected void saveConfig() {
        if (!canEdit) return;
        AuthCmdConfig.applyAndSave(mode, allowEntitySelectors, showPauseButton, enableAnimations,
                nonOpWhitelist, opBlacklist, nonOpExempt, opExempt);

        CamelotNet.sendUpdate(AuthCmdConfig.CONFIG);
    }

    @Override
    protected Component saveButtonLabel() {
        return Component.translatable("gui.authcmd.save");
    }

    @Override
    protected Component cancelButtonLabel() {
        return Component.translatable("gui.authcmd.cancel");
    }

    @Override
    protected Component viewOnlyText() {
        return Component.translatable("gui.authcmd.view_only");
    }

    @Override
    protected Component canEditText() {
        return Component.translatable("gui.authcmd.can_edit");
    }
}
