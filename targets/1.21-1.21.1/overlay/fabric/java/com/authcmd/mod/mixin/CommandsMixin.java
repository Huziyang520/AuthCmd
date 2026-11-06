package com.authcmd.mod.mixin;

import com.authcmd.mod.event.CommandLogic;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric 命令执行拦截（等价于原 Forge CommandEvent）。
 * 在 Commands.performCommand(ParseResults, String) 处拦截，直接使用解析结果判断是否取消。
 *
 * <p>注意：1.21 起 performCommand 返回 void（此前为 int），因此使用 {@link CallbackInfo} + cancel()
 * 而非返回码。</p>
 */
@Mixin(Commands.class)
public class CommandsMixin {

    @Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private void authcmd$interceptCommand(ParseResults<CommandSourceStack> parseResults, String command,
                                          CallbackInfo ci) {
        if (CommandLogic.shouldCancel(parseResults)) {
            ci.cancel();
        }
    }
}
