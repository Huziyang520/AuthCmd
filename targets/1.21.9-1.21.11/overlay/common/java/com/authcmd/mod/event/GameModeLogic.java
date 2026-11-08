package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.util.ModMsg;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

public class GameModeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean shouldBlock(ServerPlayer player, GameType oldGameMode) {
        boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        int domain = AuthCmdConfig.resolveDomain(isOp);
        LOGGER.info("[AuthCmd] shouldBlock player={} isOp={} domain={} oldMode={} whitelist={}",
                player.getName().getString(), isOp, domain, oldGameMode, AuthCmdConfig.nonOpWhitelist());
        if (domain == 0) return false;

        if (AuthCmdConfig.isExempt(domain, player.getName().getString())) return false;

        boolean allowed = AuthCmdConfig.isGamemodeAllowed(domain);
        LOGGER.info("[AuthCmd] shouldBlock gamemodeAllowed={}", allowed);
        if (!allowed) {
            player.displayClientMessage(ModMsg.red(player, "message.authcmd.blocked_gamemode"), false);
            return true;
        }
        return false;
    }

    public static boolean shouldAllowGameModeChange(Player player) {
        if (player == null) return false;
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return false;
        int domain = AuthCmdConfig.resolveDomain(false);
        if (domain != 1) return false;
        String name = player.getName().getString();
        if (AuthCmdConfig.isExempt(domain, name)) return true;
        return AuthCmdConfig.isGamemodeAllowed(domain);
    }

    public static boolean shouldAllowGameModeSwitcher(Player player) {
        return shouldAllowGameModeChange(player);
    }

    public static boolean allowHasPermissions(Player player, int permissionLevel) {
        if (shouldAllowGameModeSwitcher(player)) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
