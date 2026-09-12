package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.equipment.EquipmentSlots;
import com.yitianys.BlockZ.equipment.EquipmentView;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/** Extends armor iteration and durability while keeping adapter read views scoped to one call. */
@Mixin(Player.class)
public abstract class MixinPlayerEquipment {
    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void blockz$scopedEquipment(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack view = EquipmentView.item((Player) (Object) this, slot);
        if (view != null) cir.setReturnValue(view);
    }

    @Redirect(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack blockz$startExtraElytra(Player player, EquipmentSlot slot) {
        return EquipmentSlots.flightStack(player, player.getItemBySlot(slot));
    }

    @Inject(method = "getArmorSlots", at = @At("RETURN"), cancellable = true)
    private void blockz$armorItems(CallbackInfoReturnable<Iterable<ItemStack>> cir) {
        Player player = (Player) (Object) this;
        List<ItemStack> armor = new ArrayList<>();
        cir.getReturnValue().forEach(armor::add);
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) armor.add(entry.stack());
        cir.setReturnValue(armor);
    }

    @Inject(method = "hurtArmor", at = @At("TAIL"))
    private void blockz$damageExtraArmor(DamageSource source, float amount, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (amount <= 0) return;
        int damage = Math.max(1, (int) (amount / 4));
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            if (source.is(DamageTypeTags.IS_FIRE) && entry.stack().getItem().isFireResistant()) continue;
            entry.stack().hurtAndBreak(damage, player, entity -> entity.broadcastBreakEvent(entry.type()));
        }
    }
}
