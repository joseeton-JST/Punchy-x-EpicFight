package com.punchyepicfightcompat.client.input;

import com.punchyepicfightcompat.client.config.ClientConfig;
import com.punchyepicfightcompat.client.render.EpicActionDetector;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Lets Minecraft handle empty-hand block breaking without replacing Epic Fight's
 * general entity/block targeting. Epic Fight still owns the click whenever an
 * attackable living entity is close enough to be a plausible melee target.
 */
public final class EmptyHandBlockMiningFix {
    private static boolean drivingVanillaMining;

    private EmptyHandBlockMiningFix() {}

    /**
     * Reads the physical attack button without modifying either the vanilla or
     * Epic Fight key mappings. MultiPlayerGameMode remains Minecraft's public
     * vanilla/Forge owner of block-breaking validation, progress and packets.
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        LocalPlayerPatch playerPatch = player == null ? null : EpicFightCapabilities.getLocalPlayerPatch(player);
        boolean physicalAttackDown = isPhysicallyDown(minecraft, minecraft.options.keyAttack);
        boolean shouldDrive = playerPatch != null
            && physicalAttackDown
            && shouldYieldToVanilla(playerPatch);

        BlockHitResult blockHit = minecraft.hitResult instanceof BlockHitResult hit ? hit : null;
        if (shouldDrive && blockHit != null && minecraft.gameMode != null) {
            boolean progressed = minecraft.gameMode.isDestroying()
                ? minecraft.gameMode.continueDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection())
                : minecraft.gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
            if (progressed) {
                player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (drivingVanillaMining) {
            if (minecraft.gameMode != null) {
                minecraft.gameMode.stopDestroyBlock();
            }
        }

        drivingVanillaMining = shouldDrive && blockHit != null && minecraft.gameMode != null;
    }

    public static boolean shouldYieldToVanilla(LocalPlayerPatch playerPatch) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean enabled = ClientConfig.current().emptyHandBlockBreaking();
        boolean epicMode = playerPatch.isEpicFightMode();
        boolean emptyMainHand = player != null && player.getMainHandItem().isEmpty();
        boolean reachableBlock = player != null && isReachableBlockTarget(minecraft, player);

        if (!enabled || !epicMode || !emptyMainHand || !reachableBlock) {
            return false;
        }

        boolean activeEpicAction = EpicActionDetector.isActive(playerPatch, true);
        boolean nearbyEntity = hasNearbyAttackableEntity(player);
        return EmptyHandMiningPolicy.shouldYieldToVanilla(
            true,
            true,
            true,
            true,
            activeEpicAction,
            nearbyEntity
        );
    }

    private static boolean isReachableBlockTarget(Minecraft minecraft, LocalPlayer player) {
        if (!(minecraft.hitResult instanceof BlockHitResult blockHit)
            || blockHit.getType() != HitResult.Type.BLOCK
            || minecraft.level == null
            || minecraft.gameMode == null) {
            return false;
        }

        BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            return false;
        }

        double reach = player.getBlockReach();
        return player.getEyePosition().distanceToSqr(blockHit.getLocation()) <= reach * reach;
    }

    private static boolean hasNearbyAttackableEntity(LocalPlayer player) {
        double reach = player.getEntityReach();
        double reachSquared = reach * reach;
        AABB searchBounds = player.getBoundingBox().inflate(reach);
        Vec3 eye = player.getEyePosition();

        return !player.level().getEntitiesOfClass(
            LivingEntity.class,
            searchBounds,
            entity -> entity != player
                && entity.isAlive()
                && !entity.isRemoved()
                && !entity.isSpectator()
                && entity.isAttackable()
                && entity.isPickable()
                && player.hasLineOfSight(entity)
                && distanceToBoxSquared(eye, entity.getBoundingBox()) <= reachSquared
        ).isEmpty();
    }

    private static double distanceToBoxSquared(Vec3 point, AABB box) {
        double closestX = Mth.clamp(point.x, box.minX, box.maxX);
        double closestY = Mth.clamp(point.y, box.minY, box.maxY);
        double closestZ = Mth.clamp(point.z, box.minZ, box.maxZ);
        return point.distanceToSqr(closestX, closestY, closestZ);
    }

    private static boolean isPhysicallyDown(Minecraft minecraft, KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        return mapping.isDown();
    }
}
