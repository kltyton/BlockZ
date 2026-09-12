package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.equipment.EquipmentSlots;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolves the actual extra stack selected by local Mesh projectile durability calculations. */
@Pseudo
@Mixin(targets = "com.kltyton.mesharmoury.event.DamageEventHandler", remap = false)
public abstract class MixinMeshEquipmentDamage {
    @Inject(method = "findStackSlot", at = @At("RETURN"), cancellable = true)
    private static void blockz$extraSlot(LivingEntity entity, ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (cir.getReturnValue() != null || !(entity instanceof Player player)) return;
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            if (entry.stack() == stack) { cir.setReturnValue(entry.type()); return; }
        }
    }
}
