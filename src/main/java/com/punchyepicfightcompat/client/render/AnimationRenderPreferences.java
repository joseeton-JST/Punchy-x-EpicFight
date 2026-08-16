package com.punchyepicfightcompat.client.render;

import com.punchyepicfightcompat.PunchyEpicFightCompat;
import com.punchyepicfightcompat.client.config.ClientConfig;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AimAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Runtime catalogue and absolute renderer choice for registered animations. */
public final class AnimationRenderPreferences {
    private static final long RESCAN_INTERVAL_TICKS = 100L;

    private static Object lastPlayer;
    private static long lastScanTick = Long.MIN_VALUE;

    private AnimationRenderPreferences() {}

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            lastPlayer = null;
            lastScanTick = Long.MIN_VALUE;
            return;
        }

        if (lastPlayer != minecraft.player) {
            lastPlayer = minecraft.player;
            lastScanTick = Long.MIN_VALUE;
        }

        long tick = minecraft.player.tickCount;
        if (lastScanTick != Long.MIN_VALUE && tick - lastScanTick < RESCAN_INTERVAL_TICKS) {
            return;
        }
        lastScanTick = tick;

        discoverRegisteredAnimations();
    }

    /**
     * Returns the absolute preference for the animation that currently owns
     * the first-person pose. POV has priority, followed by Epic Fight's own
     * highest-to-lowest visible-layer order.
     */
    public static Optional<RendererChoice> activeChoice(LocalPlayerPatch playerPatch) {
        if (!playerPatch.getFirstPersonLayer().isOff()) {
            Optional<RendererChoice> firstPerson = choiceFor(
                playerPatch.getFirstPersonLayer().animationPlayer.getRealAnimation()
            );
            if (firstPerson.isPresent()) {
                return firstPerson;
            }
        }

        AtomicReference<RendererChoice> choice = new AtomicReference<>();
        playerPatch.getClientAnimator().iterVisibleLayersUntilFalse(layer -> {
            Optional<RendererChoice> layerChoice = choiceFor(layer.animationPlayer.getRealAnimation());
            layerChoice.ifPresent(choice::set);
            return layerChoice.isEmpty();
        });
        return Optional.ofNullable(choice.get());
    }

    private static Optional<RendererChoice> choiceFor(
        AssetAccessor<? extends StaticAnimation> animation
    ) {
        if (animation == null || animation.isEmpty() || animation.registryName() == null) {
            return Optional.empty();
        }
        Boolean useEpic = ClientConfig.current().animations().get(animation.registryName().toString());
        if (useEpic == null) {
            return Optional.empty();
        }
        return Optional.of(useEpic ? RendererChoice.EPIC_FIGHT : RendererChoice.PUNCHY);
    }

    private static void discoverRegisteredAnimations() {
        Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> registered;
        try {
            registered = AnimationManager.getInstance().getAnimations(accessor -> true);
        } catch (RuntimeException exception) {
            PunchyEpicFightCompat.LOGGER.debug("Epic Fight animations are not ready for discovery yet", exception);
            return;
        }

        Map<String, Boolean> existing = ClientConfig.current().animations();
        Map<String, Boolean> discovered = new LinkedHashMap<>();
        Set<ResourceLocation> customLivingAnimations = customLivingAnimationIds();
        for (Map.Entry<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> entry : registered.entrySet()) {
            String id = entry.getKey().toString();
            if (!existing.containsKey(id)) {
                discovered.put(
                    id,
                    defaultsToEpic(entry.getValue(), customLivingAnimations.contains(entry.getKey()))
                );
            }
        }
        ClientConfig.addDiscoveredAnimations(discovered);
    }

    private static boolean defaultsToEpic(
        AnimationAccessor<? extends StaticAnimation> accessor,
        boolean customLivingMotion
    ) {
        try {
            if (accessor == null || !accessor.isPresent()) {
                return false;
            }
            StaticAnimation animation = accessor.get();
            return AnimationDefaultPolicy.useEpic(
                animation instanceof MainFrameAnimation,
                animation instanceof AimAnimation,
                customLivingMotion
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Finds the animation accessors assigned to addon-defined living motions.
     * These can be long-lived action poses (for example hanging or wall running),
     * even though their concrete animation type is only StaticAnimation.
     */
    private static Set<ResourceLocation> customLivingAnimationIds() {
        Set<ResourceLocation> result = new HashSet<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return result;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(minecraft.player);
        if (playerPatch == null) {
            return result;
        }

        try {
            ClientAnimator animator = playerPatch.getClientAnimator();
            ResourceLocation idleAnimation = registryName(animator.getLivingMotion(LivingMotions.IDLE));
            for (LivingMotion motion : LivingMotion.ENUM_MANAGER.universalValues()) {
                if (motion instanceof LivingMotions) {
                    continue;
                }

                addIfCustom(result, animator.getLivingMotion(motion), idleAnimation);
                addIfCustom(result, animator.getCompositeLivingMotion(motion), idleAnimation);
            }
        } catch (RuntimeException exception) {
            PunchyEpicFightCompat.LOGGER.debug(
                "Addon living motions are not ready for animation default discovery yet",
                exception
            );
        }
        return result;
    }

    private static void addIfCustom(
        Set<ResourceLocation> result,
        AssetAccessor<? extends StaticAnimation> accessor,
        ResourceLocation idleAnimation
    ) {
        ResourceLocation animation = registryName(accessor);
        if (animation != null && !animation.equals(idleAnimation)) {
            result.add(animation);
        }
    }

    private static ResourceLocation registryName(AssetAccessor<? extends StaticAnimation> accessor) {
        return accessor == null || accessor.isEmpty() ? null : accessor.registryName();
    }

}
