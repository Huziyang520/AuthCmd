package com.authcmd.mod.config;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/** Mode enum for the NeoForge screen: same constants as the shared enum, plus translated names. */
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
