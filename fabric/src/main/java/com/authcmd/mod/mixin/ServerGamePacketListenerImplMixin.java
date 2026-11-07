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
 * Fabric server: allows the game mode packet sent by whitelisted non-operators.
 * <p>Since 1.21.11 the F3+F4 switcher sends {@code ServerboundChangeGameModePacket} instead of
 * a {@code /gamemode} command; the server side of {@link ServerGamePacketListenerImpl}
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