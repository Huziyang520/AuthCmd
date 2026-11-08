package com.authcmd.mod.gui;

import com.avalon.base.gui.screen.AvalonConfigScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Method;

final class CursorSync {

    private static Method cached;
    private static boolean resolved;

    private CursorSync() {
    }

    static void apply(AvalonConfigScreen screen, GuiGraphics graphics, boolean hand) {
        Method m = resolve();
        if (m == null) {
            return;
        }
        try {
            if (m.getParameterCount() == 2) {
                m.invoke(screen, graphics, hand);
            } else {
                m.invoke(screen, hand);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static synchronized Method resolve() {
        if (resolved) {
            return cached;
        }
        resolved = true;
        Class<?>[][] signatures = {{GuiGraphics.class, boolean.class}, {boolean.class}};
        for (Class<?>[] types : signatures) {
            try {
                Method m = AvalonConfigScreen.class.getDeclaredMethod("updateCursor", types);
                m.setAccessible(true);
                cached = m;
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return cached;
    }
}
