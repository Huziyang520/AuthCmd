package com.authcmd.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
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
        return new Button(x, y, size, size, new TextComponent(""),
                b -> onClick.run()) {
            @Override
            public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
                super.renderButton(pose, mouseX, mouseY, partialTick);
                int ox = this.x + (getWidth() - 16) / 2;
                int oy = this.y + (getHeight() - 16) / 2;
                RenderSystem.setShaderTexture(0, texture);
                GuiComponent.blit(pose, ox, oy, 0, 0, 16, 16, 16, 16);
            }
        };
    }
}
