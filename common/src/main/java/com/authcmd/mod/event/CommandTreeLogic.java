package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/**
 * 命令树权限放行逻辑（平台无关）。
 *
 * <p>背景：Minecraft 的指令树（Brigadier）中 {@code /gamemode} 等指令的 {@code requires}
 * 要求权限 2，非OP玩家在补全树过滤阶段就被剔除，导致本模组"非OP指令白名单"里的指令
 * 无法 Tab 补全。
 *
 * <p>实现：在命令注册阶段（Forge {@code RegisterCommandsEvent} / Fabric
 * {@code CommandRegistrationCallback}）用反射把每个节点的 {@code requirement} 换成动态谓词：
 * 原始 requires 通过则放行；否则当模式为 {@code non_op_only}/{@code both} 且该节点所属的
 * 根命令命中白名单时放行。动态谓词每次 {@code canUse} 都重新读白名单，因此增删白名单后
 * 只需重发命令树包即可生效，无需重新 patch。
 *
 * <p><b>别名（redirect）处理</b>：vanilla 用 {@code literal("tp").redirect(teleportNode)}
 * 实现别名，即 {@code /tp} 与 {@code /teleport} 共享同一棵子树。因此一个节点可能同时属于
 * 多个根命令，必须记录"可到达该节点的全部根命令名"，任一命中白名单即放行——否则白名单填
 * {@code tp} 时，共享子树因为先以 {@code teleport} 被登记而永远匹配不上，表现为
 * {@code /tp} 后面的参数全都补全不出来。
 *
 * <p>关于反射：Brigadier 库不参与混淆，{@code CommandNode.requirement} 在 Forge(MCP) 与
 * Fabric(Yarn) 下字段名一致，可安全定位；Java 17 下 {@code setAccessible(true)} 允许写入
 * 非静态 final 实例字段。
 */
