package com.punchyepicfightcompat.client;

import com.punchyepicfightcompat.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public final class EpicFightModeController {
    private EpicFightModeController() {}

    public static void enforce() {
        if (!ClientConfig.current().forceEpicFightMode()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (patch != null && !patch.isEpicFightMode()) {
            patch.toEpicFightMode(true);
        }
    }
}
