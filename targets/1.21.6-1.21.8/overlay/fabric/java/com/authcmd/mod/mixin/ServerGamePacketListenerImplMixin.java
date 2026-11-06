package com.authcmd.mod.mixin;

import com.authcmd.mod.event.GameModeLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric 服务端：放行功能一的非OP通过 F3+F4 切换器发送的 {@code ServerboundChangeGameModePacket}。
 *
 * <p>1.21.6 起 F3+F4 切换器不再执行 {@code /gamemode} 命令，而是直接发送
 * {@code ServerboundChangeGameModePacket}；服务端 {@link ServerGamePacketListenerImpl}
 * 在 {@code handleChangeGameMode} 中先检查 {@code player.hasPermissions(2)}，
 * 非OP直接被丢弃（仅后台 warn）。本 mixin 将本次权限检查改为：
 * {@link GameModeLogic#allowHasPermissions} 放行时返回 true，让非OP的包进入
 * {@code GameModeCommand.setGameMode}，由 {@code GameModeLogic.shouldBlock} 统一把关
 * （白名单命中→放行；黑名单/未命中→拦截+提示）。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Redirect(method = "handleChangeGameMode",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;hasPermissions(I)Z"))
    private boolean authcmd$allowGameModePacket(ServerPlayer player, int permissionLevel) {
        return GameModeLogic.allowHasPermissions(player, permissionLevel);
    }
}
