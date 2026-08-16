package com.punchyepicfightcompat.mixin;

import com.punchyepicfightcompat.client.config.ClientConfig;
import com.punchyepicfightcompat.client.input.EmptyHandBlockMiningFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(targets = "yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch", remap = false)
public abstract class EpicFightPlayerPatchMixin {
    @Inject(method = "toVanillaMode", at = @At("HEAD"), cancellable = true, remap = false)
    private void punchyEpicFightCompat$keepEpicMode(boolean synchronize, CallbackInfo callback) {
        if (ClientConfig.current().forceEpicFightMode()) {
            callback.cancel();
        }
    }

    @Inject(method = "canPlayAttackAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void punchyEpicFightCompat$yieldEmptyHandBlockToVanilla(CallbackInfoReturnable<Boolean> callback) {
        if (EmptyHandBlockMiningFix.shouldYieldToVanilla((LocalPlayerPatch) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

}
