package com.punchyepicfightcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.punchyepicfightcompat.PunchyEpicFightCompat;
import com.punchyepicfightcompat.client.render.FirstPersonRenderArbitrator;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Punchy renders before Forge posts the per-hand RenderHandEvent. Recheck the
 * arbitration at Punchy's actual draw boundary so a newly-started Epic Fight
 * animation cannot leave Punchy's already-rendered model in the same frame.
 */
@Mixin(targets = "punchy.client.render.PunchyArmRenderer", remap = false)
public abstract class PunchyArmRendererMixin {
    @Unique
    private static boolean punchyEpicFightCompat$hookReported;

    @Inject(method = "renderFirstPerson", at = @At("HEAD"), cancellable = true, remap = false)
    private static void punchyEpicFightCompat$suppressPunchyBeforeDraw(
        ItemInHandRenderer itemInHandRenderer,
        LocalPlayer player,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        CallbackInfo callback
    ) {
        if (!punchyEpicFightCompat$hookReported) {
            punchyEpicFightCompat$hookReported = true;
            PunchyEpicFightCompat.LOGGER.info("Punchy pre-render arbitration hook is active");
        }
        if (FirstPersonRenderArbitrator.shouldSuppressPunchyRenderer(partialTick)) {
            callback.cancel();
        }
    }
}
