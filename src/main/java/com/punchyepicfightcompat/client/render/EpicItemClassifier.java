package com.punchyepicfightcompat.client.render;

import com.punchyepicfightcompat.PunchyEpicFightCompat;
import com.punchyepicfightcompat.client.config.ItemOverrideMatcher;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public final class EpicItemClassifier {
    private static final TagKey<Item> FORGE_SWORDS = itemTag("forge", "swords");
    private static final TagKey<Item> FORGE_TOOL_SWORDS = itemTag("forge", "tools/swords");
    private static boolean warnedCapabilityFailure;

    private EpicItemClassifier() {}

    public static boolean isSword(ItemStack stack) {
        return !stack.isEmpty()
            && (stack.getItem() instanceof SwordItem
                || stack.is(FORGE_SWORDS)
                || stack.is(FORGE_TOOL_SWORDS));
    }

    public static boolean isMiningTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DiggerItem;
    }

    public static boolean isEpicCombatWeapon(ItemStack stack) {
        CapabilityItem capability = capability(stack);
        return capability != null
            && !capability.isEmpty()
            && capability.getWeaponCategory() != CapabilityItem.WeaponCategories.NOT_WEAPON;
    }

    public static boolean isConfiguredEpicWeapon(
        ItemStack stack,
        ItemOverrideMatcher.Snapshot overrides
    ) {
        return !stack.isEmpty()
            && overrides.namespaceAllows(stack)
            && isEpicCombatWeapon(stack);
    }

    public static float epicWeaponReach(ItemStack stack) {
        CapabilityItem capability = capability(stack);
        return capability == null ? 0.0F : Math.max(0.0F, capability.getReach());
    }

    private static CapabilityItem capability(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        try {
            return EpicFightCapabilities.getItemStackCapability(stack);
        } catch (RuntimeException exception) {
            if (!warnedCapabilityFailure) {
                warnedCapabilityFailure = true;
                PunchyEpicFightCompat.LOGGER.warn(
                    "Could not inspect an Epic Fight item capability; treating that item as non-Epic.",
                    exception
                );
            }
            return null;
        }
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }
}