public final class CommandTreeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 节点 → 可到达该节点的全部根命令名（已规范化）。
     * <p>按 identity 索引：Brigadier 节点的 equals 会比较子节点，用 identity 才能正确区分实例，
     * 也才能阻断 redirect 造成的环。
     * <p>value 用 {@link CopyOnWriteArraySet}：{@link WhitelistRequirement} 持有的是该集合的
     * <b>引用</b>，后续别名遍历往里追加根命令名时，已建好的谓词会立刻看到新值；同时保证
     * patch 与 canUse 万一并发时迭代不抛 {@code ConcurrentModificationException}。
     */
    private static final Map<CommandNode<CommandSourceStack>, Set<String>> NODE_ROOT_NAMES = new IdentityHashMap<>();

    /** 反射定位到的 requirement 字段（惰性初始化）。 */
    static Field requirementField;

    /** 上次 patch 的根节点。服务端 reload 会重建命令树，根对象变化时清空缓存重新 patch。 */
    private static CommandNode<CommandSourceStack> lastRoot;

    /** 诊断：因白名单而放行的次数（仅前若干次打日志，避免刷屏）。 */
    private static volatile long grantCount = 0;

    private CommandTreeLogic() {
    }

    /**
     * 在命令注册完成后，为命令树的每个节点套上动态放行谓词。
     *
     * @param root 服务端命令树根节点（{@code dispatcher.getRoot()}）
     */
    public static void patch(CommandNode<CommandSourceStack> root) {
        if (root == null) {
            LOGGER.warn("[AuthCmd] patch called with null root, skipping");
            return;
        }
        if (findRequirementField() == null) {
            LOGGER.error("[AuthCmd] requirementField unavailable, command tree patch is a NO-OP! "
                    + "Non-OP whitelist commands will NOT be tab-completable.");
            return;
        }

        if (root != lastRoot) {
            NODE_ROOT_NAMES.clear();
            lastRoot = root;
            LOGGER.info("[AuthCmd] Command tree instance changed, re-patching all nodes");
        }

        int before = NODE_ROOT_NAMES.size();
        for (CommandNode<CommandSourceStack> child : root.getChildren()) {
            patchTree(child, child.getName());
        }

        grantCount = 0;
        LOGGER.info("[AuthCmd] Command tree patched - {} nodes tracked (+{} new), mode={}, whitelist={}",
                NODE_ROOT_NAMES.size(), NODE_ROOT_NAMES.size() - before,
                AuthCmdConfig.mode, AuthCmdConfig.nonOpWhitelist);
    }

    /**
     * 递归为一棵子树登记根命令名并套上动态谓词。
     *
     * <p>终止条件是「(节点, 根命令名) 这一组合已处理过」而不是「节点已处理过」：
     * 前者允许同一棵共享子树被第二个别名再遍历一次以补登记根命令名，同时仍能阻断环。
     */
    private static void patchTree(CommandNode<CommandSourceStack> node, String rootName) {
        String norm = AuthCmdConfig.normalizeCommand(rootName);
        if (norm.isEmpty()) return;

        Set<String> rootNames = NODE_ROOT_NAMES.computeIfAbsent(node, k -> new CopyOnWriteArraySet<>());
        if (!rootNames.add(norm)) return; // 该 (节点, 根命令名) 组合已处理

        // 幂等：同一节点只包一层，避免动态谓词嵌套。
        // 已包裹过的节点其谓词持有的正是上面这个 rootNames 集合，追加即生效。
        if (!(node.getRequirement() instanceof WhitelistRequirement)) {
            setRequirement(node, new WhitelistRequirement(node.getRequirement(), rootNames));
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            patchTree(child, norm);
        }

        // 跟随 redirect：别名与 /execute 等重定向命令的目标子树同样需要登记本根命令名
        if (node.getRedirect() != null) {
            patchTree(node.getRedirect(), norm);
        }
    }

    /**
     * 动态放行谓词。先走原始 requires 快路径（绝大多数节点在此返回，OP 与权限0命令都不会
     * 触碰配置），仅当原始判定失败时才热加载配置并检查白名单。
     */
    private static final class WhitelistRequirement implements Predicate<CommandSourceStack> {
        private final Predicate<CommandSourceStack> original;
        private final Set<String> rootNames;

        WhitelistRequirement(Predicate<CommandSourceStack> original, Set<String> rootNames) {
            this.original = original;
            this.rootNames = rootNames;
        }

        @Override
        public boolean test(CommandSourceStack source) {
            if (original != null && original.test(source)) return true;

            AuthCmdConfig.reloadIfChanged();
            if (!isModeActive()) return false;

            for (String rootName : rootNames) {
                if (isWhitelisted(rootName)) {
                    long n = ++grantCount;
                    if (n <= 10) {
                        LOGGER.info("[AuthCmd] whitelist grant #{}: rootNames={} -> allowed", n, rootNames);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /** 功能一（非OP白名单）是否在当前 mode 下生效。 */
    private static boolean isModeActive() {
        return AuthCmdConfig.MODE_NON_OP.equals(AuthCmdConfig.mode)
                || AuthCmdConfig.MODE_BOTH.equals(AuthCmdConfig.mode);
    }

    /**
     * 白名单命中判断。
     * <p>{@code gamemode} 与 {@code g} 视为同一命令的两个别名整体判定，与
     * {@code AuthCmdConfig.isGamemodeAllowed} 及 GameModeLogic 保持一致；
     * 真正通过 redirect 实现的别名（tp/teleport 等）已由 {@link #NODE_ROOT_NAMES} 覆盖，
     * 无需在此枚举。规范化逻辑统一复用 {@link AuthCmdConfig#normalizeCommand}。
     */
    static boolean isWhitelisted(String rootName) {
        String n = AuthCmdConfig.normalizeCommand(rootName);
        if (n.isEmpty()) return false;
        return AuthCmdConfig.containsNormalized(AuthCmdConfig.nonOpWhitelist, n, true);
    }

    private static void setRequirement(CommandNode<CommandSourceStack> node, Predicate<CommandSourceStack> predicate) {
        Field field = findRequirementField();
        if (field == null) return;
        try {
            field.set(node, predicate);
        } catch (IllegalAccessException e) {
            LOGGER.error("[AuthCmd] Failed to patch command '{}' permission", node.getName(), e);
        }
    }

    private static Field findRequirementField() {
        if (requirementField != null) return requirementField;
        try {
            // CommandNode.requirement 在两个加载器映射下字段名一致（Brigadier 库不混淆）
            requirementField = CommandNode.class.getDeclaredField("requirement");
            requirementField.setAccessible(true);
        } catch (NoSuchFieldException | RuntimeException e) {
            LOGGER.error("[AuthCmd] Cannot access CommandNode.requirement field", e);
            requirementField = null;
        }
        return requirementField;
    }
}