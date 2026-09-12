package com.yitianys.BlockZ.equipment;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.EquipmentConfig;
import com.yitianys.BlockZ.item.ClothingItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.ArrayList;
import java.util.List;

public final class EquipmentSlots {
    public record Entry(String id, ItemStack stack, EquipmentSlot type, @Nullable String curioId, int curioIndex) {
        public boolean managedByCurios() { return curioId != null; }
    }
    private static final int[] SPECIAL_SLOTS = {PlayerBackpack.SLOT_MASK, PlayerBackpack.SLOT_VEST, PlayerBackpack.SLOT_BACKPACK};
    private EquipmentSlots() { }

    public static int unlocked(Player player) {
        int upgrades = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).map(PlayerBackpack::getArmorSlotUpgrades).orElse(0);
        return Math.min(EquipmentConfig.MAX_ARMOR_SLOTS, EquipmentConfig.INITIAL_ARMOR_SLOTS.get() + upgrades);
    }

    public static ItemStack armor(Player player, int index) {
        return player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getArmorInventory().getStackInSlot(index)).orElse(ItemStack.EMPTY);
    }

    public static int visibleArmorSlots(Player player) {
        int count = unlocked(player);
        for (int i = count; i < EquipmentConfig.MAX_ARMOR_SLOTS; i++) if (!armor(player, i).isEmpty()) count = i + 1;
        return count;
    }

    public static int firstFreeArmorSlot(Player player) {
        for (int i = 0; i < unlocked(player); i++) if (armor(player, i).isEmpty()) return i;
        return -1;
    }

    public static ItemStack special(Player player, int index) {
        String curio = EquipmentRules.curioSlot(index);
        if (curio != null && CuriosIntegration.hasSlotHandler(player, curio)) return CuriosIntegration.getEquippedDirect(player, curio);
        return player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getInventory().getStackInSlot(index)).orElse(ItemStack.EMPTY);
    }

    public static ItemStack flightStack(Player player, ItemStack original) {
        if (original.canElytraFly(player)) return original;
        for (Entry entry : entries(player)) {
            if (entry.type() == EquipmentSlot.CHEST && entry.stack().canElytraFly(player)) return entry.stack();
        }
        return original;
    }

    public static List<Entry> entries(Player player) {
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < EquipmentConfig.MAX_ARMOR_SLOTS; i++) {
            ItemStack stack = armor(player, i);
            EquipmentSlot type = EquipmentRules.armorType(player, stack);
            if (type != null) result.add(new Entry("armor/" + i, stack, type, null, -1));
        }
        for (int index : SPECIAL_SLOTS) {
            String curio = EquipmentRules.curioSlot(index);
            if (CuriosIntegration.hasSlotHandler(player, curio)) continue;
            ItemStack stack = special(player, index);
            if (stack.isEmpty() || stack.getItem() instanceof ClothingItem) continue;
            EquipmentSlot type = index == PlayerBackpack.SLOT_MASK ? EquipmentSlot.HEAD : EquipmentSlot.CHEST;
            result.add(new Entry("special/" + index, stack, type, null, -1));
        }
        if (CuriosIntegration.isLoaded()) CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((id, group) -> {
                var stacks = group.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    EquipmentSlot type = EquipmentRules.armorType(player, stack);
                    if (type != null) result.add(new Entry("curios/" + id + "/" + i, stack, type, id, i));
                }
            });
        });
        return result;
    }
}
