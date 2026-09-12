package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncEquipmentS2C(int entityId, CompoundTag state) {
    public static void encode(SyncEquipmentS2C message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId());
        buffer.writeNbt(message.state());
    }

    public static SyncEquipmentS2C decode(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        CompoundTag tag = buffer.readNbt();
        return new SyncEquipmentS2C(id, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncEquipmentS2C message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.yitianys.BlockZ.client.network.ClientPacketHandler.handleSyncEquipment(message)));
        context.get().setPacketHandled(true);
    }
}
