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
 *
 * <p>无独立开关：gamemode 拦截由指令限制自动带出。判断玩家所属功能域内
 * {@code gamemode}/{@code g} 是否应被拦截，是则回滚；豁免玩家不拦截。
 */
public class GameModeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 判断是否应拦截本次游戏模式切换。
     *
     * @param player   切换游戏模式的玩家
     * @param oldGameMode 当前（切换前）的游戏模式
     * @return true 表示应拦截（回滚到旧模式）
     */
    public static boolean shouldBlock(ServerPlayer player, GameType oldGameMode) {
        boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        int domain = AuthCmdConfig.resolveDomain(isOp);
        LOGGER.info("[AuthCmd] shouldBlock player={} isOp={} domain={} oldMode={} whitelist={}",
                player.getName().getString(), isOp, domain, oldGameMode, AuthCmdConfig.nonOpWhitelist);
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
     * 判定是否应放行非OP通过 F3+F4 游戏模式切换器切换游戏模式。
     *
     * <p>1.20.1 的切换器切换模式是通过执行 {@code /gamemode <mode>} 命令完成（非发包），
     * 客户端对非OP有两道 OP 权限卡点，本方法在两处均被复用，避免逻辑重复：
     * <ol>
     *   <li><b>打开界面</b>：{@code KeyboardHandler.handleDebugKeys} 仅当
     *       {@code player.hasPermissions(2)}（OP）时才打开 {@code GameModeSwitcherScreen}，
     *       非OP直接被提示「无权使用游戏模式切换器」。</li>
     *   <li><b>选择模式</b>：{@code GameModeSwitcherScreen.switchToHoveredGameMode} 在发送
     *       {@code /gamemode} 命令前同样要求 {@code hasPermissions(2)}，非OP即使打开了界面，
     *       选好模式松开 F4 时命令也不会发出。</li>
     * </ol>
     * 功能一启用且 {@code gamemode} 命中白名单时，本方法对非OP在两处都放行；
     * 命令到服务端后由 {@link CommandLogic} 提权执行，最终由 {@link #shouldBlock} 统一把关
     * （gamemode 命中白名单时不拦截）。
     *
     * @param player 本地玩家（客户端 {@code LocalPlayer}）
     * @return true 表示应绕过原版权限检查，放行切换器打开/切换命令
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
     * 客户端 F3+F4 切换器入口/发包的放行判定（<b>严格</b>，2026-10-07 口径修正）。
     *
     * <p>口径：<b>只有</b>「功能一（非OP白名单）生效 + 名单里写入了 {@code gamemode}/{@code g}
     * + 该玩家未被豁免」时才放行入口；否则一律返回 {@code false}，让原版走自己的拒绝分支——
     * {@code KeyboardHandler.handleDebugKeys} 会打出 {@code debug.gamemodes.error}
     *（中文即「[调试]：你没有权限打开游戏模式切换器」）并且<b>不打开</b>切换器界面，
     * 表现与未启用本模组时完全一致。
     *
     * <p><b>为什么不再宽松</b>：旧实现（2026-10-01）只判「功能一是否生效」，理由是想绕开
     * 「未装 AvalonBase 的客户端收不到 SYNC ⇒ 本地名单恒空 ⇒ 界面打不开」的不一致；代价是
     * <b>名单里没写 {@code gamemode} 时非OP 依然能看到切换器</b>，与「名单决定是否可用」的语义相反。
     * 现在改为严格判定：客户端名单为空（含尚未收到 SYNC 的联机首帧）⇒ 打不开，需等服务端
     * 下发配置；真正的把关仍由服务端 {@link #shouldBlock} / {@link #shouldAllowGameModeChange} 承担，
     * 客户端放行不会造成越权（服务端未放行时切换仍会被回滚）。
     *
     * @param player 客户端本地玩家（{@code LocalPlayer}，以 {@link Player} 形参传入以避免 common 侧引用客户端类）
     * @return true 表示应绕过原版权限检查，放行切换器打开/切换
     */
    public static boolean shouldAllowGameModeSwitcher(Player player) {
        return shouldAllowGameModeChange(player);
    }

    /**
     * 客户端 F3+F4 切换器两处 {@code LocalPlayer.hasPermissions(int)} 的通用放行判定。
     *
     * <p>1.20.1 等旧版本的切换器走 {@code hasPermissions(int)}；26.3 已改为直接判定
     * {@code PermissionCheck}，由 {@link #shouldAllowGameModeSwitcher} 承担。
     *
     * @param player          客户端本地玩家
     * @param permissionLevel 原版权限检查要求的权限等级
     * @return true 表示放行（绕过原版权限判定）
     */
    public static boolean allowHasPermissions(Player player, int permissionLevel) {
        if (shouldAllowGameModeSwitcher(player)) return true;
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}