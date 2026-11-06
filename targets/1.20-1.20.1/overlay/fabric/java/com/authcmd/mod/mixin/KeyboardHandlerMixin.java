package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric 客户端：功能一启用且 {@code gamemode} 命中白名单时，允许非OP打开 F3+F4 游戏模式切换器。
 *
 * <p>原版 {@code KeyboardHandler.handleDebugKeys} 仅在 {@code player.hasPermissions(2)}（OP）时
 * 打开 {@code GameModeSwitcherScreen}，非OP（非创造）直接被拦并提示「无权使用游戏模式切换器」。
 * 此处将 handleDebugKeys 中打开切换器分支那一次 {@code hasPermissions(2)} 判定改为：在
 * {@link GameModeLogic#shouldAllowGameModeChange} 放行时返回 true，从而允许非OP打开切换器；
 * 选好模式后的切换命令由 {@link GameModeSwitcherScreenMixin} 放行（1.20.1 切换器执行
 * {@code /gamemode} 命令而非发包），最终由 {@code CommandLogic} 提权 + {@code GameModeLogic.shouldBlock} 统一把关。
 */
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
