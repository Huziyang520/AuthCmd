package com.authcmd.mod.mixin;

import com.authcmd.mod.event.ClientPermissionLogic;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderMixin {

    @Inject(method = "permissions", at = @At("RETURN"), cancellable = true)
    private void authcmd$grantEntitySelectors(CallbackInfoReturnable<PermissionSet> cir) {
        cir.setReturnValue(ClientPermissionLogic.grant(cir.getReturnValue()));
    }
}
