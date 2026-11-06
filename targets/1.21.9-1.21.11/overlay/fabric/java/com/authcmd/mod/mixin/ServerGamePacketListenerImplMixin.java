package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric 服务端：放行功能一的非OP通过 F3+F4 切换器发送的 {@code ServerboundChangeGameModePacket}。
 *
 * <p>1.21.11 起 F3+F4 切换器不再发送 {@code /gamemode} 命令，而是直接发送
 * {@code ServerboundChangeGameModePacket}；服务端 {@link ServerGamePacketListenerImpl}
 * 在 {@code handleChangeGameMode} 中先检查 {@code GameModeCommand.PERMISSION_CHECK.check()}，
 * 非OP直接被丢弃（仅后台日志）。本 mixin 将本次权限检查改为：在
 * {@link GameModeLogic#shouldAllowGameModeChange} 放行时返回 true，让非OP的包进入
 * {@code ServerPlayer.setGameMode}，由 {@code GameModeLogic.shouldBlock} 统一把关
 * （白名单命中→放行；黑名单/未命中→拦截+提示）。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Redirect(method = "handleChangeGameMode",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/permissions/PermissionCheck;check(Lnet/minecraft/server/permissions/PermissionSet;)Z"))
    private boolean authcmd$allowGameModePacket(PermissionCheck check, PermissionSet set) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        if (GameModeLogic.shouldAllowGameModeChange(player)) return true;
        return check.check(set);
    }
}