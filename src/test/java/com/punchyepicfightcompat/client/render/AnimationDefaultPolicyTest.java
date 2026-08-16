package com.punchyepicfightcompat.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimationDefaultPolicyTest {
    @Test
    void addonLivingMotionDefaultsToEpicEvenWhenAnimationIsStatic() {
        assertTrue(AnimationDefaultPolicy.useEpic(false, false, true));
    }

    @Test
    void ordinaryStaticAnimationStillDefaultsToPunchy() {
        assertFalse(AnimationDefaultPolicy.useEpic(false, false, false));
    }

    @Test
    void mainFrameAndAimAnimationsStillDefaultToEpic() {
        assertTrue(AnimationDefaultPolicy.useEpic(true, false, false));
        assertTrue(AnimationDefaultPolicy.useEpic(false, true, false));
    }
}
