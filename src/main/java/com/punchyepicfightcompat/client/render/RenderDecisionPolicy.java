package com.punchyepicfightcompat.client.render;

public final class RenderDecisionPolicy {
    private RenderDecisionPolicy() {}

    public static RendererChoice choose(
        boolean activeEpicAction,
        boolean swordLike,
        boolean forcedPunchy,
        boolean forcedEpic,
        boolean mining,
        boolean configuredEpicWeapon
    ) {
        if (activeEpicAction) {
            return RendererChoice.EPIC_FIGHT;
        }
        if (swordLike) {
            return RendererChoice.EPIC_FIGHT;
        }
        if (forcedPunchy) {
            return RendererChoice.PUNCHY;
        }
        if (forcedEpic) {
            return RendererChoice.EPIC_FIGHT;
        }
        if (mining) {
            return RendererChoice.PUNCHY;
        }
        return configuredEpicWeapon ? RendererChoice.EPIC_FIGHT : RendererChoice.PUNCHY;
    }
}
