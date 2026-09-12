package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.equipment.EquipmentSlots;
import com.yitianys.BlockZ.equipment.EquipmentView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderArmEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Includes independent equipment entries in Mesh skin coverage and first-person armor rendering. */
@Pseudo
@Mixin(targets = "com.kltyton.mesharmoury.client.render.armor.MeshArmorRenderEvents", remap = false)
public abstract class MixinMeshArmorRendering {
    @Invoker("hasEquippedMeshArmor")
    private static boolean blockz$hasArmor(LivingEntity entity) { throw new AssertionError(); }
    @Invoker("renderFirstPersonArmorArm")
    private static void blockz$renderArm(AbstractClientPlayer player, EquipmentSlot slot, RenderArmEvent event) { throw new AssertionError(); }

    @Inject(method = "hasEquippedMeshArmor", at = @At("RETURN"), cancellable = true)
    private static void blockz$extraArmor(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !(entity instanceof Player player) || EquipmentView.active(entity)) return;
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            if (EquipmentView.query(entity, entry.type(), entry.stack(), () -> blockz$hasArmor(entity))) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "onRenderArm", at = @At("TAIL"))
    private static void blockz$extraArms(RenderArmEvent event, CallbackInfo ci) {
        AbstractClientPlayer player = event.getPlayer();
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            EquipmentView.query(player, entry.type(), entry.stack(), () -> {
                blockz$renderArm(player, entry.type(), event);
                return null;
            });
        }
    }

    @Inject(method = "skinArmorStacks", at = @At("RETURN"), cancellable = true, require = 0)
    private static void blockz$skinCoverage(LivingEntity entity, CallbackInfoReturnable<java.util.List<net.minecraft.world.item.ItemStack>> callback) {
        if (!(entity instanceof Player player) || EquipmentView.active(entity)) return;
        var stacks = new java.util.ArrayList<>(callback.getReturnValue());
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) stacks.add(entry.stack());
        callback.setReturnValue(stacks);
    }
}
