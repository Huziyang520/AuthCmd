package com.authcmd.mod.mixin;

import com.authcmd.mod.event.ClientPermissionLogic;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderMixin {

    @Inject(method = "allowsSelectors", at = @At("HEAD"), cancellable = true)
    private void authcmd$grantEntitySelectors(CallbackInfoReturnable<Boolean> cir) {
        if (ClientPermissionLogic.shouldGrantEntitySelectors()) cir.setReturnValue(true);
    }
}
