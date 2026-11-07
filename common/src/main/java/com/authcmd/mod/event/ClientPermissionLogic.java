package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

/**
 * Client-side elevation: non-operators using selectors under the whitelist domain.
 * <p>Background: a whitelisted command is elevated by the server, but local parsing and suggestions
 * {@code ClientSuggestionProvider}'s permission set, which lacks {@code commands/entity_selectors}
 */
public final class ClientPermissionLogic {

    /** Permission set containing only {@code commands/entity_selectors}. */
    private static final PermissionSet ENTITY_SELECTORS_ONLY = new PermissionSet() {
        @Override
        public boolean hasPermission(Permission permission) {
            return Permissions.COMMANDS_ENTITY_SELECTORS.equals(permission);
        }
    };

    private ClientPermissionLogic() {
    }

    /**
     * /**
     * Whether the client should treat the selector permission as granted.
     */
    public static boolean shouldGrantEntitySelectors() {
        if (!AuthCmdConfig.allowEntitySelectors()) return false;
        return AuthCmdConfig.resolveDomain(false) == 1;
    }

    /** Merges the selector permission into the original set (called from the client mixin). */
    public static PermissionSet grant(PermissionSet original) {
        if (original == null || !shouldGrantEntitySelectors()) return original;
        return original.union(ENTITY_SELECTORS_ONLY);
    }
}
