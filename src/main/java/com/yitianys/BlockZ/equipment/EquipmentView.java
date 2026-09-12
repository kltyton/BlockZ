package com.yitianys.BlockZ.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

/** A scoped read view for adapters that can process one armor stack per vanilla type. */
public final class EquipmentView {
    private record View(LivingEntity entity, EquipmentSlot type, ItemStack stack, java.util.List<EquipmentSlots.Entry> others) { }
    private static final ThreadLocal<View> CURRENT = new ThreadLocal<>();
    private EquipmentView() { }

    public static boolean active(LivingEntity entity) {
        View view = CURRENT.get();
        return view != null && view.entity() == entity;
    }

    @Nullable
    public static ItemStack item(LivingEntity entity, EquipmentSlot type) {
        View view = CURRENT.get();
        if (view == null || view.entity() != entity || type.getType() != EquipmentSlot.Type.ARMOR) return null;
        if (view.type() == type) return view.stack();
        if (view.others() != null) {
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                ItemStack vanilla = player.getInventory().armor.get(type.getIndex());
                if (!vanilla.isEmpty()) return vanilla;
            }
            for (EquipmentSlots.Entry entry : view.others()) if (entry.type() == type) return entry.stack();
        }
        return ItemStack.EMPTY;
    }

    public static <T> T query(LivingEntity entity, EquipmentSlot type, ItemStack stack, Supplier<T> action) {
        return withView(entity, type, stack, null, action);
    }

    public static <T> T queryEquipped(net.minecraft.world.entity.player.Player player, EquipmentSlot type, ItemStack stack, Supplier<T> action) {
        return withView(player, type, stack, EquipmentSlots.entries(player), action);
    }

    private static <T> T withView(LivingEntity entity, EquipmentSlot type, ItemStack stack, java.util.List<EquipmentSlots.Entry> others, Supplier<T> action) {
        View previous = CURRENT.get();
        CURRENT.set(new View(entity, type, stack, others));
        try { return action.get(); }
        finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
