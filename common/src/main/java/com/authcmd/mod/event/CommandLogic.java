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
 * Command blocking core logic (platform-free).
 * <p>Two domains: operators map to the blacklist domain and non-operators to the whitelist one;
 * exempt players pass through, everything else is decided by the lists.
 */
public class CommandLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * /**
     * Forge entry: results are already parsed.
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

        // Exempt player passes through
        if (AuthCmdConfig.isExempt(domain, player.getName().getString())) return false;

        // Domain one (whitelist): elevation only, nothing is blocked.
        // Whitelisted commands run through a level-4 source while the original command is cancelled;
        // commands outside the list are left to vanilla, which keeps its own behaviour.
        if (domain == 1) {
            if (AuthCmdConfig.isAllowed(domain, cmd)) {
                if (deniesEntitySelector(raw, player)) return true;
                executeElevated(player, raw);
                return true;
            }
            return false;
        }

        // Domain two (blacklist): a blocked gamemode answers with the gamemode message.
        // Both aliases are judged together, matching the game mode switcher behaviour
        if ("gamemode".equals(cmd) || "g".equals(cmd)) {
            if (!AuthCmdConfig.isGamemodeAllowed(domain)) {
                player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked_gamemode"));
                return true;
            }
            return false;
        }

        // Domain two: a blacklist hit is blocked
        if (AuthCmdConfig.shouldBlockCommand(domain, cmd)) {
            player.sendSystemMessage(ModMsg.red(player, "message.authcmd.blocked"));
            return true;
        }
        return false;
    }

    /**
     * /**
     * Blocks elevated commands that use a target selector while the selector switch is off.
     */
    private static boolean deniesEntitySelector(String raw, ServerPlayer player) {
        if (AuthCmdConfig.allowEntitySelectors()) return false;
        if (raw == null || raw.indexOf('@') < 0) return false;
        player.sendSystemMessage(ModMsg.red(player, "message.authcmd.selector_blocked"));
        return true;
    }

    /**
     * /**
     * Runs the command through a level-4 source so non-operators can really use it.
     */
    private static void executeElevated(ServerPlayer player, String command) {
        // Normalizes the command string: drops extra leading slashes so parsing does not fail
        String cmd = command == null ? "" : command.trim();
        while (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (cmd.isEmpty()) return;

        var elevated = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        player.level().getServer().getCommands().performPrefixedCommand(elevated, cmd);
    }
}