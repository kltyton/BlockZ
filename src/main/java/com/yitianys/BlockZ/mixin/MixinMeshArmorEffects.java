package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.equipment.EquipmentSlots;
import com.yitianys.BlockZ.equipment.EquipmentView;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/** Adapts the local MeshArmoury resolver, including its full-armor virtual effects. */
@Pseudo
@Mixin(targets = "com.kltyton.mesharmoury.gameplay.armor.ArmorEffectResolver", remap = false)
public abstract class MixinMeshArmorEffects {
    @Invoker("sourcesForSlot")
    public static List<?> blockz$sourcesForSlot(LivingEntity entity, EquipmentSlot slot) { throw new AssertionError(); }

    @Inject(method = "sourcesForSlot", at = @At("RETURN"), cancellable = true)
    private static void blockz$extraSources(LivingEntity entity, EquipmentSlot slot, CallbackInfoReturnable<List<?>> cir) {
        if (!(entity instanceof Player player) || EquipmentView.active(entity)) return;
        List<Object> sources = new ArrayList<>(cir.getReturnValue());
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            sources.addAll(EquipmentView.query(player, entry.type(), entry.stack(), () -> blockz$sourcesForSlot(entity, slot)));
        }
        cir.setReturnValue(sources);
    }
}
