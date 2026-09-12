package com.yitianys.BlockZ.equipment;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class EquipmentAttributes {
    private EquipmentAttributes() { }

    public static UUID modifierId(String entry, Attribute attribute, UUID original) {
        String key = "blockz/equipment/" + entry + "/" + ForgeRegistries.ATTRIBUTES.getKey(attribute) + "/" + original;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public static Multimap<Attribute, AttributeModifier> forStack(String entry, ItemStack stack, EquipmentSlot type) {
        return remap(entry, stack.getAttributeModifiers(type));
    }

    public static Multimap<Attribute, AttributeModifier> remap(String entry, Multimap<Attribute, AttributeModifier> source) {
        Multimap<Attribute, AttributeModifier> result = LinkedHashMultimap.create();
        source.forEach((attribute, modifier) -> result.put(attribute, new AttributeModifier(
                modifierId(entry, attribute, modifier.getId()), "BlockZ " + entry + ": " + modifier.getName(),
                modifier.getAmount(), modifier.getOperation())));
        return result;
    }
}
