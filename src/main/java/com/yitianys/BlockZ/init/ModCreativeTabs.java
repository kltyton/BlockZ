package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlockZ.MODID);

    // 捷克挂包图标作为 Tab 图标
    public static final RegistryObject<CreativeModeTab> BLOCKZ_TAB = CREATIVE_MODE_TABS.register("blockz_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.BACKPACK_CZECHPOUCH.get()))
            .title(Component.translatable("creativetab.blockz_tab"))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.BACKPACK_COYOTE.get());
                output.accept(ModItems.BACKPACK_ALICE.get());
                output.accept(ModItems.BACKPACK_CZECH.get());
                output.accept(ModItems.BACKPACK_CZECHPOUCH.get());
                output.accept(ModItems.BACKPACK_PATROLPACK.get());
                output.accept(ModItems.DAYZ_ZOMBIE_SPAWN_EGG.get());
            })
            .build());

    public static final RegistryObject<CreativeModeTab> NURSING_TAB = CREATIVE_MODE_TABS.register("nursing_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.BANDAGE.get()))
            .title(Component.translatable("creativetab.blockz_nursing_tab"))
            .displayItems((parameters, output) -> {})
            .build());
}
