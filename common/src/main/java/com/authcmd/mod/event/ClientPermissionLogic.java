package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;

/**
 * 客户端侧的"提权感知"判定：非OP 在功能一（白名单）下使用目标选择器。
 *
 * <p>背景：非OP 命中白名单时由服务端**提权执行**指令，但**客户端本地**的解析与补全看的是
 * {@code ClientSuggestionProvider}（本档为 int 权限等级口径），非OP 的
 * {@code hasPermission(2)} 为 false
 * ⇒ 输入 {@code @a} / {@code @e[...]} 之类会本地报"不能使用选择器"、且选择器参数不补全
 * （服务端仍会执行成功）。
 *
 * <p>本类给出"是否应把该权限视为已授予"的判定，由客户端 mixin
 * {@code com.authcmd.mod.mixin.ClientSuggestionProviderMixin} 消费；开关
 * {@code allow_entity_selectors} 关闭时判定恒为 false（客户端照原样报错，服务端也会拒绝）。
 */
public final class ClientPermissionLogic {

    private ClientPermissionLogic() {
    }

    /**
     * 是否应在客户端把"实体选择器"权限视为已授予。
     *
     * <p>条件：① 配置开关开启；② 功能一（非OP提权白名单）在本机视角下生效。
     * 未收到服务端配置（联机首帧 / 未装 AvalonBase）时本地 mode 为默认值 ⇒ 不放行。
     *
     * <p>本档落点：本地解析/补全看 {@code ClientSuggestionProvider#hasPermission(int)}
     * （内部转手 {@code LocalPlayer.hasPermissions(int)}，非OP 为 false）；判定为 true
     * 且请求等级为 2（实体选择器）时，由 mixin 把该返回值置为 true。
     */
    public static boolean shouldGrantEntitySelectors() {
        if (!AuthCmdConfig.allowEntitySelectors) return false;
        return AuthCmdConfig.resolveDomain(false) == 1;
    }
}
