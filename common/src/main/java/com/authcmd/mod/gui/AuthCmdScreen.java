package com.authcmd.mod.gui;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.network.AuthCmdUpdatePacket;
import com.authcmd.mod.network.NetworkChannels;
import com.avalon.base.gui.panel.PanelHover;
import com.avalon.base.gui.screen.AvalonConfigScreen;
import com.avalon.base.gui.util.TextFit;
import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import com.avalon.base.gui.theme.ThemedRadio;
import com.avalon.base.gui.theme.ThemedToggle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * AuthCmd 配置界面（双套指令权限控制器，仅末影紫主题）。
 *
 * <p>布局：生效范围 4 模式单选、显示暂停按钮开关、指令名单列表、豁免玩家列表、
 * 每个名单卡片带「新增」按钮（点击弹出 {@link AuthCmdAddDialog} 二级输入弹窗，可连续添加）、
 * 保存/取消。
 */
public class AuthCmdScreen extends AvalonConfigScreen {

    private static final String[] MODES =
            {AuthCmdConfig.MODE_NON_OP, AuthCmdConfig.MODE_OP, AuthCmdConfig.MODE_BOTH, AuthCmdConfig.MODE_DISABLED};

    // ─── 名单卡片布局常量（左右并排，整体滚动） ───
    private static final int LIST_CARD_Y = 92;     // 名单卡片顶部
    private static final int LIST_LEFT_X = 8;      // 指令名单卡（左半）
    private static final int LIST_RIGHT_X = 154;   // 豁免名单卡（右半）
    private static final int LIST_CARD_W = 138;    // 每张卡片宽度
    private static final int LIST_CARD_H = 92;     // 每张卡片高度
    private static final int LIST_VISIBLE_ROWS = 3; // 每卡可见行数
    // 单选/开关标签最大可用宽度：卡片宽 138 - 左内边距(+6) - 勾选框+间距(+24) - 右内边距(4)。
    // 超过该宽度的长标签（英文串）在控件内自动截断加省略号，避免文字溢出卡片边框。
    private static final int LABEL_MAX_W = LIST_CARD_W - 34; // 104

    // ─── 配置状态 ───
    private String mode;
    private boolean showPauseButton;
    private final List<String> nonOpWhitelist = new ArrayList<>();
    private final List<String> opBlacklist = new ArrayList<>();
    private final List<String> nonOpExempt = new ArrayList<>();
    private final List<String> opExempt = new ArrayList<>();

    // ─── 控件 ───
    private final ThemedRadio[] modeRadios = new ThemedRadio[4];
    private ThemedToggle showPauseToggle;
    private ThemedButton addCommandButton; // 指令名单「新增」按钮
    private ThemedButton addExemptButton;  // 豁免名单「新增」按钮
    private int hoveredCommandRow = -1;
    private int hoveredExemptRow = -1;
    private int exemptScroll;
    // Red "click to remove" hint shown in a PanelHover box while a row is hovered.
    private String rowRemoveHint;

    public AuthCmdScreen() {
        this(null, false);
    }

    public AuthCmdScreen(Screen parent, boolean localEdit) {
        super(Component.translatable("gui.authcmd.title"), parent, localEdit);
        // Ender Purple palette stays; the one-pixel button frame is drawn by
        // ButtonFrames after the widgets render (a wrapper theme would make
        // ThemedButton fall back to zero-alpha text color and hide labels).
        setTheme(new ModernTheme());
        this.mode = AuthCmdConfig.mode;
        this.showPauseButton = AuthCmdConfig.showPauseButton;
        this.nonOpWhitelist.addAll(AuthCmdConfig.nonOpWhitelist);
        this.opBlacklist.addAll(AuthCmdConfig.opBlacklist);
        this.nonOpExempt.addAll(AuthCmdConfig.nonOpExempt);
        this.opExempt.addAll(AuthCmdConfig.opExempt);
    }

    /** 是否在 GUI 中显示名单列表卡片（仅单功能模式下展示；both 模式两个功能独立生效但不显示）。 */
    private boolean modeHasCommand() {
        return AuthCmdConfig.MODE_NON_OP.equals(mode) || AuthCmdConfig.MODE_OP.equals(mode);
    }

