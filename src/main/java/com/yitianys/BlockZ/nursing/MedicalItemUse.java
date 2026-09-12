package com.yitianys.BlockZ.nursing;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.MedicalMappings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BlockZ.MODID)
public final class MedicalItemUse {
    private record Session(ItemStack stack, ItemStack snapshot, InteractionHand hand,
                           int selectedSlot, Level level, MedicalTreatment treatment) {
        boolean matches(ServerPlayer player, ItemStack current) {
            return player.isAlive() && player.level() == level && player.getUsedItemHand() == hand
                    && player.getInventory().selected == selectedSlot && player.getItemInHand(hand) == stack
                    && current == stack && ItemStack.matches(snapshot, current)
                    && MedicalMappings.find(current) == treatment;
        }
    }

    private static final Map<ServerPlayer, Session> SESSIONS = new IdentityHashMap<>();

    @SubscribeEvent
    public static void start(LivingEntityUseItemEvent.Start event) {
        var treatment = MedicalMappings.find(event.getItem());
        if (treatment == null) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)
                || !treatment.canUse(player, true)) {
            event.setCanceled(true);
            return;
        }
        event.setDuration(treatment.duration());
        if (player instanceof ServerPlayer serverPlayer) {
            var hand = player.getMainHandItem() == event.getItem() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            SESSIONS.put(serverPlayer, new Session(event.getItem(), event.getItem().copy(), hand,
                    player.getInventory().selected, player.level(), treatment));
        }
    }

    @SubscribeEvent
    public static void tick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player);
        if (session != null && (!session.matches(player, event.getItem()) || !session.treatment.canComplete(player, false))) {
            player.stopUsingItem();
            event.setCanceled(true);
        }
    }

    public static boolean handles(LivingEntity entity, ItemStack stack) {
        if (entity instanceof ServerPlayer player) {
            Session session = SESSIONS.get(player);
            if (session != null && session.stack == stack) return true;
        }
        return MedicalMappings.find(stack) != null;
    }

    public static ItemStack finish(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        Session session = SESSIONS.remove(player);
        if (session != null && player.isUsingItem() && player.getUseItemRemainingTicks() <= 0
                && session.matches(player, stack) && session.treatment.canComplete(player, true)) {
            session.treatment.apply(player);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    public static void stop(LivingEntity entity, ItemStack stack, int remaining) {
        boolean mapped = handles(entity, stack);
        if (entity instanceof ServerPlayer player) SESSIONS.remove(player);
        if (!mapped) stack.onStopUsing(entity, remaining);
    }

    private static void cancel(ServerPlayer player) {
        if (SESSIONS.containsKey(player)) player.stopUsingItem();
        SESSIONS.remove(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) { SESSIONS.clear(); }

    private MedicalItemUse() { }
}
