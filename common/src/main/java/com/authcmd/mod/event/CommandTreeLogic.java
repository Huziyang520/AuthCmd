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
 * Command tree permission widening (platform-free).
 * <p>Brigadier filters nodes by their {@code requires} predicate while building the suggestion tree,
 * so level-2 commands disappear for non-operators before the whitelist is consulted.
 */
public final class CommandTreeLogic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * /**
     * the node to every reachable root command name (normalized).
     */
    private static final Map<CommandNode<CommandSourceStack>, Set<String>> NODE_ROOT_NAMES = new IdentityHashMap<>();

    /** Reflectively located requirement field, resolved lazily. */
    static Field requirementField;

    /** Root of the last patch; a server reload rebuilds the tree, so a changed root triggers a re-patch. */
    private static CommandNode<CommandSourceStack> lastRoot;

    /** Diagnostics: how many commands were unblocked by the whitelist (logged a limited number of times). */
    private static volatile long grantCount = 0;

    private CommandTreeLogic() {
    }

    /**
     * /**
     * After command registration, wraps every node's requirement with the dynamic pass-through predicate.
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
     * Walks a subtree, recording root command names and wrapping its predicates.
     */
    private static void patchTree(CommandNode<CommandSourceStack> node, String rootName) {
        String norm = AuthCmdConfig.normalizeCommand(rootName);
        if (norm.isEmpty()) return;

        Set<String> rootNames = NODE_ROOT_NAMES.computeIfAbsent(node, k -> new CopyOnWriteArraySet<>());
        if (!rootNames.add(norm)) return; // 该 (节点, 根命令名) 组合已处理

        // Idempotent: a node is wrapped once, so predicates never nest.
        // An already wrapped node holds this very set, so appending a root name takes effect immediately.
        if (!(node.getRequirement() instanceof WhitelistRequirement)) {
            setRequirement(node, new WhitelistRequirement(node.getRequirement(), rootNames));
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            patchTree(child, norm);
        }

        // Follows redirects: alias and /execute targets need the same root command name registered
        if (node.getRedirect() != null) {
            patchTree(node.getRedirect(), norm);
        }
    }

    /**
     * /**
     * Dynamic pass-through predicate. The original requirement is tried first, which is the fast path for
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

    /** Whether the whitelist domain is active in the current mode. */
    private static boolean isModeActive() {
        return AuthCmdConfig.MODE_NON_OP.equals(AuthCmdConfig.mode())
                || AuthCmdConfig.MODE_BOTH.equals(AuthCmdConfig.mode());
    }

    /**
     * /**
     * Whitelist hit test.
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
            // Brigadier is not obfuscated, so the field name matches on both loaders
            requirementField = CommandNode.class.getDeclaredField("requirement");
            requirementField.setAccessible(true);
        } catch (NoSuchFieldException | RuntimeException e) {
            LOGGER.error("[AuthCmd] Cannot access CommandNode.requirement field", e);
            requirementField = null;
        }
        return requirementField;
    }
}