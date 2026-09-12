package com.yitianys.BlockZ.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Invoker("renderSlot") void blockz$renderSlot(GuiGraphics graphics, Slot slot);
    @Invoker("renderFloatingItem") void blockz$renderFloatingItem(GuiGraphics graphics, ItemStack stack, int x, int y, String count);
    @Invoker("renderBg") void blockz$renderBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);
    @Invoker("renderLabels") void blockz$renderLabels(GuiGraphics graphics, int mouseX, int mouseY);
    @Accessor("draggingItem") ItemStack blockz$getDraggingItem();
    @Accessor("isSplittingStack") boolean blockz$isSplittingStack();
    @Accessor("quickCraftingRemainder") int blockz$getQuickCraftingRemainder();
    @Accessor("hoveredSlot") void blockz$setHoveredSlot(Slot slot);
}
