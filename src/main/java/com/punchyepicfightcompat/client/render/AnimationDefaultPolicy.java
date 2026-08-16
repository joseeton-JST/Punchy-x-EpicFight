package com.punchyepicfightcompat.client.render;

/** Pure policy kept separate so addon living-motion defaults can be regression tested. */
final class AnimationDefaultPolicy {
    private AnimationDefaultPolicy() {}

    static boolean useEpic(boolean mainFrame, boolean aiming, boolean customLivingMotion) {
        return mainFrame || aiming || customLivingMotion;
    }
}
