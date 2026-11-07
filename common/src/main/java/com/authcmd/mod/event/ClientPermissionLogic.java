package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

/**
 * 客户端侧的"提权感知"判定：非OP 在功能一（白名单）下使用目标选择器。
 * <p>背景：非OP 命中白名单时由服务端**提权执行**指令，但**客户端本地**的解析与补全看的是
 * {@code ClientSuggestionProvider} 所用权限集，非OP 不含 {@code commands/entity_selectors}
 */
public final class ClientPermissionLogic {

    /** 只含 {@code commands/entity_selectors} 的权限集。 */
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
     * 是否应在客户端把"实体选择器"权限视为已授予。
     */
    public static boolean shouldGrantEntitySelectors() {
        if (!AuthCmdConfig.allowEntitySelectors()) return false;
        return AuthCmdConfig.resolveDomain(false) == 1;
    }

    /** 按判定结果把"实体选择器"权限并入原权限集（客户端 mixin 调用）。 */
    public static PermissionSet grant(PermissionSet original) {
        if (original == null || !shouldGrantEntitySelectors()) return original;
        return original.union(ENTITY_SELECTORS_ONLY);
    }
}
