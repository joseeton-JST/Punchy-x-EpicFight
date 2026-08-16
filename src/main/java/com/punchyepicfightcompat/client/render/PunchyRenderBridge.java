package com.punchyepicfightcompat.client.render;

import com.punchyepicfightcompat.PunchyEpicFightCompat;
import java.lang.reflect.Method;

public final class PunchyRenderBridge {
    private static final Method SET_FIRST_PERSON_HIDDEN = findHideMethod();
    private static boolean invocationFailureReported;

    private PunchyRenderBridge() {}

    public static void apply(RendererChoice choice) {
        setFirstPersonHidden(choice == RendererChoice.EPIC_FIGHT);
    }

    public static void restore() {
        setFirstPersonHidden(false);
    }

    private static Method findHideMethod() {
        try {
            Class<?> rendererClass = Class.forName("punchy.client.render.PunchyArmRenderer");
            return rendererClass.getMethod("setFirstPersonHidden", boolean.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            PunchyEpicFightCompat.LOGGER.warn(
                "Punchy no longer exposes PunchyArmRenderer.setFirstPersonHidden(boolean); "
                    + "renderer hiding is disabled instead of crashing the client",
                exception
            );
            return null;
        }
    }

    private static void setFirstPersonHidden(boolean hidden) {
        if (SET_FIRST_PERSON_HIDDEN == null) {
            return;
        }

        try {
            SET_FIRST_PERSON_HIDDEN.invoke(null, hidden);
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!invocationFailureReported) {
                invocationFailureReported = true;
                PunchyEpicFightCompat.LOGGER.warn(
                    "Punchy rejected its first-person visibility call; renderer hiding is disabled",
                    exception
                );
            }
        }
    }
}
