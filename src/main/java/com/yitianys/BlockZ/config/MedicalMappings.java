package com.yitianys.BlockZ.config;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.item.ClothingItem;
import com.yitianys.BlockZ.item.MedicalItem;
import com.yitianys.BlockZ.nursing.MedicalTreatment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MedicalMappings {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEMS;
    private static volatile Map<Item, MedicalTreatment> treatments = Map.of();

    static {
        var builder = new ForgeConfigSpec.Builder();
        ITEMS = builder.comment("External medical items: namespace:item=bandage|rags|splint|morphine|codeine.",
                "Each entry chooses one treatment, e.g. minecraft:paper=bandage. Default is empty.",
                "The mapped item uses BlockZ treatment instead of its original use callbacks.",
                "Matching is by registry ID, including all NBT variants. Stop the server before editing; restart to sync clients.")
                .defineListAllowEmpty("items", List.of(), value -> value instanceof String);
        SPEC = builder.build();
    }

    public static void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        if (event instanceof ModConfigEvent.Unloading) {
            treatments = Map.of();
            return;
        }
        Map<Item, MedicalTreatment> parsed = new HashMap<>();
        for (String entry : ITEMS.get()) {
            String[] pair = entry.split("=", -1);
            ResourceLocation id = pair.length == 2 ? ResourceLocation.tryParse(pair[0].trim()) : null;
            if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                BlockZ.LOGGER.warn("Ignoring invalid or unknown medical item mapping: {}", entry);
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR || item instanceof MedicalItem || item instanceof ClothingItem) {
                BlockZ.LOGGER.warn("Ignoring retired or empty medical item mapping: {}", entry);
                continue;
            }
            try {
                var treatment = MedicalTreatment.valueOf(pair[1].trim().toUpperCase(Locale.ROOT));
                if (parsed.putIfAbsent(item, treatment) != null) {
                    BlockZ.LOGGER.warn("Ignoring duplicate medical item mapping: {}", entry);
                }
            } catch (IllegalArgumentException invalidTreatment) {
                BlockZ.LOGGER.warn("Ignoring unknown medical treatment: {}", entry);
            }
        }
        treatments = Map.copyOf(parsed);
    }

    @Nullable
    public static MedicalTreatment find(ItemStack stack) {
        return stack.isEmpty() ? null : treatments.get(stack.getItem());
    }

    private MedicalMappings() { }
}
