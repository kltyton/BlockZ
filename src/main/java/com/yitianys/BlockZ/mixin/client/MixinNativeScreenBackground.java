package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.gui.DayZContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinNativeScreenBackground {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void blockz$skipNestedBackground(GuiGraphics graphics, CallbackInfo ci) {
        if (DayZContainerScreen.isRenderingNative(this)) ci.cancel();
    }
}
