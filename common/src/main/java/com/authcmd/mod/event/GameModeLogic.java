package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.util.ModMsg;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

/**
 * Game mode switching rules (platform-free).
 * <p>No separate switch: gamemode blocking follows the command rules. Decides whether
 * {@code gamemode}/{@code g} is blocked in the player's domain, rolling the change back; exempt players pass.
 */
public class GameModeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * /**
     * Whether this game mode change should be blocked.
     */
    public static boolean shouldBlock(ServerPlayer player, GameType oldGameMode) {
        boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        int domain = AuthCmdConfig.resolveDomain(isOp);
        LOGGER.info("[AuthCmd] shouldBlock player={} isOp={} domain={} oldMode={} whitelist={}",
                player.getName().getString(), isOp, domain, oldGameMode, AuthCmdConfig.nonOpWhitelist());
        if (domain == 0) return false; // 不在受约束功能域

        // Exempt player passes through
        if (AuthCmdConfig.isExempt(domain, player.getName().getString())) return false;

        // gamemode/g are judged as one command with two aliases:
        // whitelist: any alias listed is allowed; blacklist: any alias listed is blocked
        boolean allowed = AuthCmdConfig.isGamemodeAllowed(domain);
        LOGGER.info("[AuthCmd] shouldBlock gamemodeAllowed={}", allowed);
        if (!allowed) {
            player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked_gamemode"));
            return true;
        }
        return false;
    }

    /**
     * /**
     * Whether a non-operator may use the F3+F4 game mode switcher.
     */
    public static boolean shouldAllowGameModeChange(Player player) {
        if (player == null) return false;
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return false; // OP 走原版逻辑即可
        int domain = AuthCmdConfig.resolveDomain(false);
        if (domain != 1) return false; // 仅功能一（非OP白名单）
        String name = player.getName().getString();
        if (AuthCmdConfig.isExempt(domain, name)) return true; // 豁免玩家完全放行
        return AuthCmdConfig.isGamemodeAllowed(domain); // gamemode 命中白名单才放行
    }

    /**
     * /**
     * Client F3+F4 switcher decision (<b>strict</b>).
     */
    public static boolean shouldAllowGameModeSwitcher(Player player) {
        return shouldAllowGameModeChange(player);
    }

    /**
     * /**
     * Shared decision for the two {@code LocalPlayer.hasPermissions(int)} checks of the F3+F4 switcher.
     */
    public static boolean allowHasPermissions(Player player, int permissionLevel) {
        if (shouldAllowGameModeSwitcher(player)) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}