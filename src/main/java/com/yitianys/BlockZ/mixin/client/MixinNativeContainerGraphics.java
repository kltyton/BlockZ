package com.yitianys.BlockZ.mixin.client;

import com.yitianys.BlockZ.client.gui.DayZContainerScreen;
import com.yitianys.BlockZ.client.gui.InventoryTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphics.class)
public abstract class MixinNativeContainerGraphics {
    @Inject(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIIIIFFII)V", at = @At("HEAD"), cancellable = true)
    private void blockz$skipContainerBackdrop(ResourceLocation texture, int x1, int x2, int y1, int y2, int depth,
                                            int sourceWidth, int sourceHeight, float u, float v, int textureWidth, int textureHeight, CallbackInfo ci) {
        if (DayZContainerScreen.shouldSkipBackground(texture, x2 - x1, y2 - y1, u, v)) ci.cancel();
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int blockz$nativeTextColor(int color) {
        if (DayZContainerScreen.isNativeRenderActive() && (color & 0xFFFFFF) == 0x404040) return InventoryTheme.TEXT;
        return color;
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
            at = @At("HEAD"), cancellable = true)
    private void blockz$skipOldInventoryLabel(Font font, Component text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (DayZContainerScreen.shouldSkipLabel(text)) cir.setReturnValue(0);
    }
    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void blockz$nativeScissor(int x1, int y1, int x2, int y2, CallbackInfo ci) {
        if (DayZContainerScreen.applyNativeScissor((GuiGraphics) (Object) this, x1, y1, x2, y2)) ci.cancel();
    }
}