    private boolean modeIsNonOp() {
        return AuthCmdConfig.MODE_NON_OP.equals(mode);
    }

    /** 当前指令名单。 */
    private List<String> currentCommandList() {
        return modeIsNonOp() ? nonOpWhitelist : opBlacklist;
    }

    /** 当前豁免名单。 */
    private List<String> currentExemptList() {
        return modeIsNonOp() ? nonOpExempt : opExempt;
    }

    @Override
    protected void init() {
        super.init();

        // ─── 生效范围 4 模式单选（卡片内左缘 +6 内边距，与下方卡片左缘对齐） ───
        for (int i = 0; i < 4; i++) {
            modeRadios[i] = new ThemedRadio(guiLeft + LIST_LEFT_X + 6, 30 + i * 12,
                    Component.translatable("gui.authcmd.mode_" + MODES[i]),
                    MODES[i].equals(mode), canEdit, LABEL_MAX_W);
        }

        // ─── 显示暂停按钮开关（卡片内左缘 +6 内边距，与下方卡片左缘对齐） ───
        showPauseToggle = new ThemedToggle(guiLeft + LIST_RIGHT_X + 6, 30,
                Component.translatable("gui.authcmd.show_pause_button"), showPauseButton, canEdit, false, LABEL_MAX_W);

        // ─── 名单卡片「新增」按钮（弹窗连续添加，位于各卡右下角） ───
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

        // ─── 保存/取消（相对基类标准位置整体左移 10px） ───
        // The base helper pins the buttons to GUI_WIDTH-118/58; AuthCmd wants the
        // pair a little further left, so the buttons are built locally instead.
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

    // ═══════════ 渲染 ═══════════

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackdrop(graphics);

        int panelH = yo(CONTENT_MAX_Y) + 14 - (yo(CONTENT_MIN_Y) - 4);
        theme.drawPanel(graphics, guiLeft, yo(CONTENT_MIN_Y) - 4, GUI_WIDTH, panelH);

        // ─── 生效范围卡片（左半，70px 高，与下方左卡同尺寸对齐） ───
        int cardLeftX = LIST_LEFT_X, cardLeftW = LIST_CARD_W;
        theme.drawCard(graphics, guiLeft + cardLeftX, yo(14), cardLeftW, 70);
        graphics.text(font, Component.translatable("gui.authcmd.mode_label"),
                guiLeft + cardLeftX + 6, yo(18), theme.labelColor(), false);
        for (ThemedRadio r : modeRadios) r.render(graphics, font, theme, yo(r.relY), mouseX, mouseY);

        // ─── 开关卡片（右半，与下方右卡同尺寸对齐） ───
        int cardRightX = LIST_RIGHT_X, cardRightW = LIST_CARD_W;
        theme.drawCard(graphics, guiLeft + cardRightX, yo(14), cardRightW, 70);
        graphics.text(font, Component.translatable("gui.authcmd.toggle_label"),
                guiLeft + cardRightX + 6, yo(18), theme.labelColor(), false);
        showPauseToggle.render(graphics, font, theme, yo(showPauseToggle.relY), mouseX, mouseY);

        hoveredCommandRow = -1;
        hoveredExemptRow = -1;
        rowRemoveHint = null;

        // ─── 指令/豁免名单卡片（左右并排，各自整体滚动） ───
        if (modeHasCommand()) {
            // 指令名单卡（左半）
            renderList(graphics, mouseX, mouseY,
                    Component.translatable("gui.authcmd.command_list_label"),
                    currentCommandList(), LIST_LEFT_X, LIST_CARD_Y, true);
            // 豁免名单卡（右半）
            renderList(graphics, mouseX, mouseY,
                    Component.translatable("gui.authcmd.exempt_list_label"),
                    currentExemptList(), LIST_RIGHT_X, LIST_CARD_Y, false);
        }

        // ─── 底部状态 ───
        renderStatus(graphics, 212);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // One-pixel right-angle frames on top of every active themed button.
        if (theme instanceof ModernTheme modern) {
            ButtonFrames.render(graphics, this, modern.palette());
        }

        // ─── 光标 ───
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
            if (!showHand && showPauseToggle.isClicked(mouseX, mouseY, font, yo(showPauseToggle.relY))) {
                showHand = true;
            }
            if (!showHand && (hoveredCommandRow >= 0 || hoveredExemptRow >= 0)) showHand = true;
        }
        updateCursor(showHand);

