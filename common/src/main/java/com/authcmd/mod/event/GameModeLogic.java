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
 * 游戏模式切换拦截业务逻辑（平台无关）。
 * <p>无独立开关：gamemode 拦截由指令限制自动带出。判断玩家所属功能域内
 * {@code gamemode}/{@code g} 是否应被拦截，是则回滚；豁免玩家不拦截。
 */
public class GameModeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * /**
     * 判断是否应拦截本次游戏模式切换。
     */
    public static boolean shouldBlock(ServerPlayer player, GameType oldGameMode) {
        boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        int domain = AuthCmdConfig.resolveDomain(isOp);
        LOGGER.info("[AuthCmd] shouldBlock player={} isOp={} domain={} oldMode={} whitelist={}",
                player.getName().getString(), isOp, domain, oldGameMode, AuthCmdConfig.nonOpWhitelist());
        if (domain == 0) return false; // 不在受约束功能域

        // 豁免玩家 → 完全放行
        if (AuthCmdConfig.isExempt(domain, player.getName().getString())) return false;

        // gamemode/g 作为同一命令的两个别名整体判定：
        // 功能一（白名单）：任一别名在白名单即放行；功能二（黑名单）：任一别名在黑名单即拦截
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
     * 判定是否应放行非OP通过 F3+F4 游戏模式切换器切换游戏模式。
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
     * 客户端 F3+F4 切换器入口/发包的放行判定（<b>严格</b>，2026-10-07 口径修正）。
     */
    public static boolean shouldAllowGameModeSwitcher(Player player) {
        return shouldAllowGameModeChange(player);
    }

    /**
     * /**
     * 客户端 F3+F4 切换器两处 {@code LocalPlayer.hasPermissions(int)} 的通用放行判定。
     */
    public static boolean allowHasPermissions(Player player, int permissionLevel) {
        if (shouldAllowGameModeSwitcher(player)) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}