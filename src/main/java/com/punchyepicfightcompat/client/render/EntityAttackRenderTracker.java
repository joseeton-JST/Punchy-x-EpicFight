package com.punchyepicfightcompat.client.render;

import net.minecraft.client.player.LocalPlayer;

/**
 * Bridges the short gap between the client attack input and Epic Fight publishing
 * its ActionAnimation/EntityState. The normal action detector keeps ownership once
 * the animation is visible.
 */
public final class EntityAttackRenderTracker {
    private static final int FORCE_TICKS_AFTER_ENTITY_ATTACK = 12;

    private static LocalPlayer trackedPlayer;
    private static int forceUntilTick = Integer.MIN_VALUE;

    private EntityAttackRenderTracker() {}

    public static void record(LocalPlayer player) {
        trackedPlayer = player;
        forceUntilTick = player.tickCount + FORCE_TICKS_AFTER_ENTITY_ATTACK;
    }

    public static boolean isForcingEpic(LocalPlayer player) {
        return trackedPlayer == player && player.tickCount <= forceUntilTick;
    }

    public static void clear() {
        trackedPlayer = null;
        forceUntilTick = Integer.MIN_VALUE;
    }
}
