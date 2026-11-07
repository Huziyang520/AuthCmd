package com.authcmd.mod.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PauseScreenButton {

    private PauseScreenButton() {
    }

    public static Button create(int x, int y, int size, Identifier texture, Runnable onClick) {
        return new Button(x, y, size, size, Component.empty(),
                b -> onClick.run(), b -> Component.empty()) {
            @Override
            public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                extractDefaultSprite(graphics);
                int ox = getX() + (getWidth() - 16) / 2;
                int oy = getY() + (getHeight() - 16) / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, ox, oy, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        };
    }
}