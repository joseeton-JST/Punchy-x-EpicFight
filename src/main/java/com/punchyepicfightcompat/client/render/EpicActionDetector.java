package com.punchyepicfightcompat.client.render;

import java.util.concurrent.atomic.AtomicBoolean;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

public final class EpicActionDetector {
    private EpicActionDetector() {}

    public static boolean isActive(LocalPlayerPatch playerPatch, boolean mining) {
        if (!playerPatch.getFirstPersonLayer().isOff()) {
            return true;
        }

        EntityState state = playerPatch.getEntityState();
        if (state.inaction() || state.attacking() || state.hurt() || state.knockDown()) {
            return true;
        }

        AtomicBoolean actionAnimation = new AtomicBoolean(false);
        playerPatch.getClientAnimator().iterVisibleLayersUntilFalse(layer -> {
            if (layer.animationPlayer.getRealAnimation().get() instanceof ActionAnimation) {
                actionAnimation.set(true);
                return false;
            }
            return true;
        });
        if (actionAnimation.get()) {
            return true;
        }

        return !mining && !(playerPatch.getCurrentLivingMotion() instanceof LivingMotions);
    }
}
