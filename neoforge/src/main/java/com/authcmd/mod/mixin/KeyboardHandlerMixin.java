package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Redirect(method = "handleDebugKeys",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasPermissions(I)Z",
                    ordinal = 2))
    private boolean authcmd$allowGameModeSwitcher(LocalPlayer player, int permissionLevel) {
        return GameModeLogic.allowHasPermissions(player, permissionLevel);
    }
}
