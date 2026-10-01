package com.authcmd.mod.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 暂停界面右上角的 AuthCmd 设置按钮（无文字、仅图标）。
 *
 * <p>Fabric 与 Forge 的 {@code GuiEventHandler} 共用同一绘制逻辑（图标居中 blit），
 * 收敛为静态工厂避免各自复制匿名的 {@code Button} 子类。创建/挂载仍由各平台
 * 事件处理器完成（事件接入方式不同：Fabric 用 {@code ScreenEvents}，Forge 用
 * {@code ScreenEvent.Init.Post}）。
 */
public final class PauseScreenButton {

    private PauseScreenButton() {
    }

    /**
     * @param size    按钮边长（正方形）
     * @param texture 按钮图标贴图
     * @param onClick 点击回调
     */
    public static Button create(int x, int y, int size, ResourceLocation texture, Runnable onClick) {
        return new Button(x, y, size, size, Component.empty(),
                b -> onClick.run(), b -> Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                int ox = getX() + (getWidth() - 16) / 2;
                int oy = getY() + (getHeight() - 16) / 2;
                graphics.blit(texture, ox, oy, 0, 0, 16, 16, 16, 16);
            }
        };
    }
}
