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
 * <p>背景：Minecraft 的指令树（Brigadier）中 {@code /gamemode} 等指令的 {@code requires}
 * 要求权限 2，非OP玩家在补全树过滤阶段就被剔除，导致本模组"非OP指令白名单"里的指令
 */
public final class CommandTreeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * /**
     * 节点 → 可到达该节点的全部根命令名（已规范化）。
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
     * /**
     * 在命令注册完成后，为命令树的每个节点套上动态放行谓词。
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
                AuthCmdConfig.mode(), AuthCmdConfig.nonOpWhitelist());
    }

    /**
     * /**
     * 递归为一棵子树登记根命令名并套上动态谓词。
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
     * /**
     * 动态放行谓词。先走原始 requires 快路径（绝大多数节点在此返回，OP 与权限0命令都不会
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

            AuthCmdConfig.CONFIG.reload();
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
        return AuthCmdConfig.MODE_NON_OP.equals(AuthCmdConfig.mode())
                || AuthCmdConfig.MODE_BOTH.equals(AuthCmdConfig.mode());
    }

    /**
     * /**
     * 白名单命中判断。
     */
    static boolean isWhitelisted(String rootName) {
        String n = AuthCmdConfig.normalizeCommand(rootName);
        if (n.isEmpty()) return false;
        return AuthCmdConfig.containsNormalized(AuthCmdConfig.nonOpWhitelist(), n, true);
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