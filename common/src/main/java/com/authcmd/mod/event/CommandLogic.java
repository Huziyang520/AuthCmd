package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import com.authcmd.mod.util.ModMsg;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public class CommandLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

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

        boolean isOp = source.hasPermission(2);
        int domain = AuthCmdConfig.resolveDomain(isOp);
        if (domain == 0) return false;

        if (AuthCmdConfig.isExempt(domain, player.getName().getString())) return false;

        if (domain == 1) {
            if (AuthCmdConfig.isAllowed(domain, cmd)) {
                if (deniesEntitySelector(raw, player)) return true;
                executeElevated(player, raw);
                return true;
            }
            return false;
        }

        if ("gamemode".equals(cmd) || "g".equals(cmd)) {
            if (!AuthCmdConfig.isGamemodeAllowed(domain)) {
                player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked_gamemode"));
                return true;
            }
            return false;
        }

        if (AuthCmdConfig.shouldBlockCommand(domain, cmd)) {
            player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked"));
            return true;
        }
        return false;
    }

    private static boolean deniesEntitySelector(String raw, ServerPlayer player) {
        if (AuthCmdConfig.allowEntitySelectors()) return false;
        if (raw == null || raw.indexOf('@') < 0) return false;
        player.sendSystemMessage(ModMsg.red(player, "message.authcmd.selector_blocked"));
        return true;
    }

    private static void executeElevated(ServerPlayer player, String command) {

        String cmd = command == null ? "" : command.trim();
        while (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (cmd.isEmpty()) return;

        var elevated = player.createCommandSourceStack().withPermission(4);
        player.level().getServer().getCommands().performPrefixedCommand(elevated, cmd);
    }
}