        // Hover box for truncated radio/toggle labels: show the full original text.
        // Drawn last (above cards, widgets and the read-only overlay) so it is never covered.
        String tip = null;
        for (ThemedRadio r : modeRadios) {
            tip = r.truncatedTooltip(font, mouseX, mouseY, yo(r.relY));
            if (tip != null) break;
        }
        if (tip == null) {
            tip = showPauseToggle.truncatedTooltip(font, mouseX, mouseY, yo(showPauseToggle.relY));
        }
        if (tip != null) {
            PanelHover.render(graphics, font, tip, mouseX, mouseY, this.width, this.height);
        }

        // Red row-action hint ("click to remove") in the gold hover box.
        if (rowRemoveHint != null) {
            PanelHover.render(graphics, font, rowRemoveHint, mouseX, mouseY, this.width, this.height, 0xFFFF5555);
        }
    }

    /** 渲染一个可整体滚动的名单卡片（左右并排，cardX 决定左右半区）。 */
    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Component label,
                            List<String> list, int cardX, int cardY, boolean isCommand) {
        int cx = guiLeft + cardX;
        theme.drawCard(graphics, cx, yo(cardY), LIST_CARD_W, LIST_CARD_H);
        graphics.text(font, label, cx + 8, yo(cardY + 4), theme.labelColor(), false);

        int listStartY = cardY + 22;
        int scroll = isCommand ? blScroll : exemptScroll;
        int visible = Math.min(LIST_VISIBLE_ROWS, list.size());
        // Row text may use the full inner width (8px insets on both sides); long
        // entries are ellipsised by TextFit instead of overlapping anything.
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
                // The red "click to remove" hint is now shown in a PanelHover box
                // (rendered on top at the end of the frame) instead of inside the row,
                // so it can never overlap long localized entry text.
                rowRemoveHint = Component.translatable("gui.authcmd.click_to_remove").getString();
                // ─────────────────────────────────────────────────────────────
                // 备用方案：行内右对齐红字提示（悬停时直接绘制在名单行右侧）。
                // 该方案作为备用方案，不得随意删除。
                // String hint = Component.translatable("gui.authcmd.click_to_remove").getString();
                // // 颜色必须带 FF 不透明 alpha（0xFFFF5555）；写成 0xFF5555 会被当作 ARGB → alpha=0 → 全透明不可见
                // graphics.text(font, hint, cx + LIST_CARD_W - 8 - font.width(hint), y, 0xFFFF5555, false);
                // ─────────────────────────────────────────────────────────────
            }
            String text = TextFit.fit(font, list.get(idx), rowTextW);
            graphics.text(font, text, cx + 8, y, canEdit ? theme.textColor() : theme.disabledColor(), false);
        }

        // 滚动条（卡内右侧）
        if (list.size() > LIST_VISIBLE_ROWS) {
            int tx = cx + LIST_CARD_W - 5;
            int tTop = yo(listStartY) - 6;
            int tBot = yo(listStartY + LIST_VISIBLE_ROWS * LIST_ITEM_H) - 8;
            theme.drawScrollTrack(graphics, tx, tTop, 4, tBot - tTop);
            int trackH = tBot - tTop;
            int thumbH = Math.max(8, trackH * LIST_VISIBLE_ROWS / list.size());
            float p = (float) scroll / Math.max(1, list.size() - LIST_VISIBLE_ROWS);
            theme.drawScrollThumb(graphics, tx, tTop + Math.round((trackH - thumbH) * p), 4, thumbH);
        }
    }

    // ═══════════ 交互 ═══════════

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean param) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && canEdit) {
            // 模式单选
            for (int i = 0; i < 4; i++) {
                if (modeRadios[i].isClicked(mouseX, mouseY, font, yo(modeRadios[i].relY))) {
                    if (!MODES[i].equals(mode)) {
                        playClickSound();
                        mode = MODES[i];
                        blScroll = 0;
                        exemptScroll = 0;
                        reloadWidgets();
                    }
                    return true;
                }
            }
            // 显示暂停按钮开关
            if (showPauseToggle.isClicked(mouseX, mouseY, font, yo(showPauseToggle.relY))) {
                playClickSound();
                showPauseButton = !showPauseButton;
                showPauseToggle.setChecked(showPauseButton);
                return true;
            }
            // 指令名单删除
            if (hoveredCommandRow >= 0 && modeHasCommand()) {
                List<String> list = currentCommandList();
                if (hoveredCommandRow < list.size()) {
                    playClickSound();
                    list.remove(hoveredCommandRow);
                    blScroll = Mth.clamp(blScroll, 0, Math.max(0, list.size() - LIST_VISIBLE_ROWS));
                    hoveredCommandRow = -1;
                    return true;
                }
            }
            // 豁免名单删除
            if (hoveredExemptRow >= 0 && modeHasCommand()) {
                List<String> list = currentExemptList();
                if (hoveredExemptRow < list.size()) {
                    playClickSound();
                    list.remove(hoveredExemptRow);
                    exemptScroll = Mth.clamp(exemptScroll, 0, Math.max(0, list.size() - LIST_VISIBLE_ROWS));
                    hoveredExemptRow = -1;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, param);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!modeHasCommand()) return false;
        // 列表行区域（卡片内，标题下方到按钮上方）
        int listTop = yo(LIST_CARD_Y + 22) - 6;
        int listBot = yo(LIST_CARD_Y + 22 + LIST_VISIBLE_ROWS * LIST_ITEM_H) - 8;
        if (mouseY < listTop || mouseY >= listBot) return false;
        int dir = -(int) Math.signum(scrollY);

        // 左栏：指令名单
        int lx = guiLeft + LIST_LEFT_X;
        if (mouseX >= lx && mouseX <= lx + LIST_CARD_W && currentCommandList().size() > LIST_VISIBLE_ROWS) {
            blScroll = Mth.clamp(blScroll + dir, 0, currentCommandList().size() - LIST_VISIBLE_ROWS);
            return true;
        }
        // 右栏：豁免名单
        int rx = guiLeft + LIST_RIGHT_X;
        if (mouseX >= rx && mouseX <= rx + LIST_CARD_W && currentExemptList().size() > LIST_VISIBLE_ROWS) {
            exemptScroll = Mth.clamp(exemptScroll + dir, 0, currentExemptList().size() - LIST_VISIBLE_ROWS);
            return true;
        }
        return false;
    }

    // ═══════════ 辅助方法 ═══════════

    private void reloadWidgets() {
        clearWidgets();
        init();
    }

    /** 打开二级输入弹窗，向指定名单（true=指令 / false=豁免）连续添加。 */
    private void openAddDialog(boolean command) {
        if (minecraft == null) return;
        playClickSound();
        List<String> target = command ? currentCommandList() : currentExemptList();
        minecraft.setScreenAndShow(new AuthCmdAddDialog(this, command, target));
    }

    @Override
    protected void saveConfig() {
        if (!canEdit) return;
        // Local edits (opened from the main-menu mod list) only persist to the local toml.
        if (!localEdit && AvalonLink.isAvalonLoaded()) {
            com.avalon.base.network.AvalonNetwork.sendToServer(NetworkChannels.UPDATE,
                    new AuthCmdUpdatePacket(mode, showPauseButton,
                            nonOpWhitelist, opBlacklist, nonOpExempt, opExempt));
        }
        AuthCmdConfig.mode = mode;
        AuthCmdConfig.showPauseButton = showPauseButton;
        AuthCmdConfig.nonOpWhitelist = new ArrayList<>(nonOpWhitelist);
        AuthCmdConfig.opBlacklist = new ArrayList<>(opBlacklist);
        AuthCmdConfig.nonOpExempt = new ArrayList<>(nonOpExempt);
        AuthCmdConfig.opExempt = new ArrayList<>(opExempt);
        if (localEdit) {
            AuthCmdConfig.save();
        }
    }

    // ─── 文案钩子（AuthCmd 自身语言键） ───

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