package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.util.ModMsg;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;

/**
 * 指令拦截核心业务逻辑（平台无关）。
 *
 * <p>双套权限体系：按玩家是否为 OP 解析到功能一（非OP白名单提权）或功能二（OP黑名单降权），
 * 命中豁免玩家则完全放行；否则按名单判定是否拦截。
 *
 * <p>入口：{@link #shouldCancel(ParseResults)}，Forge 的 CommandEvent 与 Fabric 的
 * CommandsMixin 均传入解析结果 {@code ParseResults} 使用。
 */
public class CommandLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Forge 侧入口：已有解析结果。
     */
    public static boolean shouldCancel(ParseResults<CommandSourceStack> parseResults) {
        CommandSourceStack source = parseResults.getContext().getSource();
        String raw = parseResults.getReader().getString();
        String cmd = resolveCommandName(parseResults, raw);
        boolean r = handle(source, raw, cmd);
        if ("gamemode".equals(cmd) || "g".equals(cmd)) {
            LOGGER.info("[AuthCmd] shouldCancel gamemode cmd='{}' raw='{}' entity={} -> {}",
                    cmd, raw, source.getEntity(), r);
        }
        return r;
    }

    private static String resolveCommandName(ParseResults<CommandSourceStack> parse, String raw) {
        if (!parse.getContext().getNodes().isEmpty()) {
            return parse.getContext().getNodes().get(0).getNode().getName();
        }
        if (raw == null || raw.isEmpty()) return "";
        String first = raw.split(" ")[0].toLowerCase();
        if (first.startsWith("/")) first = first.substring(1);
        return first;
    }

    private static boolean handle(CommandSourceStack source, String raw, String cmd) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return false;

        boolean isOp = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        int domain = AuthCmdConfig.resolveDomain(isOp);
        if (domain == 0) return false; // 该玩家不在任何受约束功能域

        // 豁免玩家 → 完全放行
        if (AuthCmdConfig.isExempt(domain, player.getName().getString())) return false;

        // 功能一（非OP白名单）：纯提权、不拦截。
        // 白名单内指令用权限4的 source 代其执行并取消原命令（否则原版执行仍要求权限2）；
        // 白名单外的指令不做任何干预，交还原版——非OP原本能用的照常用，原本不能用的由原版拦截。
        if (domain == 1) {
            if (AuthCmdConfig.isAllowed(domain, cmd)) {
                if (deniesEntitySelector(raw, player)) return true;
                executeElevated(player, raw);
                return true;
            }
            return false;
        }

        // 功能二（OP黑名单）：gamemode/g 被禁时，提示游戏模式切换被禁（而非通用的指令禁止提示）。
        // 两个别名整体判定，与游戏模式切换器（GameModeLogic）保持一致
        if ("gamemode".equals(cmd) || "g".equals(cmd)) {
            if (!AuthCmdConfig.isGamemodeAllowed(domain)) {
                player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked_gamemode"));
                return true;
            }
            return false;
        }

        // 功能二：命中黑名单 → 拦截
        if (AuthCmdConfig.shouldBlockCommand(domain, cmd)) {
            player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked"));
            return true;
        }
        return false;
    }

    /**
     * 目标选择器开关关闭时，拒绝**含选择器**的提权指令。
     *
     * <p>只关客户端是不够的：服务端是"用全权限源提权执行"，选择器天然不受限。因此在提权之前
     * 再拦一次，保证"关闭 = 真的不能用"。
     *
     * @return true 表示已拦截（并已提示玩家）
     */
    private static boolean deniesEntitySelector(String raw, ServerPlayer player) {
        if (AuthCmdConfig.allowEntitySelectors) return false;
        if (raw == null || raw.indexOf('@') < 0) return false;
        player.sendSystemMessage(ModMsg.red(player, "message.authcmd.selector_blocked"));
        return true;
    }

    /**
     * 用权限4的命令源代非OP执行命令，使其能真正使用白名单指令。
     * 参照 OnlyTP {@code CommandLogic.executeElevated} 的实现。
     */
    private static void executeElevated(ServerPlayer player, String command) {
        // 规范化命令字符串：去除多余前导 '/', 避免 performPrefixedCommand 重复加 '/' 导致解析失败
        String cmd = command == null ? "" : command.trim();
        while (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (cmd.isEmpty()) return;

        var elevated = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        player.level().getServer().getCommands().performPrefixedCommand(elevated, cmd);
    }
}