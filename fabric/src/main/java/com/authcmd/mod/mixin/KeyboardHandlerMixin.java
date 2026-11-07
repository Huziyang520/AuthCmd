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

/**
 * Fabric client: allows non-operators to open the F3+F4 switcher when the whitelist domain is active.
 * <p>1.21.11 reworked the vanilla permission check and removed {@code LocalPlayer.hasPermissions(int)};
 * {@code handleDebugKeys} opens the switcher through this decision:
 */
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