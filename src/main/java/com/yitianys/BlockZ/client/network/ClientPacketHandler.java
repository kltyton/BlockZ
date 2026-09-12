package com.yitianys.BlockZ.client.network;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import com.yitianys.BlockZ.network.SyncPlayerStatusS2C;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleSyncBackpack(SyncBackpackS2C msg, Supplier<NetworkEvent.Context> ctx) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
                cap.getInventory().setStackInSlot(msg.getSlotId(), msg.getStack());
            });
        }
    }

    public static void handleSyncEquipment(com.yitianys.BlockZ.network.SyncEquipmentS2C message) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(message.entityId()) instanceof net.minecraft.world.entity.player.Player player) {
            player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> cap.deserializeNBT(message.state()));
        }
    }

    public static void handleSyncPlayerStatus(SyncPlayerStatusS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ClientSettings.healthPointsRatio = msg.getHealthPointsRatio();
        ClientSettings.healthRatio = msg.getHealthRatio();
        ClientSettings.staminaRatio = msg.getStaminaRatio();
        ClientSettings.infectionRatio = msg.getInfectionRatio();
    }

}
