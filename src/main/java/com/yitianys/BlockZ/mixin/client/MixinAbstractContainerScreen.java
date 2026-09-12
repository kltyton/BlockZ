package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.gui.PanelInventoryScreen;
import com.yitianys.BlockZ.client.gui.DayZContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {
    @Shadow @Nullable private Slot clickedSlot;
    @Shadow private ItemStack draggingItem;

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void blockz$renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if ((Object) this instanceof PanelInventoryScreen<?> screen
                && screen.renderCustomSlot(graphics, slot, clickedSlot, draggingItem)) ci.cancel();
    }

    @Inject(method = "findSlot", at = @At("HEAD"), cancellable = true)
    private void blockz$findVisibleSlot(double x, double y, CallbackInfoReturnable<Slot> cir) {
        if ((Object) this instanceof PanelInventoryScreen<?> screen) cir.setReturnValue(screen.findVisibleSlot(x, y));
        else if (DayZContainerScreen.isNativeDelegate(this)) cir.setReturnValue(null);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void blockz$renderNativeControls(GuiGraphics graphics, int x, int y, float partialTick, CallbackInfo ci) {
        if (DayZContainerScreen.isRenderingNative(this)) {
            DayZContainerScreen.renderNativeBase((AbstractContainerScreen<?>) (Object) this, graphics, x, y, partialTick);
            ci.cancel();
        }
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void blockz$skipNativeLabels(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (DayZContainerScreen.isRenderingNative(this)) ci.cancel();
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void blockz$singleSlotInputOwner(Slot slot, int index, int button, ClickType type, CallbackInfo ci) {
        if (DayZContainerScreen.isNativeDelegate(this)) ci.cancel();
    }
}
