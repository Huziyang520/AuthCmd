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
 * NeoForge 客户端：功能一启用且 {@code gamemode} 命中白名单时，允许非OP打开 F3+F4 游戏模式切换器。
 *
 * <p>1.21.11 起原版权限判定重构，{@code LocalPlayer.hasPermissions(int)} 已被移除，
 * {@code handleDebugKeys} 中打开切换器分支改为：
 * {@code if (... && GameModeCommand.PERMISSION_CHECK.check(player.permissions())) ... new GameModeSwitcherScreen()}
 * 此处将打开切换器分支那一次 {@code PERMISSION_CHECK.check(...)} 判定改为：在
 * {@link GameModeLogic#shouldAllowGameModeChange} 放行时返回 true，从而允许非OP打开切换器；
 * 选好模式后的切换包由 {@link GameModeSwitcherScreenMixin} 放行，服务端由
 * {@link ServerGamePacketListenerImplMixin} 放行 + {@code GameModeLogic.shouldBlock} 统一把关。
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Redirect(method = "handleDebugKeys",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/permissions/PermissionCheck;check(Lnet/minecraft/server/permissions/PermissionSet;)Z",
                    ordinal = 1))
    private static boolean authcmd$allowGameModeSwitcher(PermissionCheck check, PermissionSet set) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && GameModeLogic.shouldAllowGameModeChange(player)) return true;
        return check.check(set);
    }
}