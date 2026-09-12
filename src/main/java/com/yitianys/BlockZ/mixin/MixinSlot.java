package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class MixinSlot {

    @Shadow public abstract ItemStack getItem();

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void onMayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.getItem().is(ModItems.LOCK_ITEM.get())) {
            cir.setReturnValue(false);
        }
    }
    @Unique
    private boolean blockz$outsidePockets() {
        Slot slot = (Slot) (Object) this;
        return slot.container instanceof Inventory inventory
                && com.yitianys.BlockZ.ui.DayZUiPolicy.shouldUseDayZ(inventory.player)
                && slot.getSlotIndex() >= 9 + com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots()
                && slot.getSlotIndex() < 36;
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void blockz$restrictPocketInsertion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (blockz$outsidePockets()) cir.setReturnValue(false);
    }

    @Inject(method = {"getMaxStackSize()I", "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I"},
            at = @At("HEAD"), cancellable = true)
    private void blockz$restrictPocketMerging(CallbackInfoReturnable<Integer> cir) {
        if (blockz$outsidePockets()) cir.setReturnValue(0);
    }

}
