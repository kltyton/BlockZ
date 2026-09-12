package com.yitianys.BlockZ.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class EquipmentConfig {
    public static final int MAX_ARMOR_SLOTS = 4;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue INITIAL_ARMOR_SLOTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNLOCK_ITEMS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        INITIAL_ARMOR_SLOTS = builder.comment("Initially available general armor slots. Each accepts any armor type, including duplicates.")
                .defineInRange("initial_armor_slots", 1, 1, MAX_ARMOR_SLOTS);
        UNLOCK_ITEMS = builder.comment("Optional item IDs that unlock one additional armor slot when used. Empty by default; maximum four slots.")
                .defineListAllowEmpty("unlock_items", List.of(), value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        SPEC = builder.build();
    }

    private EquipmentConfig() { }
}
