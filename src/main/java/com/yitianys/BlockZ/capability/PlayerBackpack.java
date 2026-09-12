package com.yitianys.BlockZ.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

public class PlayerBackpack implements INBTSerializable<CompoundTag> {
    private final net.minecraft.world.entity.player.Player owner;
    public PlayerBackpack() { this(null); }
    public PlayerBackpack(net.minecraft.world.entity.player.Player owner) { this.owner = owner; }

    public static final int SLOT_BACKPACK = 0;
    public static final int SLOT_VEST = 1;
    public static final int SLOT_GLOVES = 2;
    public static final int SLOT_MASK = 3;
    public static final int SLOT_COUNT = 4;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return owner == null ? !(stack.getItem() instanceof com.yitianys.BlockZ.item.ClothingItem)
                    : com.yitianys.BlockZ.equipment.EquipmentRules.acceptsSpecial(owner, slot, stack);
        }
    };

    private final ItemStackHandler armorInventory = new ItemStackHandler(com.yitianys.BlockZ.config.EquipmentConfig.MAX_ARMOR_SLOTS) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return owner == null ? com.yitianys.BlockZ.equipment.EquipmentRules.armorType(stack) != null
                    : slot < com.yitianys.BlockZ.equipment.EquipmentSlots.unlocked(owner)
                    && com.yitianys.BlockZ.equipment.EquipmentRules.acceptsArmor(owner, stack);
        }
    };
    private int armorSlotUpgrades;
    private boolean legacyArmorImported;
    private boolean specialEquipmentMigrated;

    public ItemStackHandler getArmorInventory() { return armorInventory; }
    public int getArmorSlotUpgrades() { return armorSlotUpgrades; }
    public void setArmorSlotUpgrades(int upgrades) { armorSlotUpgrades = net.minecraft.util.Mth.clamp(upgrades, 0, 3); }
    public boolean hasImportedLegacyArmor() { return legacyArmorImported; }
    public void markLegacyArmorImported() { legacyArmorImported = true; }
    public boolean hasMigratedSpecialEquipment() { return specialEquipmentMigrated; }
    public void markSpecialEquipmentMigrated() { specialEquipmentMigrated = true; }

    public ItemStackHandler getInventory() {
        ensureInventorySize();
        return inventory;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.put("ArmorInventory", armorInventory.serializeNBT());
        nbt.putInt("ArmorSlotUpgrades", armorSlotUpgrades);
        nbt.putBoolean("LegacyArmorImported", legacyArmorImported);
        nbt.putBoolean("SpecialEquipmentMigrated", specialEquipmentMigrated);
        
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("ArmorInventory")) {
            CompoundTag saved = nbt.getCompound("ArmorInventory").copy();
            saved.putInt("Size", com.yitianys.BlockZ.config.EquipmentConfig.MAX_ARMOR_SLOTS);
            armorInventory.deserializeNBT(saved);
        }
        setArmorSlotUpgrades(nbt.getInt("ArmorSlotUpgrades"));
        legacyArmorImported = nbt.getBoolean("LegacyArmorImported");
        specialEquipmentMigrated = nbt.getBoolean("SpecialEquipmentMigrated");
        if (nbt.contains("Inventory")) {
            CompoundTag invNbt = nbt.getCompound("Inventory");
            int expectedSize = SLOT_COUNT;
            inventory.deserializeNBT(invNbt);
            
            if (inventory.getSlots() != expectedSize) {
                repairInventorySize(expectedSize);
            }
        }
    }

    public void copyProgressFrom(PlayerBackpack other) {
        armorSlotUpgrades = other.armorSlotUpgrades;
        legacyArmorImported = other.legacyArmorImported;
        specialEquipmentMigrated = other.specialEquipmentMigrated;
    }

    public void copyFrom(PlayerBackpack other) {
        copyProgressFrom(other);
        for (int i = 0; i < armorInventory.getSlots(); i++) {
            armorInventory.setStackInSlot(i, other.armorInventory.getStackInSlot(i).copy());
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.inventory.setStackInSlot(i, other.inventory.getStackInSlot(i).copy());
        }
    }

    private void ensureInventorySize() {
        int expectedSize = SLOT_COUNT;
        if (inventory.getSlots() != expectedSize) {
            repairInventorySize(expectedSize);
        }
    }

    private void repairInventorySize(int expectedSize) {
        ItemStackHandler newInv = new ItemStackHandler(expectedSize);
        for (int i = 0; i < Math.min(expectedSize, inventory.getSlots()); i++) {
            newInv.setStackInSlot(i, inventory.getStackInSlot(i));
        }
        CompoundTag fixedNbt = newInv.serializeNBT();
        inventory.deserializeNBT(fixedNbt);
    }
}
