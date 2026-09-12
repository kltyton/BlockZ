package com.yitianys.BlockZ.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotPositionAccess {
    @Mutable @Accessor("x") void blockz$setX(int x);
    @Mutable @Accessor("y") void blockz$setY(int y);
}
