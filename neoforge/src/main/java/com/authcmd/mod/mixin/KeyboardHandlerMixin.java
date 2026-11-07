package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Redirect(method = "handleDebugKeys",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/permissions/PermissionCheck;check(Lnet/minecraft/server/permissions/PermissionSet;)Z",
                    ordinal = 1))
    private static boolean authcmd$allowGameModeSwitcher(PermissionCheck check, PermissionSet set) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && GameModeLogic.shouldAllowGameModeSwitcher(player)) return true;
        return check.check(set);
    }
}