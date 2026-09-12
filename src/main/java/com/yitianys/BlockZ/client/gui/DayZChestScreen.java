package com.yitianys.BlockZ.client.gui;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public class DayZChestScreen extends DayZContainerScreen<ChestMenu> {
    public DayZChestScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(new ContainerScreen(menu, inventory, title), inventory);
    }
}
