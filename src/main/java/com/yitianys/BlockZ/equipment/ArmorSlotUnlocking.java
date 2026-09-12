package com.yitianys.BlockZ.equipment;

import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.config.EquipmentConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "blockz")
public final class ArmorSlotUnlocking {
    private ArmorSlotUnlocking() { }

    /** Unlocks one permanent additional slot; called only on the authoritative server. */
    public static boolean unlockOne(ServerPlayer player) {
        if (EquipmentSlots.unlocked(player) >= EquipmentConfig.MAX_ARMOR_SLOTS) return false;
        return player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).map(cap -> {
            cap.setArmorSlotUpgrades(cap.getArmorSlotUpgrades() + 1);
            EquipmentState.sync(player);
            return true;
        }).orElse(false);
    }

    public static boolean isUnlockItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && EquipmentConfig.UNLOCK_ITEMS.get().contains(id.toString());
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!isUnlockItem(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean unlocked = unlockOne(player);
        if (unlocked && !player.getAbilities().instabuild) event.getItemStack().shrink(1);
        player.displayClientMessage(Component.translatable(unlocked ? "msg.blockz.armor_slot_unlocked" : "msg.blockz.armor_slots_full",
                EquipmentSlots.unlocked(player)), true);
    }
}
