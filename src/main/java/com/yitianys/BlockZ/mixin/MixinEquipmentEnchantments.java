package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.equipment.EquipmentSlots;
import com.yitianys.BlockZ.equipment.EquipmentView;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Includes every equipped stack while retaining each enchantment's native slot restrictions. */
@Mixin(EnchantmentHelper.class)
public abstract class MixinEquipmentEnchantments {
    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), cancellable = true)
    private static void blockz$enchantmentLevel(Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof Player player) || EquipmentView.active(entity)) return;
        int level = cir.getReturnValue();
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            boolean eligible = EquipmentView.query(player, entry.type(), entry.stack(), () -> enchantment.getSlotItems(player).get(entry.type()) == entry.stack());
            if (eligible) level = Math.max(level, entry.stack().getEnchantmentLevel(enchantment));
        }
        cir.setReturnValue(level);
    }

    @Inject(method = "getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;", at = @At("HEAD"), cancellable = true)
    private static void blockz$randomEquippedItem(Enchantment enchantment, LivingEntity entity, Predicate<ItemStack> predicate,
                                                CallbackInfoReturnable<Map.Entry<EquipmentSlot, ItemStack>> cir) {
        if (!(entity instanceof Player player) || EquipmentView.active(entity)) return;
        List<Map.Entry<EquipmentSlot, ItemStack>> choices = new ArrayList<>();
        for (var entry : enchantment.getSlotItems(player).entrySet()) {
            if (entry.getValue().getEnchantmentLevel(enchantment) > 0 && predicate.test(entry.getValue())) choices.add(entry);
        }
        int vanillaCount = choices.size();
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            if (entry.stack().getEnchantmentLevel(enchantment) <= 0 || !predicate.test(entry.stack())) continue;
            boolean eligible = EquipmentView.query(player, entry.type(), entry.stack(), () -> enchantment.getSlotItems(player).get(entry.type()) == entry.stack());
            if (eligible && choices.stream().noneMatch(choice -> choice.getValue() == entry.stack())) choices.add(Map.entry(entry.type(), entry.stack()));
        }
        if (choices.size() > vanillaCount) cir.setReturnValue(choices.get(player.getRandom().nextInt(choices.size())));
    }
}
