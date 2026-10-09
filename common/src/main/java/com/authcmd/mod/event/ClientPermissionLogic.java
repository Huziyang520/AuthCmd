package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

/**
 * 客户端侧的"提权感知"判定：非OP 在功能一（白名单）下使用目标选择器。
 *
 * <p>背景：非OP 命中白名单时由服务端**提权执行**指令，但**客户端本地**的解析与补全看的是
 * {@code ClientSuggestionProvider} 所用权限集，非OP 不含 {@code commands/entity_selectors}
 * ⇒ 输入 {@code @a} / {@code @e[...]} 之类会本地报"不能使用选择器"、且选择器参数不补全
 * （服务端仍会执行成功）。
 *
 * <p>本类给出"是否应把该权限视为已授予"的判定，由客户端 mixin
 * {@code com.authcmd.mod.mixin.ClientSuggestionProviderMixin} 消费；开关
 * {@code allow_entity_selectors} 关闭时判定恒为 false（客户端照原样报错，服务端也会拒绝）。
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
     * 是否应在客户端把"实体选择器"权限视为已授予。
     *
     * <p>条件：① 配置开关开启；② 功能一（非OP提权白名单）在本机视角下生效。
     * 未收到服务端配置（联机首帧 / 未装 AvalonBase）时本地 mode 为默认值 ⇒ 不放行。
     */
    public static boolean shouldGrantEntitySelectors() {
        if (!AuthCmdConfig.allowEntitySelectors) return false;
        return AuthCmdConfig.resolveDomain(false) == 1;
    }

    /** 按判定结果把"实体选择器"权限并入原权限集（客户端 mixin 调用）。 */
    public static PermissionSet grant(PermissionSet original) {
        if (original == null || !shouldGrantEntitySelectors()) return original;
        return original.union(ENTITY_SELECTORS_ONLY);
    }
}
