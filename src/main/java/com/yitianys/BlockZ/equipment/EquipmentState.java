package com.yitianys.BlockZ.equipment;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncEquipmentS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "blockz")
public final class EquipmentState {
    private record Applied(CompoundTag snapshot, Multimap<Attribute, AttributeModifier> attributes) { }
    private static final Map<Player, Applied> APPLIED = new WeakHashMap<>();
    private EquipmentState() { }

    public static void initialize(ServerPlayer player) {
        CuriosIntegration.importToCapability(player);
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            if (!cap.hasImportedLegacyArmor()) {
                for (int i = 0; i < 4; i++) {
                    ItemStack stack = player.getInventory().armor.get(i);
                    if (!stack.isEmpty()) {
                        cap.getArmorInventory().setStackInSlot(i, stack);
                        player.getInventory().armor.set(i, ItemStack.EMPTY);
                    }
                }
                cap.markLegacyArmorImported();
            }
        });
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap ->
                NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new SyncEquipmentS2C(player.getId(), cap.serializeNBT())));
    }

    public static void syncTo(ServerPlayer viewer, Player subject) {
        subject.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap ->
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                        new SyncEquipmentS2C(subject.getId(), cap.serializeNBT())));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.isAlive()) return;
        Player player = event.player;
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            EquipmentView.queryEquipped(player, entry.type(), entry.stack(), () -> {
                if (entry.managedByCurios()) entry.stack().getItem().onArmorTick(entry.stack(), player.level(), player);
                else entry.stack().onInventoryTick(player.level(), player, 36 + entry.type().getIndex(), player.getInventory().selected);
                return null;
            });
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        // Vanilla right-click, dispensers and mod equip APIs still write the vanilla armor inventory.
        if (!player.isCreative()) player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            for (EquipmentSlot type : EquipmentSlot.values()) {
                if (type.getType() != EquipmentSlot.Type.ARMOR) continue;
                ItemStack stack = player.getInventory().armor.get(type.getIndex());
                if (stack.isEmpty()) continue;
                int free = EquipmentSlots.firstFreeArmorSlot(player);
                player.getInventory().armor.set(type.getIndex(), ItemStack.EMPTY);
                if (free >= 0 && EquipmentRules.acceptsArmor(player, stack)) cap.getArmorInventory().setStackInSlot(free, stack);
                else if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        });
        CuriosIntegration.importToCapability(serverPlayer);
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            CompoundTag snapshot = cap.serializeNBT();
            Applied previous = APPLIED.get(player);
            if (previous != null && previous.snapshot().equals(snapshot)) return;
            if (previous != null) player.getAttributes().removeAttributeModifiers(previous.attributes());
            Multimap<Attribute, AttributeModifier> attributes = LinkedHashMultimap.create();
            for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
                if (!entry.managedByCurios()) attributes.putAll(EquipmentAttributes.forStack(entry.id(), entry.stack(), entry.type()));
            }
            player.getAttributes().addTransientAttributeModifiers(attributes);
            APPLIED.put(player, new Applied(snapshot, attributes));
            com.yitianys.BlockZ.compat.MeshEquipment.changed(player);
            sync(serverPlayer);
        });
    }
}
