package com.yitianys.BlockZ.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import java.util.ArrayList;
import java.util.List;

public final class InventoryPanel {
    public record Cell(Slot slot, int x, int y, int width, int height, boolean alias) { }

    public final String id;
    public Component title = Component.empty();
    public final List<Cell> cells = new ArrayList<>();
    public int x;
    public int y;
    public int width;
    public int bodyHeight;
    public int contentHeight;
    public int scroll;
    public boolean collapsed;
    public boolean metal;
    public boolean visible;
    public boolean positioned;
    public boolean userPositioned;
    public int equipmentIcon = -1;

    public InventoryPanel(String id) {
        this.id = id;
    }

    public int height() {
        return InventoryTheme.HEADER + (collapsed ? 0 : bodyHeight);
    }

    public int contentX() {
        return x + InventoryTheme.PAD;
    }

    public int contentY() {
        return y + InventoryTheme.HEADER + InventoryTheme.PAD;
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - bodyHeight + InventoryTheme.PAD * 2);
    }

    public boolean contains(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height();
    }

    public boolean headerContains(double mouseX, double mouseY) {
        return contains(mouseX, mouseY) && mouseY < y + InventoryTheme.HEADER;
    }

    public boolean contentContains(double mouseX, double mouseY) {
        return !collapsed && contains(mouseX, mouseY) && mouseY >= y + InventoryTheme.HEADER;
    }

    public void add(Slot slot, int x, int y) {
        cells.add(new Cell(slot, x, y, InventoryTheme.CELL, InventoryTheme.CELL, false));
        contentHeight = Math.max(contentHeight, y + InventoryTheme.CELL);
    }
}
