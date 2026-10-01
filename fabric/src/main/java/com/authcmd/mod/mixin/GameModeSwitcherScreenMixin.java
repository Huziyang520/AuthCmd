package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric 客户端：放行非OP在 F3+F4 切换器中选择模式（执行 {@code /gamemode} 命令）。
 *
 * <p>原版 {@code GameModeSwitcherScreen.switchToHoveredGameMode} 在执行切换命令前要求
 * {@code player.hasPermissions(2)}（OP），非OP即使已打开切换器界面，选好模式松开 F4 时
 * 命令也不会被发送（仅 OP 分支会 {@code sendUnsignedCommand("gamemode ...")}）。
 * 此处将本次 {@code hasPermissions(2)} 判定改为：在 {@link GameModeLogic#shouldAllowGameModeChange}
 * 放行时返回 true，从而让非OP真正发出切换命令；
 * 命令到服务端后由 {@code CommandLogic} 提权（gamemode 命中白名单）并交给
 * {@code GameModeLogic.shouldBlock} 统一把关。
 */
@Mixin(GameModeSwitcherScreen.class)
public class GameModeSwitcherScreenMixin {

    @Redirect(method = "switchToHoveredGameMode(Lnet/minecraft/client/Minecraft;Ljava/util/Optional;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasPermissions(I)Z"))
    private static boolean authcmd$allowSwitchGameMode(LocalPlayer player, int permissionLevel) {
        return GameModeLogic.allowHasPermissions(player, permissionLevel);
    }
}
