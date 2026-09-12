package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.equipment.EquipmentSlots;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class MixinEquipmentFlight {
    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack blockz$tickExtraElytra(LivingEntity entity, EquipmentSlot slot) {
        ItemStack original = entity.getItemBySlot(slot);
        return entity instanceof Player player ? EquipmentSlots.flightStack(player, original) : original;
    }
}
