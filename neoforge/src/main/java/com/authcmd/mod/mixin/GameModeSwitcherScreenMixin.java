package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NeoForge 客户端：放行非OP在 F3+F4 切换器中选择模式（发送 {@code ServerboundChangeGameModePacket}）。
 * <p>1.21.11 起原版权限判定重构，{@code LocalPlayer.hasPermissions(int)} 已被移除，
 * {@code GameModeSwitcherScreen.switchToHoveredGameMode} 不再发送 {@code /gamemode} 命令，
 */
@Mixin(GameModeSwitcherScreen.class)
public class GameModeSwitcherScreenMixin {

    @Redirect(method = "switchToHoveredGameMode(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/debug/GameModeSwitcherScreen$GameModeIcon;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/permissions/PermissionCheck;check(Lnet/minecraft/server/permissions/PermissionSet;)Z"))
    private static boolean authcmd$allowSwitchGameMode(PermissionCheck check, PermissionSet set) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && GameModeLogic.shouldAllowGameModeSwitcher(player)) return true;
        return check.check(set);
    }
}