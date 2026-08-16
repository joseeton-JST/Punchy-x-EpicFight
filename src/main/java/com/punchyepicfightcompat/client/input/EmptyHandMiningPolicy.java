package com.punchyepicfightcompat.client.input;

/** Pure precedence rule for the narrowly scoped empty-hand mining fix. */
public final class EmptyHandMiningPolicy {
    private EmptyHandMiningPolicy() {}

    public static boolean shouldYieldToVanilla(
        boolean fixEnabled,
        boolean epicFightMode,
        boolean emptyMainHand,
        boolean reachableBlockTarget,
        boolean activeEpicAction,
        boolean nearbyAttackableEntity
    ) {
        return fixEnabled
            && epicFightMode
            && emptyMainHand
            && reachableBlockTarget
            && !activeEpicAction
            && !nearbyAttackableEntity;
    }
}
