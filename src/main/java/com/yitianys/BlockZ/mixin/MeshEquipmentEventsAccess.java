package com.yitianys.BlockZ.mixin;

import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Calls only the local Mesh speed recalculation without publishing a synthetic Forge event. */
@Pseudo
@Mixin(targets = "com.kltyton.mesharmoury.event.DamageEventHandler", remap = false)
public interface MeshEquipmentEventsAccess {
    @Invoker("onEquipmentChange")
    static void blockz$updateSpeed(LivingEquipmentChangeEvent event) { throw new AssertionError(); }
}
