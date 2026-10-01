package com.authcmd.mod.config;

import com.authcmd.mod.Constants;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AuthCmd 指令权限控制器配置（config/authcmd.toml）。
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
 * 游戏模式切换无独立开关，由 gamemode 指令的限制自动带出。
 *
 * <p>配置读写使用原生 {@code com.electronwill.nightconfig}（与 OnlyTPConfig 一致），
 * 不再依赖 AvalonBase 的 {@code AvalonToml}，使 AuthCmd 可在未安装 AvalonBase 的纯服务端独立运行。
 */
public class AuthCmdConfig {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Path CONFIG_PATH = Paths.get("config", "authcmd.toml");

       // 记录配置文件最后修改时间，用于热加载判定（等价于原 AvalonToml.isChanged 语义）
    private static volatile long lastModified = -1L;

    /** 热加载最小检查间隔（毫秒）。reloadIfChanged 可能在 sendCommands 中被逐节点高频调用，
     *  不节流会导致每次玩家登录产生数千次文件 stat。 */
    private static final long RELOAD_CHECK_INTERVAL_MS = 1000L;

    /** 上次真正执行文件 stat 的时刻。 */
    private static volatile long lastCheckTime = 0L;


    public static final String MODE_DISABLED = "disabled";
    public static final String MODE_NON_OP = "non_op_only";
    public static final String MODE_OP = "op_only";
    public static final String MODE_BOTH = "both";

    /** 生效范围：disabled / non_op_only / op_only / both。 */
    public static String mode = MODE_DISABLED;

    /** 是否显示暂停页面可视化编辑按钮。 */
    public static boolean showPauseButton = true;

    /** 客户端进入世界时若未装 AvalonBase，是否显示「安装AvalonBase启用可视化编辑」提示。0=关闭, 1=开启(默认)。 */
    public static int showTips = 1;

    /** 功能一：非OP指令白名单（提权，默认为空）。 */
    public static List<String> nonOpWhitelist = new ArrayList<>();

    /** 功能二：OP指令黑名单（降权，默认为空）。 */
    public static List<String> opBlacklist = new ArrayList<>();

    /** 功能一：非OP豁免玩家（完全豁免，不受非OP白名单限制）。 */
    public static List<String> nonOpExempt = new ArrayList<>();

    /** 功能二：OP豁免玩家（完全豁免，不受OP黑名单限制）。 */
    public static List<String> opExempt = new ArrayList<>();

    /** 是否已初始化（用于热加载判定）。 */
    private static volatile boolean initialized = false;

    /** 热加载确认配置文件变更后，由平台入口注入的回调：向在线玩家重发命令树（config 层无服务端引用，用回调解耦）。 */
    private static volatile Runnable commandTreeResender;

    private AuthCmdConfig() {
    }

