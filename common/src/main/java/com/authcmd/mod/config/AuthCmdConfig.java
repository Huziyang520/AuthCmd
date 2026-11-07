package com.authcmd.mod.config;

import com.camelot.config.CamelotConfig;
import com.camelot.config.ConfigOption;
import com.camelot.config.ConfigSchema;
import com.camelot.config.ConfigScope;
import com.camelot.net.CamelotNet;

import java.util.ArrayList;
import java.util.List;

public final class AuthCmdConfig {

    public enum Mode {
        disabled,
        non_op_only,
        op_only,
        both
    }

    public static final String MODE_DISABLED = "disabled";
    public static final String MODE_NON_OP = "non_op_only";
    public static final String MODE_OP = "op_only";
    public static final String MODE_BOTH = "both";

    public static final ConfigSchema COMMON_SCHEMA = new ConfigSchema("authcmd", ConfigScope.COMMON,
            "gui.authcmd.title",
            List.of(
                    ConfigOption.select("mode", Mode.disabled),
                    ConfigOption.bool("allow_entity_selectors", true),
                    ConfigOption.bool("enable_config_entry", true).legacy("show_pause_button"),
                    ConfigOption.list("non_op_whitelist", List.of()),
                    ConfigOption.list("op_blacklist", List.of()),
                    ConfigOption.list("non_op_exempt", List.of()),
                    ConfigOption.list("op_exempt", List.of())));

    public static final ConfigSchema CLIENT_SCHEMA = new ConfigSchema("authcmd", ConfigScope.CLIENT,
            "gui.authcmd.title",
            List.of(
                    ConfigOption.bool("enable_animations", true),
                    ConfigOption.switchInt("show_tips", 1)));

    public static final CamelotConfig CONFIG = new CamelotConfig(COMMON_SCHEMA);

    public static final CamelotConfig CLIENT = new CamelotConfig(CLIENT_SCHEMA);

    private AuthCmdConfig() {
    }

    public static CamelotConfig[] all() {
        return new CamelotConfig[]{CONFIG, CLIENT};
    }

    public static String mode() {
        return CONFIG.getString("mode");
    }

    public static boolean allowEntitySelectors() {
        return CONFIG.getBool("allow_entity_selectors");
    }

    public static boolean enableConfigEntry() {
        return CONFIG.getBool("enable_config_entry");
    }

    public static boolean enableAnimations() {
        return CLIENT.getBool("enable_animations");
    }

    public static List<String> nonOpWhitelist() {
        return CONFIG.getList("non_op_whitelist");
    }

    public static List<String> opBlacklist() {
        return CONFIG.getList("op_blacklist");
    }

    public static List<String> nonOpExempt() {
        return CONFIG.getList("non_op_exempt");
    }

    public static List<String> opExempt() {
        return CONFIG.getList("op_exempt");
    }

    public static void applyAndSave(String newMode, boolean selectors, boolean entry, boolean animations,
                                    List<String> whitelist, List<String> blacklist,
                                    List<String> nonOpExempts, List<String> opExempts) {
        CONFIG.set("mode", newMode);
        CONFIG.set("allow_entity_selectors", selectors);
        CONFIG.set("enable_config_entry", entry);
        CONFIG.set("non_op_whitelist", new ArrayList<>(whitelist));
        CONFIG.set("op_blacklist", new ArrayList<>(blacklist));
        CONFIG.set("non_op_exempt", new ArrayList<>(nonOpExempts));
        CONFIG.set("op_exempt", new ArrayList<>(opExempts));
        CONFIG.save();

        CLIENT.set("enable_animations", animations);
        CLIENT.save();
    }

    public static boolean isClientConfigSynced() {
        return CamelotNet.isSynced("authcmd");
    }

    public static int resolveDomain(boolean isOp) {
        String mode = mode();
        if (MODE_DISABLED.equals(mode)) return 0;
        if (MODE_BOTH.equals(mode)) return isOp ? 2 : 1;
        if (MODE_NON_OP.equals(mode)) return isOp ? 0 : 1;
        if (MODE_OP.equals(mode)) return isOp ? 2 : 0;
        return 0;
    }

    public static boolean isExempt(int domain, String playerName) {
        if (domain == 1) return nonOpExempt().contains(playerName);
        if (domain == 2) return opExempt().contains(playerName);
        return false;
    }

    public static boolean shouldBlockCommand(int domain, String commandName) {
        if (commandName == null) return false;
        String norm = normalizeCommand(commandName);
        if (domain == 1) {

            return !containsNormalized(nonOpWhitelist(), norm);
        }
        if (domain == 2) {

            return containsNormalized(opBlacklist(), norm);
        }
        return false;
    }

    private static boolean containsNormalized(List<String> list, String norm) {
        return containsNormalized(list, norm, false);
    }

    public static boolean containsNormalized(List<String> list, String norm, boolean gamemodeAliases) {
        if (list == null || list.isEmpty()) return false;
        for (String entry : list) {
            String e = normalizeCommand(entry);
            if (e.equals(norm)) return true;
            if (gamemodeAliases && isGamemodeCommand(e) && isGamemodeCommand(norm)) return true;
        }
        return false;
    }

    public static boolean isAllowed(int domain, String commandName) {
        return !shouldBlockCommand(domain, commandName);
    }

    public static boolean isGamemodeAllowed(int domain) {
        if (domain == 1) {
            for (String entry : nonOpWhitelist()) {
                if (isGamemodeCommand(normalizeCommand(entry))) return true;
            }
            return false;
        }
        if (domain == 2) {
            for (String entry : opBlacklist()) {
                if (isGamemodeCommand(normalizeCommand(entry))) return false;
            }
            return true;
        }
        return true;
    }

    public static boolean isGamemodeCommand(String normalized) {
        return "gamemode".equals(normalized) || "g".equals(normalized);
    }

    public static String normalizeCommand(String cmd) {
        if (cmd == null) return "";
        String c = cmd.trim();
        if (c.startsWith("/")) c = c.substring(1);
        int sp = c.indexOf(' ');
        if (sp > 0) c = c.substring(0, sp);
        return c.toLowerCase();
    }
}
