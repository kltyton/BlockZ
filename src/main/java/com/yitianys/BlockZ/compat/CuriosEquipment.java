package com.yitianys.BlockZ.compat;

import com.google.common.collect.LinkedHashMultimap;
import com.yitianys.BlockZ.equipment.EquipmentAttributes;
import com.yitianys.BlockZ.equipment.EquipmentRules;
import com.yitianys.BlockZ.entity.CorpseEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public final class CuriosEquipment {
    private record CorpseDrops(CorpseEntity corpse, Map<ItemStack, Integer> destinations) { }
    private static final Map<Player, CorpseDrops> CORPSE_DROPS = new WeakHashMap<>();

    private CuriosEquipment() { }

    public static void collectCorpseDrops(Player player, CorpseEntity corpse) {
        CORPSE_DROPS.put(player, new CorpseDrops(corpse, new IdentityHashMap<>()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropRules(DropRulesEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        CorpseDrops pending = CORPSE_DROPS.get(player);
        if (pending == null) return;
        event.getCurioHandler().getCurios().forEach((id, group) -> {
            recordDrops(event, pending, id, group.getStacks(), false, group.getRenders());
            recordDrops(event, pending, id, group.getCosmeticStacks(), true, group.getRenders());
        });
    }

    private static void recordDrops(DropRulesEvent event, CorpseDrops pending, String id,
                                    IDynamicStackHandler stacks, boolean cosmetic, java.util.List<Boolean> renders) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            DropRule rule = null;
            for (var override : event.getOverrides()) if (override.getA().test(stack)) rule = override.getB();
            if (rule == null) {
                SlotContext context = new SlotContext(id, event.getEntity(), i, cosmetic, i < renders.size() && renders.get(i));
                rule = CuriosApi.getCurio(stack).map(curio -> curio.getDropRule(context, event.getSource(),
                        event.getLootingLevel(), event.isRecentlyHit())).orElse(DropRule.DEFAULT);
            }
            if (rule == DropRule.DEFAULT) rule = CuriosApi.getSlot(id, event.getEntity().level())
                    .map(type -> type.getDropRule()).orElse(DropRule.DEFAULT);
            if (rule != DropRule.DEFAULT) continue;
            int destination = !cosmetic && i == 0 ? switch (id) {
                case "back" -> 0;
                case "body" -> 1;
                case "head" -> 7;
                default -> -1;
            } : -1;
            pending.destinations().put(stack, destination);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void corpseDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        CorpseDrops pending = CORPSE_DROPS.remove(player);
        if (pending == null || event.isCanceled()) return;
        event.getDrops().removeIf(drop -> {
            Integer destination = pending.destinations().get(drop.getItem());
            if (destination == null) return false;
            if (destination < 0 || !pending.corpse().getItem(destination).isEmpty()) {
                destination = 49;
                while (destination < pending.corpse().getContainerSize() && !pending.corpse().getItem(destination).isEmpty()) destination++;
            }
            if (destination >= pending.corpse().getContainerSize()) return false;
            pending.corpse().setItem(destination, drop.getItem());
            return true;
        });
    }
    public static void register() {
        CuriosApi.registerCurioPredicate(new ResourceLocation("blockz", "equipment"), result -> {
            var stack = result.stack();
            var wearer = result.slotContext().entity();
            if (stack.isEmpty() || stack.getItem() instanceof com.yitianys.BlockZ.item.ClothingItem) return false;
            int meshSlot = MeshEquipment.specialSlot(stack);
            if (meshSlot != 0) return result.slotContext().identifier().equals(switch (meshSlot) {
                case 1 -> "head";
                case 2 -> "body";
                case 3 -> "back";
                default -> "";
            });
            return switch (result.slotContext().identifier()) {
                case "head" -> stack.is(EquipmentRules.HELMETS) || EquipmentRules.armorType(stack) == EquipmentSlot.HEAD || wearer != null && stack.canEquip(EquipmentSlot.HEAD, wearer);
                case "body" -> stack.is(EquipmentRules.VESTS) || EquipmentRules.armorType(stack) == EquipmentSlot.CHEST || wearer != null && stack.canEquip(EquipmentSlot.CHEST, wearer);
                case "back" -> stack.is(EquipmentRules.BACKPACKS);
                default -> false;
            };
        });
        MinecraftForge.EVENT_BUS.register(new CuriosEquipment());
    }

    @SubscribeEvent
    public void changed(top.theillusivec4.curios.api.event.CurioChangeEvent event) {
        if (event.getEntity() instanceof Player player) MeshEquipment.changed(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void attributes(CurioAttributeModifierEvent event) {
        if (!(event.getSlotContext().entity() instanceof Player player)) return;
        EquipmentSlot type = EquipmentRules.armorType(player, event.getItemStack());
        if (type == null) return;
        var original = LinkedHashMultimap.create(event.getModifiers());
        var combined = LinkedHashMultimap.create(original);
        event.getItemStack().getAttributeModifiers(type).forEach((attribute, modifier) -> {
            if (original.get(attribute).stream().noneMatch(existing -> existing.getId().equals(modifier.getId()))) combined.put(attribute, modifier);
        });
        String id = "curios/" + event.getSlotContext().identifier() + "/" + event.getSlotContext().index();
        event.clearModifiers();
        EquipmentAttributes.remap(id, combined).forEach(event::addModifier);
    }
}