    /**
     * 服务端启动时初始化配置：不存在则写默认，存在则读取。
     */
    public static void init() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH) && Files.size(CONFIG_PATH) > 0) {
                read();
            } else {
                resetDefaults();
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Fatal config error", e);
            resetDefaults();
        }
        initialized = true;
        LOGGER.info("[AuthCmd] Config loaded - mode: {}, showPauseButton: {}", mode, showPauseButton);
    }

    private static Config buildConfig() {
        Config cfg = Config.inMemory();
        cfg.set("mode", mode);
        cfg.set("show_pause_button", showPauseButton);
        cfg.set("show_tips", showTips);
        setSubList(cfg, "non_op_whitelist", nonOpWhitelist);
        setSubList(cfg, "op_blacklist", opBlacklist);
        setSubList(cfg, "non_op_exempt", nonOpExempt);
        setSubList(cfg, "op_exempt", opExempt);
        return cfg;
    }

    /**
     * 读取配置。
     */
    public static void read() {
        try {
            String text = Files.readString(CONFIG_PATH);
            Config cfg = new TomlParser().parse(text);
            mode = cfg.getOrElse("mode", MODE_DISABLED);
            showPauseButton = cfg.getOrElse("show_pause_button", true);
            showTips = cfg.getOrElse("show_tips", 1);
            nonOpWhitelist = new ArrayList<>(getSubList(cfg, "non_op_whitelist"));
            opBlacklist = new ArrayList<>(getSubList(cfg, "op_blacklist"));
            nonOpExempt = new ArrayList<>(getSubList(cfg, "non_op_exempt"));
            opExempt = new ArrayList<>(getSubList(cfg, "op_exempt"));
            lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        } catch (Exception e) {
            LOGGER.error("Failed to read config, using defaults", e);
            resetDefaults();
        }
    }

    /**
     * 写盘。由服务端收到客户端更新包后调用，或首次初始化写默认时调用。
     */
    public static void save() {
        try {
            String toml = new TomlWriter().writeToString(buildConfig());
            // 为 mode 注入示例注释（TOML 解析时会自动忽略注释，安全）
            toml = insertModeComment(toml);
            Files.writeString(CONFIG_PATH, toml);
            lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            LOGGER.info("[AuthCmd] Config saved - mode: {}, showPauseButton: {}", mode, showPauseButton);
        } catch (Exception e) {
            LOGGER.error("Failed to write config", e);
        }
    }

    /**
     * 在 TOML 字符串的 {@code mode = "..."} 行前插入示例注释，说明可选值。
     * 仅在 save() 写盘时调用；read() 使用 TomlParser 会忽略注释，不影响解析。
     */
    private static String insertModeComment(String toml) {
        String marker = "mode = \"";
        int idx = toml.indexOf(marker);
        if (idx < 0) return toml; // 未找到则原样返回
        String comment = "# mode: \"disabled\"  \"non_op_only\"  \"op_only\"  or  \"both\"\n";
        return toml.substring(0, idx) + comment + toml.substring(idx);
    }

     /**
     * 热加载：若配置文件被外部修改，则重新读取并刷新内存字段。
     * 确认变更后触发命令树重发回调，使已在线玩家的补全树反映最新白名单。
     *
     * <p>最多每 {@link #RELOAD_CHECK_INTERVAL_MS} 毫秒做一次文件 stat：本方法会被命令树
     * 动态谓词高频调用，无节流会造成大量无谓的磁盘 IO。
     */
    public static void reloadIfChanged() {
        if (!initialized) return;

        long now = System.currentTimeMillis();
        if (now - lastCheckTime < RELOAD_CHECK_INTERVAL_MS) return;
        lastCheckTime = now;

        try {
            long modified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            if (modified == lastModified) return;
            read();
            LOGGER.info("[AuthCmd] Config hot-reloaded");
            if (commandTreeResender != null) {
                commandTreeResender.run();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to hot-reload config", e);
        }
    }


    /**
     * 注入命令树重发回调。仅服务端入口在服务器启动后调用一次；
     * 纯客户端或无服务端上下文时留空（非 OP 补全由服务端命令树决定）。
     */
    public static void setCommandTreeResender(Runnable resender) {
        commandTreeResender = resender;
    }

    private static List<String> getSubList(Config cfg, String section) {
        Config sub = cfg.get(section);
        if (sub == null) return new ArrayList<>();
        return new ArrayList<>(sub.getOrElse("blacklist", Collections.emptyList()));
    }

    private static void setSubList(Config cfg, String section, List<String> list) {
        Config sub = Config.inMemory();
        sub.set("blacklist", list);
        cfg.set(section, sub);
    }

    private static void resetDefaults() {
        mode = MODE_DISABLED;
        showPauseButton = true;
        showTips = 1;
        nonOpWhitelist = new ArrayList<>();
        opBlacklist = new ArrayList<>();
        nonOpExempt = new ArrayList<>();
        opExempt = new ArrayList<>();
    }

    // ─── 判定方法 ───

    /**
     * 判断某玩家在本次命令执行中应走哪个功能域。
     *
     * @return 0=不约束；1=功能一（非OP白名单）；2=功能二（OP黑名单）
     */
    public static int resolveDomain(boolean isOp) {
        reloadIfChanged();
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
        reloadIfChanged();
        if (domain == 1) return nonOpExempt.contains(playerName);
        if (domain == 2) return opExempt.contains(playerName);
        return false;
    }

    /**
     * 判断指定命令名在指定功能域内是否应被拦截。
     *
     * @param domain 由 {@link #resolveDomain(boolean)} 得出：1=功能一，2=功能二
     */
    public static boolean shouldBlockCommand(int domain, String commandName) {
        reloadIfChanged();
        if (commandName == null) return false;
        String norm = normalizeCommand(commandName);
        if (domain == 1) {
            // 功能一（非OP，白名单）：未命中白名单 → 拦截（规范化匹配，与命令树补全放行逻辑一致）
            return !containsNormalized(nonOpWhitelist, norm);
        }
        if (domain == 2) {
            // 功能二（OP，黑名单）：命中黑名单 → 拦截（规范化匹配）
            return containsNormalized(opBlacklist, norm);
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
     * <p>功能一（白名单）：两个别名任一在白名单即放行，避免只配 {@code gamemode}
     * 而别名 {@code g} 未配导致游戏模式切换器仍被拦截；
     * 功能二（黑名单）：两个别名任一在黑名单即拦截。
     * 白名单项做规范化匹配（去 / 前缀、小写），容忍 {@code /gamemode}、{@code GameMode} 等写法。
     */
    public static boolean isGamemodeAllowed(int domain) {
        reloadIfChanged();
        if (domain == 1) {
            for (String entry : nonOpWhitelist) {
                if (isGamemodeCommand(normalizeCommand(entry))) return true;
            }
            return false;
        }
        if (domain == 2) {
            for (String entry : opBlacklist) {
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
