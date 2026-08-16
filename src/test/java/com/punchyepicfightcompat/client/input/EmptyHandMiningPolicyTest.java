package com.punchyepicfightcompat.client.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmptyHandMiningPolicyTest {
    @Test
    void reachableBlockWithoutNearbyEntityUsesVanillaMining() {
        assertTrue(decide(true, true, true, true, false, false));
    }

    @Test
    void nearbyEntityKeepsEpicFightAttack() {
        assertFalse(decide(true, true, true, true, false, true));
    }

    @Test
    void activeEpicActionIsNeverInterrupted() {
        assertFalse(decide(true, true, true, true, true, false));
    }

    @Test
    void itemInMainHandIsOutsideTheFix() {
        assertFalse(decide(true, true, false, true, false, false));
    }

    @Test
    void airOrOutOfReachBlockKeepsEpicFightBehavior() {
        assertFalse(decide(true, true, true, false, false, false));
    }

    @Test
    void vanillaModeIsUntouched() {
        assertFalse(decide(true, false, true, true, false, false));
    }

    @Test
    void perspectiveDoesNotLimitTheMiningFix() {
        // Perspective is intentionally absent from the policy: first-person,
        // third-person back and third-person front use the same input rule.
        assertTrue(decide(true, true, true, true, false, false));
    }

    @Test
    void disabledFixDoesNothing() {
        assertFalse(decide(false, true, true, true, false, false));
    }

    private static boolean decide(
        boolean enabled,
        boolean epicMode,
        boolean emptyHand,
        boolean block,
        boolean action,
        boolean entity
    ) {
        return EmptyHandMiningPolicy.shouldYieldToVanilla(
            enabled,
            epicMode,
            emptyHand,
            block,
            action,
            entity
        );
    }
}
