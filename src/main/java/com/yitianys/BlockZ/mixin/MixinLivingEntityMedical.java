package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.nursing.MedicalItemUse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityMedical {
    @Redirect(method = "stopUsingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;onStopUsing(Lnet/minecraft/world/entity/LivingEntity;I)V", remap = false))
    private void blockz$stop(ItemStack stack, LivingEntity entity, int remaining) {
        MedicalItemUse.stop(entity, stack, remaining);
    }
}
