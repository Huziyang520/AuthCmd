package com.authcmd.mod.event;

import com.authcmd.mod.config.AuthCmdConfig;

public final class ClientPermissionLogic {

    private ClientPermissionLogic() {
    }

    public static boolean shouldGrantEntitySelectors() {
        if (!AuthCmdConfig.allowEntitySelectors()) return false;
        return AuthCmdConfig.resolveDomain(false) == 1;
    }
}
