package com.yitianys.BlockZ.equipment;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.item.BackpackItem;
import com.yitianys.BlockZ.item.ClothingItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class EquipmentRules {
    public static final TagKey<Item> HELMETS = tag("equipment/helmets");
    public static final TagKey<Item> VESTS = tag("equipment/vests");
    public static final TagKey<Item> BACKPACKS = tag("backpacks");
    private static final EquipmentSlot[] ARMOR_TYPES = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private EquipmentRules() { }
    private static TagKey<Item> tag(String path) { return ItemTags.create(new ResourceLocation("blockz", path)); }

    @Nullable
    public static EquipmentSlot armorType(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ClothingItem) return null;
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        if (slot.getType() == EquipmentSlot.Type.ARMOR) return slot;
        if (stack.is(HELMETS)) return EquipmentSlot.HEAD;
        if (stack.is(VESTS)) return EquipmentSlot.CHEST;
        return null;
    }

    @Nullable
    public static EquipmentSlot armorType(@Nullable Player player, ItemStack stack) {
        EquipmentSlot standard = armorType(stack);
        if (standard != null || player == null || stack.isEmpty() || stack.getItem() instanceof ClothingItem) return standard;
        for (EquipmentSlot type : ARMOR_TYPES) if (stack.canEquip(type, player)) return type;
        return null;
    }

    public static boolean acceptsArmor(Player player, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ClothingItem) return false;
        if (com.yitianys.BlockZ.compat.MeshEquipment.specialSlot(stack) != 0) return false;
        if (stack.is(HELMETS) || stack.is(VESTS)) return true;
        for (EquipmentSlot type : ARMOR_TYPES) if (stack.canEquip(type, player)) return true;
        return false;
    }

    public static boolean acceptsSpecial(Player player, int slot, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ClothingItem) return false;
        int meshSlot = com.yitianys.BlockZ.compat.MeshEquipment.specialSlot(stack);
        if (meshSlot != 0) return slot == switch (meshSlot) {
            case 1 -> PlayerBackpack.SLOT_MASK;
            case 2 -> PlayerBackpack.SLOT_VEST;
            case 3 -> PlayerBackpack.SLOT_BACKPACK;
            default -> -1;
        };
        return switch (slot) {
            case PlayerBackpack.SLOT_MASK -> stack.is(HELMETS) || stack.canEquip(EquipmentSlot.HEAD, player)
                    || CuriosIntegration.supportsSlot(player, stack, CuriosIntegration.SLOT_HEAD);
            case PlayerBackpack.SLOT_VEST -> stack.is(VESTS) || stack.canEquip(EquipmentSlot.CHEST, player)
                    || CuriosIntegration.supportsSlot(player, stack, CuriosIntegration.SLOT_BODY);
            case PlayerBackpack.SLOT_BACKPACK -> stack.getItem() instanceof BackpackItem || stack.is(BACKPACKS)
                    || CuriosIntegration.supportsSlot(player, stack, CuriosIntegration.SLOT_BACK);
            default -> false;
        };
    }

    @Nullable
    public static String curioSlot(int slot) {
        return switch (slot) {
            case PlayerBackpack.SLOT_MASK -> CuriosIntegration.SLOT_HEAD;
            case PlayerBackpack.SLOT_VEST -> CuriosIntegration.SLOT_BODY;
            case PlayerBackpack.SLOT_BACKPACK -> CuriosIntegration.SLOT_BACK;
            default -> null;
        };
    }
}
