package com.punchyepicfightcompat.client.render;

import com.punchyepicfightcompat.client.config.ItemOverrideMatcher;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderHandEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public final class FirstPersonRenderArbitrator {
    private FirstPersonRenderArbitrator() {}

    public static boolean shouldBypassEpicHandler(RenderHandEvent event) {
        RendererChoice choice = decide(event.getPartialTick());
        PunchyRenderBridge.apply(choice);
        return choice == RendererChoice.PUNCHY;
    }

    public static boolean shouldSuppressPunchyRenderer(float partialTick) {
        RendererChoice choice = decide(partialTick);
        PunchyRenderBridge.apply(choice);
        return choice == RendererChoice.EPIC_FIGHT;
    }

    public static RendererChoice decide(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return RendererChoice.NO_INTERVENTION;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return calculate(player, mainHand, offHand);
    }

    private static RendererChoice calculate(LocalPlayer player, ItemStack mainHand, ItemStack offHand) {
        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch == null || !playerPatch.isEpicFightMode() || !ClientConfig.enableAnimatedFirstPersonModel) {
            return RendererChoice.NO_INTERVENTION;
        }

        // Animation preferences are absolute: they intentionally take priority
        // over attacks, swords, item overrides, namespaces and mining context.
        java.util.Optional<RendererChoice> animationChoice = AnimationRenderPreferences.activeChoice(playerPatch);
        if (animationChoice.isPresent()) {
            return animationChoice.get();
        }

        ItemStack controllingStack = mainHand.isEmpty() ? offHand : mainHand;
        ItemOverrideMatcher.Snapshot overrides = ItemOverrideMatcher.current();
        // Read-only render context. Epic Fight remains the sole owner of attack,
        // target selection, click consumption and block breaking behavior.
        boolean mining = Minecraft.getInstance().gameMode != null
            && Minecraft.getInstance().gameMode.isDestroying();
        boolean activeEpicAction = EntityAttackRenderTracker.isForcingEpic(player)
            || EpicActionDetector.isActive(playerPatch, mining);
        boolean swordLike = EpicItemClassifier.isSword(controllingStack);
        boolean forcedPunchy = overrides.forcePunchy().matches(controllingStack);
        boolean forcedEpic = overrides.forceEpic().matches(controllingStack);
        boolean configuredEpicWeapon = EpicItemClassifier.isConfiguredEpicWeapon(controllingStack, overrides);

        return RenderDecisionPolicy.choose(
            activeEpicAction,
            swordLike,
            forcedPunchy,
            forcedEpic,
            mining,
            configuredEpicWeapon
        );
    }
}
