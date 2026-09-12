package com.yitianys.BlockZ.equipment;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

/** Delegates special menu slots to their actual owner, including empty Curios slots. */
public final class SpecialEquipmentHandler implements IItemHandlerModifiable {
    private final Player player;
    public SpecialEquipmentHandler(Player player) { this.player = player; }
    @Override public int getSlots() { return 4; }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public ItemStack getStackInSlot(int slot) { return EquipmentSlots.special(player, slot); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return EquipmentRules.acceptsSpecial(player, slot, stack); }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        String curio = EquipmentRules.curioSlot(slot);
        if (curio != null && CuriosIntegration.hasSlotHandler(player, curio)) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(curio)
                    .ifPresent(group -> group.getStacks().setStackInSlot(0, stack)));
        } else player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .ifPresent(cap -> cap.getInventory().setStackInSlot(slot, stack));
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !getStackInSlot(slot).isEmpty() || !isItemValid(slot, stack)) return stack;
        if (!simulate) setStackInSlot(slot, stack.copyWithCount(1));
        return stack.copyWithCount(stack.getCount() - 1);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack current = getStackInSlot(slot);
        if (amount <= 0 || current.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = current.copyWithCount(Math.min(amount, current.getCount()));
        if (!simulate) setStackInSlot(slot, current.copyWithCount(current.getCount() - extracted.getCount()));
        return extracted;
    }
}
