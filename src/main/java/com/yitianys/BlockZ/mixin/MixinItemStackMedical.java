package com.yitianys.BlockZ.mixin;

import com.yitianys.BlockZ.config.MedicalMappings;
import com.yitianys.BlockZ.nursing.MedicalItemUse;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStackMedical {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void blockz$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        var treatment = MedicalMappings.find(stack);
        if (treatment == null) return;
        if (!treatment.canUse(player, true)) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
            return;
        }
        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.consume(stack));
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void blockz$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (MedicalMappings.find((ItemStack) (Object) this) != null) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void blockz$interact(Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (MedicalMappings.find((ItemStack) (Object) this) != null) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void blockz$duration(CallbackInfoReturnable<Integer> cir) {
        var treatment = MedicalMappings.find((ItemStack) (Object) this);
        if (treatment != null) cir.setReturnValue(treatment.duration());
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void blockz$animation(CallbackInfoReturnable<UseAnim> cir) {
        var treatment = MedicalMappings.find((ItemStack) (Object) this);
        if (treatment != null) cir.setReturnValue(treatment.animation());
    }

    @Inject(method = "useOnRelease", at = @At("HEAD"), cancellable = true)
    private void blockz$useOnRelease(CallbackInfoReturnable<Boolean> cir) {
        if (MedicalMappings.find((ItemStack) (Object) this) != null) cir.setReturnValue(false);
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void blockz$tick(Level level, LivingEntity entity, int remaining, CallbackInfo ci) {
        if (MedicalItemUse.handles(entity, (ItemStack) (Object) this)) ci.cancel();
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void blockz$release(Level level, LivingEntity entity, int remaining, CallbackInfo ci) {
        if (MedicalItemUse.handles(entity, (ItemStack) (Object) this)) ci.cancel();
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void blockz$finish(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (MedicalItemUse.handles(entity, stack)) cir.setReturnValue(MedicalItemUse.finish(entity, stack));
    }
}
