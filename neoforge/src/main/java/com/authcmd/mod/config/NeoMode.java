package com.authcmd.mod.config;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/** NeoForge 界面用的模式枚举：取值与通用枚举同名，额外提供译文。 */
public enum NeoMode implements TranslatableEnum {
    disabled,
    non_op_only,
    op_only,
    both;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("gui.authcmd.mode." + name());
    }
}
