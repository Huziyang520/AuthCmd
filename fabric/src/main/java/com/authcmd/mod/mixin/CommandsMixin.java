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
 * Fabric command blocking (equivalent to Forge's CommandEvent).
 * Hooks Commands.performCommand(ParseResults, String) and cancels using the parsed result.
 * <p>Note: performCommand returns void since 1.21, so this uses {@link CallbackInfo} and cancel()
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