package com.authcmd.mod.mixin;

import com.authcmd.mod.event.ClientPermissionLogic;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端：把"实体选择器"权限并入本地命令解析/补全的判定。
 *
 * <p>本区间（1.20.5-1.20.6）没有 {@code PermissionSet}：本地命令解析与补全用的是
 * {@code ClientSuggestionProvider}（实现 {@code net.minecraft.commands.SharedSuggestionProvider}）
 * 的 int 权限等级口径，而 {@code EntitySelectorParser} 以 {@code hasPermission(2)}
 * 判定选择器可用性（实现转手 {@code LocalPlayer.hasPermissions(int)}），非OP 为 false，于是
 * 含 {@code @} 的指令会在本地报 "不能使用选择器"、选择器参数不补全（服务端因提权执行仍会生效）。
 * 此处按 {@link ClientPermissionLogic#shouldGrantEntitySelectors()} 的判定，
 * <b>仅把等级 2（实体选择器）</b>的返回值置为 true，使本地行为与服务端的提权口径一致，
 * 且不影响其它权限等级。
 *
 * <p>注入点已按 1.20.6 真实字节码核定：{@code ClientSuggestionProvider} 只有
 * {@code hasPermission(int)}，本档（1.20.5-1.20.6）并无 {@code allowsSelectors()} 覆写，
 * 因此不能照抄 1.21.x 分支线的注入点。
 *
 * <p>只在客户端生效（本类挂在 mixin 配置的 {@code client} 列表里）。
 */
@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderMixin {

    /** EntitySelectorParser 判定选择器权限所用的等级。 */
    private static final int SELECTOR_PERMISSION_LEVEL = 2;

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true, require = 1)
    private void authcmd$grantEntitySelectors(int level, CallbackInfoReturnable<Boolean> cir) {
        if (level == SELECTOR_PERMISSION_LEVEL && ClientPermissionLogic.shouldGrantEntitySelectors()) {
            cir.setReturnValue(true);
        }
    }
}
