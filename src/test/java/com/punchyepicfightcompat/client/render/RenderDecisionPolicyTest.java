package com.punchyepicfightcompat.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenderDecisionPolicyTest {
    @Test
    void activeEpicActionWinsOverEveryIdleRule() {
        assertChoice(RendererChoice.EPIC_FIGHT, true, false, true, false, true, false);
    }

    @Test
    void swordStaysEpicEvenIfListedAsPunchyOrMining() {
        assertChoice(RendererChoice.EPIC_FIGHT, false, true, true, false, true, false);
    }

    @Test
    void forcedPunchyWinsOverForcedEpicAndNamespace() {
        assertChoice(RendererChoice.PUNCHY, false, false, true, true, false, true);
    }

    @Test
    void forcedEpicWinsDuringMining() {
        assertChoice(RendererChoice.EPIC_FIGHT, false, false, false, true, true, false);
    }

    @Test
    void miningUsesPunchyForAutomaticallyDetectedWeapon() {
        assertChoice(RendererChoice.PUNCHY, false, false, false, false, true, true);
    }

    @Test
    void configuredNamespaceWeaponUsesEpicOutsideMining() {
        assertChoice(RendererChoice.EPIC_FIGHT, false, false, false, false, false, true);
    }

    @Test
    void unconfiguredOrNonWeaponItemUsesPunchy() {
        assertChoice(RendererChoice.PUNCHY, false, false, false, false, false, false);
    }

    private static void assertChoice(
        RendererChoice expected,
        boolean action,
        boolean sword,
        boolean forcedPunchy,
        boolean forcedEpic,
        boolean mining,
        boolean configuredWeapon
    ) {
        assertEquals(
            expected,
            RenderDecisionPolicy.choose(
                action,
                sword,
                forcedPunchy,
                forcedEpic,
                mining,
                configuredWeapon
            )
        );
    }
}
