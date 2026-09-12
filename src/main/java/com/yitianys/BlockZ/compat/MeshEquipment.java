package com.yitianys.BlockZ.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;

public final class MeshEquipment {
    private static java.util.function.ToIntFunction<ItemStack> slotProvider = stack -> 0;
    private static java.util.function.ToIntFunction<ItemStack> sizeProvider = stack -> -1;
    private static java.util.function.ToIntFunction<ItemStack> columnsProvider = stack -> -1;

    private MeshEquipment() { }

    /** IMC providers avoid linking Mesh classes when the optional mod is absent. */
    @SuppressWarnings("unchecked")
    public static void acceptProviders(net.minecraftforge.fml.event.lifecycle.InterModProcessEvent event) {
        event.getIMCStream().filter(message -> message.senderModId().equals("mesharmoury")).forEach(message -> {
            Object supplied = message.messageSupplier().get();
            if (!(supplied instanceof java.util.function.ToIntFunction<?>)) return;
            java.util.function.ToIntFunction<ItemStack> provider = (java.util.function.ToIntFunction<ItemStack>) supplied;
            switch (message.method()) {
                case "equipment_slot" -> slotProvider = provider;
                case "storage_size" -> sizeProvider = provider;
                case "storage_columns" -> columnsProvider = provider;
                default -> { }
            }
        });
    }

    public static int specialSlot(ItemStack stack) { return stack.isEmpty() ? 0 : slotProvider.applyAsInt(stack); }
    public static int storageSize(ItemStack stack) { return stack.isEmpty() ? -1 : sizeProvider.applyAsInt(stack); }
    public static int storageColumns(ItemStack stack) { return stack.isEmpty() ? -1 : columnsProvider.applyAsInt(stack); }
    public static void changed(Player player) {
        if (ModList.get().isLoaded("mesharmoury"))
            com.yitianys.BlockZ.mixin.MeshEquipmentEventsAccess.blockz$updateSpeed(
                    new LivingEquipmentChangeEvent(player, EquipmentSlot.CHEST, ItemStack.EMPTY, ItemStack.EMPTY));
    }
}
