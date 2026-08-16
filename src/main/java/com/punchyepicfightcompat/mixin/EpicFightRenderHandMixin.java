package com.punchyepicfightcompat.mixin;

import com.punchyepicfightcompat.client.render.FirstPersonRenderArbitrator;
import net.minecraftforge.client.event.RenderHandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "yesman.epicfight.client.events.engine.RenderEngine$Events", remap = false)
public abstract class EpicFightRenderHandMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true, remap = false)
    private static void punchyEpicFightCompat$selectRenderer(RenderHandEvent event, CallbackInfo callback) {
        if (FirstPersonRenderArbitrator.shouldBypassEpicHandler(event)) {
            callback.cancel();
        }
    }
}
