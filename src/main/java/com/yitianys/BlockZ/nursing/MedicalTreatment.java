package com.yitianys.BlockZ.nursing;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.effect.FractureEffect;
import com.yitianys.BlockZ.init.ModEffects;
import com.yitianys.BlockZ.util.PlayerMessageUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;

public enum MedicalTreatment {
    BANDAGE(24, UseAnim.BOW),
    RAGS(32, UseAnim.BOW),
    SPLINT(32, UseAnim.BOW),
    MORPHINE(24, UseAnim.BOW),
    CODEINE(20, UseAnim.EAT);

    private static final int FRACTURE_RECOVERY_TICKS = 20 * 60 * 5;
    private final int duration;
    private final UseAnim animation;

    MedicalTreatment(int duration, UseAnim animation) {
        this.duration = duration;
        this.animation = animation;
    }

    public int duration() { return duration; }
    public UseAnim animation() { return animation; }

    public boolean canUse(Player player, boolean notify) {
        return canUse(player, notify, true);
    }

    public boolean canComplete(Player player, boolean notify) {
        return canUse(player, notify, false);
    }

    private boolean canUse(Player player, boolean notify, boolean starting) {
        if (!BlockZConfigs.isNursingEnabled()) return false;
        String failure = null;
        switch (this) {
            case BANDAGE, RAGS -> {
                if (!BlockZConfigs.isBleedingEnabled()) return false;
                if (!player.hasEffect(ModEffects.BLEEDING.get())) {
                    failure = this == BANDAGE ? "bandage_no_wound" : "rags_no_wound";
                }
            }
            case SPLINT -> {
                if (!BlockZConfigs.isBrokenLegsEnabled()) return false;
                var fracture = player.getEffect(ModEffects.FRACTURE.get());
                if (fracture == null) failure = "splint_no_fracture";
                else if (starting && fracture.getDuration() <= FRACTURE_RECOVERY_TICKS) {
                    failure = "splint_already_applied";
                }
            }
            case MORPHINE, CODEINE -> { }
        }
        if (failure == null) return true;
        if (notify && !player.level().isClientSide) {
            PlayerMessageUtils.sendActionbarWithCooldown(player, Component.translatable("msg.blockz." + failure), "blockz_" + failure, 60);
        }
        return false;
    }

    public void apply(Player player) {
        switch (this) {
            case BANDAGE -> {
                player.removeEffect(ModEffects.BLEEDING.get());
                message(player, "bandage_applied");
            }
            case RAGS -> {
                boolean success = player.getRandom().nextFloat() < 0.65F;
                if (success) player.removeEffect(ModEffects.BLEEDING.get());
                message(player, success ? "rags_applied" : "rags_failed");
            }
            case SPLINT -> {
                player.getPersistentData().putBoolean(FractureEffect.SUPPRESS_FRACTURE_RECOVERED_MESSAGE_TAG, true);
                player.removeEffect(ModEffects.FRACTURE.get());
                player.addEffect(new MobEffectInstance(ModEffects.FRACTURE.get(), FRACTURE_RECOVERY_TICKS, 0, false, false, true));
                message(player, "splint_applied");
            }
            case MORPHINE -> {
                player.addEffect(new MobEffectInstance(ModEffects.ANALGESIC.get(), 4800, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 900, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 1, false, false, true));
            }
            case CODEINE -> {
                player.addEffect(new MobEffectInstance(ModEffects.ANALGESIC.get(), 2400, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false, true));
            }
        }
    }

    private static void message(Player player, String key) {
        PlayerMessageUtils.sendActionbar(player, Component.translatable("msg.blockz." + key));
    }
}
