package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.entity.CorpseEntity;
import com.yitianys.BlockZ.entity.ZombieCorpseEntity;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.RotateItemC2S;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.List;

public class DayZInventoryScreen extends PanelInventoryScreen<DayZInventoryMenu> {
    private static final int STORAGE_WIDTH = 170;
    private static final int EQUIPMENT_WIDTH = 80;

    public DayZInventoryScreen(DayZInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected int requiredHeight() { return Math.max(384, 90 + (1 + menu.getStorageSections().size()) * 48); }

    @Override protected void beforeLayout() { menu.updateSlotPositions(); }

    @Override
    protected Slot resolvePickupSlot(Slot slot) {
        int anchor = menu.getGridAnchorMenuSlotIndex(slot.index);
        return anchor >= 0 && anchor < menu.slots.size() ? menu.getSlot(anchor) : slot;
    }

    @Override
    protected void buildPanels() {
        int x = layoutLeft();
        int middle = x + InventoryTheme.PLAYER_WIDTH + InventoryTheme.GAP;
        int right = middle + 176;
        int top = 58;
        InventoryPanel hotbar = panel("hotbar", label("hotbar"), x + (requiredWidth() - 250) / 2, 3, 242, 34, false);
        for (int i = 0; i < 9; i++) {
            hotbar.cells.add(new InventoryPanel.Cell(menu.getSlot(menu.getHotbarStart() + i), i * 26, 0, 26, 26, false));
        }
        hotbar.contentHeight = 26;
        panel("character", minecraft.player.getDisplayName(), x, top, 112, contentBottom() - top - 90, false);
        buildEquipment(x + 118, top);
        InventoryPanel sling = panel("sling", label("sling"), x, contentBottom() - 72, InventoryTheme.PLAYER_WIDTH, 54, false);
        int selected = minecraft.player.getInventory().selected;
        sling.cells.add(new InventoryPanel.Cell(menu.getSlot(menu.getHotbarStart() + selected), 0, 0, InventoryTheme.PLAYER_WIDTH - 8, 46, true));
        sling.contentHeight = 46;

        List<DayZInventoryMenu.StorageSection> storage = menu.getStorageSections();
        InventoryPanel pockets = panel("pockets", capacity(label("pockets"), menu.getPocketStart(), menu.getPocketCount()),
                middle, top, STORAGE_WIDTH, Math.max(26, rows(menu.getPocketCount(), 9) * 18 + 8), true);
        addGrid(pockets, menu.getPocketStart(), menu.getPocketCount(), 9, 0);
        int y = top + pockets.height() + 6;
        int remainingSections = storage.size();
        int remainingBody = Math.max(remainingSections * 26, contentBottom() - y - remainingSections * (InventoryTheme.HEADER + 6));
        for (int index : new int[]{8, 0, 1, 2, 3, 6, 5}) {
            for (DayZInventoryMenu.StorageSection section : storage) {
                if (section.equipmentIndex() != index) continue;
                String id = index == 5 ? "backpack_storage" : index == 6 ? "rig_storage" : "armor_storage_" + index;
                int slotStart = menu.getBackpackSlotStart() + section.offset();
                Component name = section.stack().getHoverName();
                int body = Math.min(rows(section.capacity(), section.columns()) * 18 + 8,
                        Math.max(26, remainingBody / remainingSections));
                remainingBody -= body;
                remainingSections--;
                InventoryPanel panel = panel(id, capacity(name, slotStart, section.capacity()), middle, y, STORAGE_WIDTH,
                        body, true);
                addGrid(panel, slotStart, section.capacity(), section.columns(), 0);
                y += panel.height() + 6;
            }
        }
        if (menu.getActiveContainer() != null || menu.isWorkbench() || menu.isEnchantingTable) buildVicinity(right, top);
        if (!menu.isWorkbench() && menu.getCraftingInputEnd() <= menu.slots.size()) {
            InventoryPanel crafting = panel("crafting", label("crafting"), right, contentBottom() - 76, STORAGE_WIDTH, 54, true);
            addGrid(crafting, menu.getCraftingInputStart(), 4, 2, 0);
            crafting.add(menu.getSlot(menu.getCraftingResultSlot()), 76, 9);
        }
    }

    private void buildEquipment(int x, int y) {
        int base = menu.getEquipmentStart();
        InventoryPanel face = panel("helmet", label("helmet"), x, y, EQUIPMENT_WIDTH, 30, false);
        face.equipmentIcon = 0;
        face.cells.add(new InventoryPanel.Cell(menu.getSlot(base + 8), 25, 0, 22, 22, false));
        y += face.height() + 2;
        InventoryPanel armor = panel("armor", label("armor"), x, y, EQUIPMENT_WIDTH, 26, false);
        armor.equipmentIcon = 1;
        int armorCount = com.yitianys.BlockZ.equipment.EquipmentSlots.visibleArmorSlots(minecraft.player);
        for (int i = 0; i < armorCount; i++) armor.add(menu.getSlot(base + i), (72 - armorCount * 18) / 2 + i * 18, 0);
        y += armor.height() + 2;
        InventoryPanel backpack = panel("backpack_equipment", label("backpack"), x, y, EQUIPMENT_WIDTH, 30, false);
        backpack.equipmentIcon = 2;
        backpack.cells.add(new InventoryPanel.Cell(menu.getSlot(base + 5), 25, 0, 22, 22, false));
        y += backpack.height() + 2;
        InventoryPanel rig = panel("rig", label("rig"), x, y, EQUIPMENT_WIDTH, 30, false);
        rig.equipmentIcon = 3;
        rig.cells.add(new InventoryPanel.Cell(menu.getSlot(base + 6), 25, 0, 22, 22, false));
        y += rig.height() + 2;
        InventoryPanel accessories = panel("accessories", label("accessories"), x, y, EQUIPMENT_WIDTH,
                26, false);
        boolean hasLegacyGloves = menu.getSlot(base + 7).hasItem();
        int handStart = (72 - (hasLegacyGloves ? 36 : 18)) / 2;
        accessories.add(menu.getSlot(base + 4), handStart, 0);
        if (hasLegacyGloves) accessories.add(menu.getSlot(base + 7), handStart + 18, 0);
        int extraCount = menu.getAdditionalEquipmentSlotCount();
        for (int i = 0; i < extraCount; i++) {
            int rowCount = Math.min(3, extraCount - i / 3 * 3);
            accessories.add(menu.getSlot(menu.getAdditionalEquipmentSlotStart() + i), (72 - rowCount * 18) / 2 + i % 3 * 18, 18 + i / 3 * 18);
        }
    }

    private void buildVicinity(int x, int y) {
        int naturalHeight = 194;
        if (menu.isWorkbench()) naturalHeight = 74;
        else if (menu.isEnchantingTable) naturalHeight = 108;
        else if (!(menu.getActiveContainer() instanceof CorpseEntity) && !(menu.getActiveContainer() instanceof ZombieCorpseEntity)) {
            Container container = menu.getActiveContainer();
            int count = container == null ? 0 : Math.min(81, Math.max(0, container.getContainerSize() - menu.getContainerPage() * 81));
            int bottom = 0;
            for (int i = 0; i < count; i++) {
                Slot slot = menu.getSlot(i);
                if (slot.isActive() && slot.y > -500) bottom = Math.max(bottom, slot.y - UIConstants.VICINITY_SLOTS_Y + 18);
            }
            naturalHeight = Math.max(26, bottom + InventoryTheme.PAD * 2);
        }
        InventoryPanel vicinity = panel("vicinity", vicinityTitle(), x, y, STORAGE_WIDTH,
                Math.min(naturalHeight, contentBottom() - y - 108), false);
        if (menu.isWorkbench()) {
            addGrid(vicinity, 0, 9, 3, 12);
            vicinity.add(menu.getSlot(9), 112, 30);
        } else if (menu.isEnchantingTable) {
            vicinity.add(menu.getSlot(0), 36, 6);
            vicinity.add(menu.getSlot(1), 90, 6);
            vicinity.contentHeight = 100;
        } else if (menu.getActiveContainer() instanceof CorpseEntity || menu.getActiveContainer() instanceof ZombieCorpseEntity) {
            int sourceX = UIConstants.VICINITY_SLOTS_X + menu.getVicinityOffsetX();
            addCanonical(vicinity, 0, 81, sourceX, UIConstants.VICINITY_SLOTS_Y, 0);
            int corpseStart = menu.getCorpseStorageSlotStart();
            if (corpseStart >= 0) {
                addCanonical(vicinity, corpseStart, menu.getCorpseStorageSlotEnd() - corpseStart + 1,
                        sourceX, UIConstants.VICINITY_SLOTS_Y, 0);
            }
        } else {
            Container container = menu.getActiveContainer();
            int count = container == null ? 0 : Math.min(81, Math.max(0, container.getContainerSize() - menu.getContainerPage() * 81));
            addCanonical(vicinity, 0, count, UIConstants.VICINITY_SLOTS_X + menu.getVicinityOffsetX(), UIConstants.VICINITY_SLOTS_Y, 0);
        }
    }

    private Component vicinityTitle() {
        Container container = menu.getActiveContainer();
        Component name = label("vicinity");
        if (container instanceof CorpseEntity corpse) {
            String owner = corpse.getOwnerName();
            name = owner == null || owner.isBlank() ? label("corpse") : Component.literal(owner);
        } else if (container instanceof ZombieCorpseEntity corpse) {
            name = corpse.getDisplayName();
        } else if (container instanceof Nameable named) {
            name = named.getDisplayName();
        } else if (container != null || menu.isWorkbench() || menu.isEnchantingTable) {
            name = title;
        }
        if (menu.supportsContainerPaging()) name = name.copy().append(" " + (menu.getContainerPage() + 1) + "/" + menu.getContainerPageCount());
        return name;
    }





    private Component capacity(Component name, int start, int count) {
        int occupied = 0;
        for (int i = start; i < start + count && i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            if (slot.hasItem()) {
                ItemSizeManager.ItemSize size = ItemSizeManager.getSize(slot.getItem());
                occupied += slot instanceof com.yitianys.BlockZ.menu.slot.TetrisSlot ? size.width() * size.height() : 1;
            }
        }
        return name.copy().append(" (" + occupied + "/" + count + ")");
    }

    private void addCanonical(InventoryPanel panel, int start, int count, int baseX, int baseY, int offsetY) {
        for (int i = start; i < start + count && i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            if (slot.x > -500 && slot.y > -500 && slot.isActive()) panel.add(slot, slot.x - baseX, slot.y - baseY + offsetY);
        }
    }

    private void addGrid(InventoryPanel panel, int start, int count, int cols, int offsetY) {
        for (int i = 0; i < count && start + i < menu.slots.size(); i++) panel.add(menu.getSlot(start + i), i % cols * 18, offsetY + i / cols * 18);
    }

    private static int rows(int count, int cols) { return (count + cols - 1) / cols; }
    private static Component label(String key) { return Component.translatable("screen.blockz.panel." + key); }

    @Override
    protected void renderPanelContent(GuiGraphics graphics, InventoryPanel panel, int mouseX, int mouseY, float partialTick) {
        if (panel.id.equals("character")) renderCharacter(graphics, panel, mouseX, mouseY);
        if (panel.id.equals("crafting")) graphics.drawString(font, ">", panel.contentX() + 56, panel.contentY() + 14, InventoryTheme.TEXT, false);
        if (panel.id.equals("vicinity") && !menu.isWorkbench() && !menu.isEnchantingTable
                && !(menu.getActiveContainer() instanceof CorpseEntity) && !(menu.getActiveContainer() instanceof ZombieCorpseEntity)) {
            for (int row = 0; row < Math.max(3, panel.contentHeight / 18); row++) {
                for (int col = 0; col < 9; col++) InventoryTheme.slot(graphics, panel.contentX() + col * 18,
                        panel.contentY() + row * 18 - panel.scroll, 18, false);
            }
        }
        if (panel.id.equals("vicinity") && menu.isEnchantingTable) renderEnchantments(graphics, panel, mouseX, mouseY);
    }

    private void renderEnchantments(GuiGraphics graphics, InventoryPanel panel, int mouseX, int mouseY) {
        for (int i = 0; i < 3; i++) {
            int x = panel.contentX() + 4;
            int y = panel.contentY() + 34 + i * 20 - panel.scroll;
            int cost = menu.costs[i];
            boolean hovered = topPanel(mouseX, mouseY) == panel && mouseX >= x && mouseX < x + 150 && mouseY >= y && mouseY < y + 18;
            graphics.fill(x, y, x + 150, y + 18, hovered ? 0xFF41483B : 0xD0181C17);
            graphics.renderOutline(x, y, 150, 18, 0xFF66705F);
            graphics.drawString(font, cost > 0 ? Component.translatable("screen.blockz.enchant_cost", cost) : Component.literal("—"),
                    x + 5, y + 5, cost > 0 ? InventoryTheme.SELECTED : InventoryTheme.MUTED, false);
            if (hovered && cost > 0 && menu.enchantClue[i] >= 0) {
                Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.byId(menu.enchantClue[i]);
                if (enchantment != null) graphics.renderTooltip(font, enchantment.getFullname(menu.levelClue[i]), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderPanelHeader(GuiGraphics graphics, InventoryPanel panel) {
        if (panel.id.equals("vicinity") && menu.supportsContainerPaging()) {
            graphics.fill(panel.x + panel.width - 44, panel.y + 1, panel.x + panel.width - 15,
                    panel.y + InventoryTheme.HEADER - 1, 0xFF20241F);
            graphics.drawString(font, "<", panel.x + panel.width - 40, panel.y + 4, InventoryTheme.TEXT, false);
            graphics.drawString(font, ">", panel.x + panel.width - 26, panel.y + 4, InventoryTheme.TEXT, false);
        }
    }

    @Override
    protected boolean panelHeaderClicked(InventoryPanel panel, double x, double y, int button) {
        if (!panel.id.equals("vicinity") || !menu.supportsContainerPaging() || button != 0 || !panel.headerContains(x, y)) return false;
        int relative = (int) x - panel.x;
        if (relative < panel.width - 44 || relative >= panel.width - 15) return false;
        boolean next = relative >= panel.width - 29;
        menu.setContainerPage(menu.getContainerPage() + (next ? 1 : -1));
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, next ? 101 : 100);
        panel.scroll = 0;
        refreshLayout();
        return true;
    }

    @Override
    protected boolean panelClicked(InventoryPanel panel, double x, double y, int button) {
        if (!panel.id.equals("vicinity") || !menu.isEnchantingTable || button != 0) return false;
        int row = ((int) y - panel.contentY() - 34 + panel.scroll) / 20;
        int firstY = panel.contentY() + 34 - panel.scroll;
        if (x >= panel.contentX() + 4 && x < panel.contentX() + 154 && y >= firstY && row >= 0 && row < 3 && menu.costs[row] > 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, row);
            return true;
        }
        return false;
    }

    @Override
    protected void renderTargetFeedback(GuiGraphics graphics, InventoryPanel panel, InventoryPanel.Cell cell) {
        if (cell.alias()) {
            super.renderTargetFeedback(graphics, panel, cell);
            return;
        }
        ItemStack carried = menu.getCarried();
        int slotIndex = carried.isEmpty() ? menu.getGridAnchorMenuSlotIndex(cell.slot().index)
                : menu.getCenteredPreviewAnchorMenuSlotIndex(cell.slot().index, carried);
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            super.renderTargetFeedback(graphics, panel, cell);
            return;
        }
        Slot anchor = menu.getSlot(slotIndex);
        ItemStack stack = carried.isEmpty() ? anchor.getItem() : carried;
        if (stack.isEmpty()) return;
        ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
        int color = carried.isEmpty() || anchor.mayPlace(carried) ? 0xB0D0D7AA : 0xB0DC6655;
        graphics.renderOutline(anchor.x - 1, anchor.y - 1, size.width() * 18, size.height() * 18, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.ROTATE_ITEM != null && ModKeyMappings.ROTATE_ITEM.matches(keyCode, scanCode) && !menu.getCarried().isEmpty()) {
            ItemSizeManager.toggleRotation(menu.getCarried());
            NetworkHandler.CHANNEL.sendToServer(new RotateItemC2S());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
