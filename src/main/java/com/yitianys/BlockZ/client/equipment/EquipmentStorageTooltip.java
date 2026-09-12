package com.yitianys.BlockZ.client.equipment;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT)
public final class EquipmentStorageTooltip {
    private EquipmentStorageTooltip() { }

    @SubscribeEvent
    public static void storage(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        int size = BlockZConfigs.getBackpackSlots(stack);
        boolean overflow = false;
        if (stack.hasTag()) {
            var contents = stack.getTag().getCompound("Inventory").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < contents.size(); i++) {
                var entry = contents.getCompound(i);
                if (entry.getInt("Slot") >= size && entry.getByte("Count") > 0) { overflow = true; break; }
            }
        }
        if (size > 0 || overflow) {
            int columns = Math.min(size, Math.min(9, ItemSizeManager.getCapacityCols(stack, 9)));
            event.getToolTip().add(Component.translatable("tooltip.blockz.storage_capacity", size, columns).withStyle(ChatFormatting.GRAY));
        }
        if (overflow) event.getToolTip().add(Component.translatable("tooltip.blockz.storage_overflow").withStyle(ChatFormatting.GOLD));
    }
}
