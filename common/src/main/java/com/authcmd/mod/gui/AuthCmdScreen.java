package com.authcmd.mod.gui;

import com.authcmd.mod.AvalonLink;
import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.network.AuthCmdUpdatePacket;
import com.authcmd.mod.network.NetworkChannels;
import com.avalon.base.gui.anim.ScreenAnimType;
import com.avalon.base.gui.anim.ScrollAnim;
import com.avalon.base.gui.panel.PanelHover;
import com.avalon.base.gui.screen.AvalonConfigScreen;
import com.avalon.base.gui.util.MouseButtons;
import com.avalon.base.gui.util.TextFit;
import com.avalon.base.gui.theme.GuiTheme;
import com.avalon.base.gui.theme.ModernTheme;
import com.avalon.base.gui.theme.ThemedButton;
import com.avalon.base.gui.theme.ThemedRadio;
import com.avalon.base.gui.theme.ThemedToggle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
    private ThemedToggle enableAnimationsToggle;
    private ThemedToggle allowSelectorsToggle;
    /** 「启用动画效果」的本地编辑值（保存时随配置下发；旧配置缺键 → 默认开启）。 */
    private boolean enableAnimations;
    /** 「允许目标选择器」的本地编辑值（保存时随配置下发；默认开启）。 */
    private boolean allowEntitySelectors;
    private ThemedButton addCommandButton; // 指令名单「新增」按钮
    private ThemedButton addExemptButton;  // 豁免名单「新增」按钮
    private int hoveredCommandRow = -1;
    private int hoveredExemptRow = -1;
    private int exemptScroll;
    /** 两张名单卡片的平滑滚动：{@link #blScroll}/{@link #exemptScroll} 仍是“目标值”。 */
    private final ScrollAnim blAnim = new ScrollAnim(0);
    private final ScrollAnim exemptAnim = new ScrollAnim(0);
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
        this.enableAnimations = AuthCmdConfig.enableAnimations;
        this.allowEntitySelectors = AuthCmdConfig.allowEntitySelectors;
        this.nonOpWhitelist.addAll(AuthCmdConfig.nonOpWhitelist);
        this.opBlacklist.addAll(AuthCmdConfig.opBlacklist);
        this.nonOpExempt.addAll(AuthCmdConfig.nonOpExempt);
        this.opExempt.addAll(AuthCmdConfig.opExempt);
        // 开/关屏动画（AvalonBase 动画 API）：本屏选用「缩放弹跳」；开关关闭时完全无动画。
        // 在构造函数里配置 → 只在“打开本屏”时播放一次；模式切换导致的 init() 重入不会重播。
        configureAnimations(enableAnimations, ScreenAnimType.SCALE_BOUNCE, ScreenAnimType.SCALE_BOUNCE);
        playOpenAnimation();
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
        // 「设置」卡片第 2 行：界面动画开关（默认启用）
        enableAnimationsToggle = new ThemedToggle(guiLeft + LIST_RIGHT_X + 6, 42,
                Component.translatable("gui.authcmd.enable_animations"), enableAnimations, canEdit, false, LABEL_MAX_W);
        // 「设置」卡片第 3 行：目标选择器开关（默认启用）
        allowSelectorsToggle = new ThemedToggle(guiLeft + LIST_RIGHT_X + 6, 54,
                Component.translatable("gui.authcmd.allow_entity_selectors"), allowEntitySelectors, canEdit, false, LABEL_MAX_W);

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 开/关屏动画：命中测试用动画坐标系（自绘控件与自绘文字才能跟着位移/缩放对齐）
        mouseX = (int) animation().localX(mouseX, width);
        mouseY = (int) animation().localY(mouseY, height);
        // 背景暗化层由原版背景通道（renderBackground）负责，且天然在动画变换之外
        beginAnimatedRender(graphics);

        int panelH = yo(CONTENT_MAX_Y) + 14 - (yo(CONTENT_MIN_Y) - 4);
        theme.drawPanel(graphics, guiLeft, yo(CONTENT_MIN_Y) - 4, GUI_WIDTH, panelH);

        // ─── 生效范围卡片（左半，70px 高，与下方左卡同尺寸对齐） ───
        int cardLeftX = LIST_LEFT_X, cardLeftW = LIST_CARD_W;
        theme.drawCard(graphics, guiLeft + cardLeftX, yo(14), cardLeftW, 70);
        graphics.drawString(font, Component.translatable("gui.authcmd.mode_label"),
                guiLeft + cardLeftX + 6, yo(18), theme.labelColor(), false);
        for (ThemedRadio r : modeRadios) r.render(graphics, font, theme, yo(r.relY), mouseX, mouseY);

        // ─── 开关卡片（右半，与下方右卡同尺寸对齐） ───
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

        // ─── 底部状态（与保存/取消同一行高度，避免贴到面板最下缘） ───
        renderStatus(graphics, 200);

        super.render(graphics, mouseX, mouseY, partialTick);

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
            if (!showHand && (showPauseToggle.isClicked(mouseX, mouseY, font, yo(showPauseToggle.relY))
                    || enableAnimationsToggle.isClicked(mouseX, mouseY, font, yo(enableAnimationsToggle.relY))
                    || allowSelectorsToggle.isClicked(mouseX, mouseY, font, yo(allowSelectorsToggle.relY)))) {
                showHand = true;
            }
            if (!showHand && (hoveredCommandRow >= 0 || hoveredExemptRow >= 0)) showHand = true;
        }
        updateCursor(graphics, showHand);

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
        if (tip == null) {
            tip = enableAnimationsToggle.truncatedTooltip(font, mouseX, mouseY, yo(enableAnimationsToggle.relY));
        }
        if (tip == null) {
            tip = allowSelectorsToggle.truncatedTooltip(font, mouseX, mouseY, yo(allowSelectorsToggle.relY));
        }
        if (tip != null) {
            PanelHover.render(graphics, font, tip, mouseX, mouseY, this.width, this.height);
        }

        // Red row-action hint ("click to remove") in the gold hover box.
        if (rowRemoveHint != null) {
            PanelHover.render(graphics, font, rowRemoveHint, mouseX, mouseY, this.width, this.height, 0xFFFF5555);
        }

        endAnimatedRender(graphics);
    }

    /** 渲染一个可整体滚动的名单卡片（左右并排，cardX 决定左右半区）。 */
    private void renderList(GuiGraphics graphics, int mouseX, int mouseY, Component label,
                            List<String> list, int cardX, int cardY, boolean isCommand) {
        int cx = guiLeft + cardX;
        theme.drawCard(graphics, cx, yo(cardY), LIST_CARD_W, LIST_CARD_H);
        graphics.drawString(font, label, cx + 8, yo(cardY + 4), theme.labelColor(), false);

        int listStartY = cardY + 22;
        int scroll = isCommand ? blAnim.displayValue() : exemptAnim.displayValue();
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
                // graphics.drawString(font, hint, cx + LIST_CARD_W - 8 - font.width(hint), y, 0xFFFF5555, false);
                // ─────────────────────────────────────────────────────────────
            }
            String text = TextFit.fit(font, list.get(idx), rowTextW);
            graphics.drawString(font, text, cx + 8, y, canEdit ? theme.textColor() : theme.disabledColor(), false);
        }

        // 滚动条（卡内右侧）
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

    // ═══════════ 交互 ═══════════

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseButtons.isLeft(button) && canEdit) {
            // 模式单选
            for (int i = 0; i < 4; i++) {
                if (modeRadios[i].isClicked(mouseX, mouseY, font, yo(modeRadios[i].relY))) {
                    if (!MODES[i].equals(mode)) {
                        playClickSound();
                        mode = MODES[i];
                        blScroll = 0;
                        exemptScroll = 0;
                        blAnim.snapTo(0); // 换模式＝换两套名单，直接归零不滑
                        exemptAnim.snapTo(0);
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
            // 界面动画开关：改动立即生效（重新装配动画器，但不重播开屏动画）
            if (enableAnimationsToggle.isClicked(mouseX, mouseY, font, yo(enableAnimationsToggle.relY))) {
                playClickSound();
                enableAnimations = !enableAnimations;
                enableAnimationsToggle.setChecked(enableAnimations);
                configureAnimations(enableAnimations, ScreenAnimType.SCALE_BOUNCE, ScreenAnimType.SCALE_BOUNCE);
                return true;
            }
            // 目标选择器开关：改完保存即生效（客户端提权感知 + 服务端拦截，两侧同一开关）
            if (allowSelectorsToggle.isClicked(mouseX, mouseY, font, yo(allowSelectorsToggle.relY))) {
                playClickSound();
                allowEntitySelectors = !allowEntitySelectors;
                allowSelectorsToggle.setChecked(allowEntitySelectors);
                return true;
            }
            // 指令名单删除
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
            // 豁免名单删除
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
        // 列表行区域（卡片内，标题下方到按钮上方）
        int listTop = yo(LIST_CARD_Y + 22) - 6;
        int listBot = yo(LIST_CARD_Y + 22 + LIST_VISIBLE_ROWS * LIST_ITEM_H) - 8;
        if (mouseY < listTop || mouseY >= listBot) return false;
        int dir = -(int) Math.signum(scrollY);

        // 左栏：指令名单
        int lx = guiLeft + LIST_LEFT_X;
        if (mouseX >= lx && mouseX <= lx + LIST_CARD_W && currentCommandList().size() > LIST_VISIBLE_ROWS) {
            blScroll = Mth.clamp(blScroll + dir, 0, currentCommandList().size() - LIST_VISIBLE_ROWS);
            blAnim.setTarget(blScroll); // 滚轮只改目标，ScrollAnim 负责滑过去
            return true;
        }
        // 右栏：豁免名单
        int rx = guiLeft + LIST_RIGHT_X;
        if (mouseX >= rx && mouseX <= rx + LIST_CARD_W && currentExemptList().size() > LIST_VISIBLE_ROWS) {
            exemptScroll = Mth.clamp(exemptScroll + dir, 0, currentExemptList().size() - LIST_VISIBLE_ROWS);
            exemptAnim.setTarget(exemptScroll);
            return true;
        }
        return false;
    }

    // ═══════════ 辅助方法 ═══════════

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

    /** 打开二级输入弹窗，向指定名单（true=指令 / false=豁免）连续添加。 */
    private void openAddDialog(boolean command) {
        if (minecraft == null) return;
        playClickSound();
        List<String> target = command ? currentCommandList() : currentExemptList();
        minecraft.setScreen(new AuthCmdAddDialog(this, command, target));
    }

    @Override
    protected void saveConfig() {
        if (!canEdit) return;
        // Local edits (opened from the main-menu mod list) only persist to the local toml.
        if (!localEdit && AvalonLink.isAvalonLoaded()) {
            com.avalon.base.network.AvalonNetwork.sendToServer(NetworkChannels.UPDATE,
                    new AuthCmdUpdatePacket(mode, allowEntitySelectors, showPauseButton, enableAnimations,
                            nonOpWhitelist, opBlacklist, nonOpExempt, opExempt));
        }
        AuthCmdConfig.mode = mode;
        AuthCmdConfig.allowEntitySelectors = allowEntitySelectors;
        AuthCmdConfig.showPauseButton = showPauseButton;
        AuthCmdConfig.enableAnimations = enableAnimations;
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
