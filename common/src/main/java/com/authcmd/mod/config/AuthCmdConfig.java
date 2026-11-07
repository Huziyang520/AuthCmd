package com.authcmd.mod.config;

import com.camelot.config.CamelotConfig;
import com.camelot.config.ConfigOption;
import com.camelot.config.ConfigSchema;
import com.camelot.config.ConfigScope;
import com.camelot.net.CamelotNet;

import java.util.ArrayList;
import java.util.List;

/**
 * AuthCmd 指令权限配置：判定项在 {@code authcmd-common.toml}，本机偏好项在 {@code authcmd-client.toml}。
 *
 * <p>双套权限体系：
 * <ul>
 *   <li><b>功能一（非OP）</b>：提权白名单 {@code non_op_whitelist}，名单内指令额外对非OP放行；
 *       豁免玩家 {@code non_op_exempt} 完全豁免。</li>
 *   <li><b>功能二（OP）</b>：降权黑名单 {@code op_blacklist}，名单内指令对OP拦截；
 *       豁免玩家 {@code op_exempt} 完全豁免。</li>
 * </ul>
 *
 * <p>生效范围 {@code mode}：disabled（全关闭，默认）/ non_op_only / op_only / both。
 */
public final class AuthCmdConfig {

    public static final String MODE_DISABLED = "disabled";
    public static final String MODE_NON_OP = "non_op_only";
    public static final String MODE_OP = "op_only";
    public static final String MODE_BOTH = "both";

    public static final ConfigSchema COMMON_SCHEMA = new ConfigSchema("authcmd", ConfigScope.COMMON,
            "gui.authcmd.title",
            List.of(
                    ConfigOption.select("mode", MODE_DISABLED,
                                    MODE_DISABLED, MODE_NON_OP, MODE_OP, MODE_BOTH)
                            .comment("mode: \"disabled\"  \"non_op_only\"  \"op_only\"  or  \"both\""),
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

    /** 服务端下发、全局生效的配置。 */
    public static final CamelotConfig CONFIG = new CamelotConfig(COMMON_SCHEMA);

    /** 只影响本机的配置，不参与同步。 */
    public static final CamelotConfig CLIENT = new CamelotConfig(CLIENT_SCHEMA);

    private AuthCmdConfig() {
    }

    public static CamelotConfig[] all() {
        return new CamelotConfig[]{CONFIG, CLIENT};
    }

    // ─── 取值 ───

    public static String mode() {
        return CONFIG.getString("mode");
    }

    public static boolean allowEntitySelectors() {
        return CONFIG.getBool("allow_entity_selectors");
    }

    /** 配置入口开关：关闭后暂停界面无按钮，世界内进入入口显示"服务端已关闭"。 */
    public static boolean enableConfigEntry() {
        return CONFIG.getBool("enable_config_entry");
    }

    public static boolean enableAnimations() {
        return CLIENT.getBool("enable_animations");
    }

    /** 功能一：非OP指令白名单（提权）。 */
    public static List<String> nonOpWhitelist() {
        return CONFIG.getList("non_op_whitelist");
    }

    /** 功能二：OP指令黑名单（降权）。 */
    public static List<String> opBlacklist() {
        return CONFIG.getList("op_blacklist");
    }

    /** 功能一：非OP豁免玩家。 */
    public static List<String> nonOpExempt() {
        return CONFIG.getList("non_op_exempt");
    }

    /** 功能二：OP豁免玩家。 */
    public static List<String> opExempt() {
        return CONFIG.getList("op_exempt");
    }

    /** 界面保存后写回：立即生效并落盘（联机客户端随后由服务端下发覆盖）。 */
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

    /** 客户端是否已收到过服务端下发的配置；未收到时不应按本地默认值做否定判定。 */
    public static boolean isClientConfigSynced() {
        return CamelotNet.isSynced("authcmd");
    }

    // ─── 判定方法 ───

    /**
     * 判断某玩家在本次命令执行中应走哪个功能域。
     *
     * @return 0=不约束；1=功能一（非OP白名单）；2=功能二（OP黑名单）
     */
    public static int resolveDomain(boolean isOp) {
        String mode = mode();
        if (MODE_DISABLED.equals(mode)) return 0;
        if (MODE_BOTH.equals(mode)) return isOp ? 2 : 1;
        if (MODE_NON_OP.equals(mode)) return isOp ? 0 : 1;
        if (MODE_OP.equals(mode)) return isOp ? 2 : 0;
        return 0;
    }

    /**
     * 判断某玩家是否在其所属功能域内被完全豁免。
     *
     * @param domain 由 {@link #resolveDomain(boolean)} 得出：1=功能一，2=功能二
     */
    public static boolean isExempt(int domain, String playerName) {
        if (domain == 1) return nonOpExempt().contains(playerName);
        if (domain == 2) return opExempt().contains(playerName);
        return false;
    }

    /**
     * 判断指定命令名在指定功能域内是否应被拦截。
     *
     * @param domain 由 {@link #resolveDomain(boolean)} 得出：1=功能一，2=功能二
     */
    public static boolean shouldBlockCommand(int domain, String commandName) {
        if (commandName == null) return false;
        String norm = normalizeCommand(commandName);
        if (domain == 1) {
            // 功能一（非OP，白名单）：未命中白名单 → 拦截（规范化匹配，与命令树补全放行逻辑一致）
            return !containsNormalized(nonOpWhitelist(), norm);
        }
        if (domain == 2) {
            // 功能二（OP，黑名单）：命中黑名单 → 拦截（规范化匹配）
            return containsNormalized(opBlacklist(), norm);
        }
        return false;
    }

    /** 判断列表是否存在规范化后与目标命令名相等的条目（去 /、小写、取第一段）。 */
    private static boolean containsNormalized(List<String> list, String norm) {
        return containsNormalized(list, norm, false);
    }

    /**
     * 判断列表是否存在规范化后与目标命令名相等的条目。
     *
     * @param gamemodeAliases 为 true 时，{@code gamemode}/{@code g} 视为同一命令的两个别名整体匹配
     */
    public static boolean containsNormalized(List<String> list, String norm, boolean gamemodeAliases) {
        if (list == null || list.isEmpty()) return false;
        for (String entry : list) {
            String e = normalizeCommand(entry);
            if (e.equals(norm)) return true;
            if (gamemodeAliases && isGamemodeCommand(e) && isGamemodeCommand(norm)) return true;
        }
        return false;
    }

    /**
     * 判断指定命令名在指定功能域内是否被允许（放行）。
     *
     * @param domain 由 {@link #resolveDomain(boolean)} 得出：1=功能一，2=功能二
     */
    public static boolean isAllowed(int domain, String commandName) {
        return !shouldBlockCommand(domain, commandName);
    }

    /**
     * 判断 gamemode/g（同一命令的两个别名）在指定功能域内是否被整体放行。
     * <p>功能一（白名单）：两个别名任一在白名单即放行；
     * 功能二（黑名单）：两个别名任一在黑名单即拦截。
     */
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

    /** 规范化后的命令名是否为 {@code gamemode} 或其别名 {@code g}。 */
    public static boolean isGamemodeCommand(String normalized) {
        return "gamemode".equals(normalized) || "g".equals(normalized);
    }

    /** 规范化命令名：去前导 /、统一小写、只取空格前第一段。 */
    public static String normalizeCommand(String cmd) {
        if (cmd == null) return "";
        String c = cmd.trim();
        if (c.startsWith("/")) c = c.substring(1);
        int sp = c.indexOf(' ');
        if (sp > 0) c = c.substring(0, sp);
        return c.toLowerCase();
    }
}
