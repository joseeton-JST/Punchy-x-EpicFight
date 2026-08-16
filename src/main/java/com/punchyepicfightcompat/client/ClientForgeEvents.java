package com.punchyepicfightcompat.client;

import com.punchyepicfightcompat.PunchyEpicFightCompat;
import com.punchyepicfightcompat.client.config.ClientConfig;
import com.punchyepicfightcompat.client.input.EmptyHandBlockMiningFix;
import com.punchyepicfightcompat.client.render.EntityAttackRenderTracker;
import com.punchyepicfightcompat.client.render.AnimationRenderPreferences;
import com.punchyepicfightcompat.client.render.FirstPersonRenderArbitrator;
import com.punchyepicfightcompat.client.render.PunchyRenderBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = PunchyEpicFightCompat.MOD_ID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PunchyRenderBridge.apply(FirstPersonRenderArbitrator.decide(event.renderTickTime));
        } else {
            PunchyRenderBridge.restore();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            EmptyHandBlockMiningFix.tick();
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            ClientConfig.tick();
            EpicFightModeController.enforce();
            AnimationRenderPreferences.tick();
            if (Minecraft.getInstance().player == null) {
                EntityAttackRenderTracker.clear();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null && event.getEntity() == localPlayer && localPlayer.level().isClientSide) {
            EntityAttackRenderTracker.record(localPlayer);
        }
    }
}
