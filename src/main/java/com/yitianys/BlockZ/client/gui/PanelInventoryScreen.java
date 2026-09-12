package com.yitianys.BlockZ.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.menu.slot.TetrisSlot;
import com.yitianys.BlockZ.mixin.client.ContainerScreenAccess;
import com.yitianys.BlockZ.mixin.client.SlotPositionAccess;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class PanelInventoryScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected final Map<String, InventoryPanel> panels = new LinkedHashMap<>();
    private final List<InventoryPanel> order = new ArrayList<>();
    private final Properties preferences = new Properties();
    private static final Path LAYOUT_FILE = FMLPaths.CONFIGDIR.get().resolve("blockz-inventory-layout.properties");
    protected float layoutScale = 1;
    protected int canvasWidth;
    protected int canvasHeight;
    @Nullable private InventoryPanel draggedPanel;
    @Nullable private InventoryPanel scrollingPanel;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean layoutChanged;
    private AbstractButton resetLayoutButton;
    @Nullable protected InventoryPanel.Cell hoveredCell;

    protected PanelInventoryScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        if (Files.isRegularFile(LAYOUT_FILE)) {
            try (Reader reader = Files.newBufferedReader(LAYOUT_FILE)) {
                preferences.load(reader);
            } catch (IOException invalidLayout) {
                BlockZ.LOGGER.warn("Unable to read inventory panel layout", invalidLayout);
            }
        }
    }

    protected int requiredWidth() { return InventoryTheme.PLAYER_WIDTH + InventoryTheme.STORAGE_WIDTH * 2 + InventoryTheme.GAP * 2 + 8; }
    protected abstract void buildPanels();
    protected void beforeLayout() { }
    protected Slot resolvePickupSlot(Slot slot) { return slot; }
    protected void renderPanelHeader(GuiGraphics graphics, InventoryPanel panel) { }
    protected boolean panelHeaderClicked(InventoryPanel panel, double x, double y, int button) { return false; }
    protected void renderPanelContent(GuiGraphics graphics, InventoryPanel panel, int mouseX, int mouseY, float partialTick) { }
    protected boolean panelClicked(InventoryPanel panel, double x, double y, int button) { return false; }
    protected boolean panelScrolled(InventoryPanel panel, double x, double y, double delta) { return false; }
    protected boolean finishPanelInput(double x, double y, int button) { return false; }
    protected boolean dragPanelInput(double x, double y, int button, double dx, double dy) { return false; }

    @Override
    protected void init() {
        super.init();
        layoutScale = Math.min(1f, Math.min((float) width / requiredWidth(), (float) height / requiredHeight()));
        canvasWidth = Mth.ceil(width / layoutScale);
        canvasHeight = Mth.ceil(height / layoutScale);
        leftPos = 0;
        topPos = 0;
        imageWidth = canvasWidth;
        imageHeight = canvasHeight;
        Component resetLabel = Component.translatable("screen.blockz.layout_reset");
        int buttonWidth = Math.max(76, font.width(resetLabel) + 16);
        resetLayoutButton = addRenderableWidget(new AbstractButton(canvasWidth - buttonWidth - 4, canvasHeight - 21,
                buttonWidth, 18, resetLabel) {
            @Override public void onPress() { resetLayout(); }
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fillGradient(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF343733, 0xFF131513);
                graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? InventoryTheme.SELECTED : InventoryTheme.MUTED);
                graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 5, InventoryTheme.TEXT);
            }
            @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        });
        refreshLayout();
    }

    protected int requiredHeight() { return 384; }

    protected final int layoutLeft() { return Math.max(4, (canvasWidth - requiredWidth()) / 2 + 4); }
    protected final int contentBottom() { return canvasHeight - 24; }

    private void resetLayout() {
        preferences.clear();
        panels.clear();
        order.clear();
        draggedPanel = null;
        scrollingPanel = null;
        layoutChanged = true;
        refreshLayout();
        saveLayout();
    }

    protected final InventoryPanel panel(String id, Component label, int x, int y, int width, int bodyHeight, boolean metal) {
        InventoryPanel panel = panels.computeIfAbsent(id, key -> {
            InventoryPanel created = new InventoryPanel(key);
            order.add(created);
            return created;
        });
        panel.title = label;
        panel.width = width;
        panel.bodyHeight = Math.max(0, bodyHeight);
        panel.metal = metal;
        panel.visible = true;
        if (!panel.positioned) {
            panel.collapsed = Boolean.parseBoolean(preferences.getProperty(id + ".folded", "false"));
            String savedX = preferences.getProperty(id + ".x");
            String savedY = preferences.getProperty(id + ".y");
            if (savedX != null && savedY != null) {
                try {
                    double px = Double.parseDouble(savedX);
                    double py = Double.parseDouble(savedY);
                    if (Double.isFinite(px) && Double.isFinite(py)) {
                        panel.x = (int) (Mth.clamp(px, 0, 1) * Math.max(0, canvasWidth - width));
                        panel.y = (int) (Mth.clamp(py, 0, 1) * Math.max(0, contentBottom() - panel.height()));
                        panel.userPositioned = true;
                    }
                } catch (NumberFormatException invalidPosition) {
                    BlockZ.LOGGER.warn("Ignoring invalid position for inventory panel {}", id);
                }
            }
            panel.positioned = true;
        }
        if (!panel.userPositioned) {
            panel.x = x;
            panel.y = y;
        }
        clampPanel(panel);
        return panel;
    }

    private void clampPanel(InventoryPanel panel) {
        panel.x = Mth.clamp(panel.x, 0, Math.max(0, canvasWidth - panel.width));
        panel.y = Mth.clamp(panel.y, 0, Math.max(0, contentBottom() - panel.height()));
    }

    protected final void refreshLayout() {
        beforeLayout();
        layoutScale = Math.min(1f, Math.min((float) width / requiredWidth(), (float) height / requiredHeight()));
        canvasWidth = Mth.ceil(width / layoutScale);
        canvasHeight = Mth.ceil(height / layoutScale);
        imageWidth = canvasWidth;
        imageHeight = canvasHeight;
        if (resetLayoutButton != null) {
            resetLayoutButton.setX(canvasWidth - resetLayoutButton.getWidth() - 4);
            resetLayoutButton.setY(canvasHeight - 21);
        }
        for (InventoryPanel panel : panels.values()) {
            panel.visible = false;
            panel.cells.clear();
            panel.contentHeight = 0;
            panel.equipmentIcon = -1;
        }
        buildPanels();
        for (Slot slot : menu.slots) setSlotPosition(slot, -10000, -10000);
        for (InventoryPanel panel : order) {
            if (!panel.visible) continue;
            clampPanel(panel);
            panel.scroll = Mth.clamp(panel.scroll, 0, panel.maxScroll());
            if (panel.collapsed) continue;
            for (InventoryPanel.Cell cell : panel.cells) {
                if (!cell.alias()) {
                    setSlotPosition(cell.slot(), panel.contentX() + cell.x() + (cell.width() - 16) / 2,
                            panel.contentY() + cell.y() - panel.scroll + (cell.height() - 16) / 2);
                }
            }
        }
    }

    protected static void setSlotPosition(Slot slot, int x, int y) {
        SlotPositionAccess access = (SlotPositionAccess) slot;
        access.blockz$setX(x);
        access.blockz$setY(y);
    }

    @Nullable
    protected final InventoryPanel topPanel(double x, double y) {
        for (int i = order.size() - 1; i >= 0; i--) {
            InventoryPanel panel = order.get(i);
            if (panel.contains(x, y)) return panel;
        }
        return null;
    }

    @Nullable
    private InventoryPanel.Cell findCell(double x, double y) {
        InventoryPanel panel = topPanel(x, y);
        if (panel == null || !panel.contentContains(x, y)) return null;
        if (panel.maxScroll() > 0 && x >= panel.x + panel.width - 4) return null;
        for (InventoryPanel.Cell cell : panel.cells) {
            if (!cell.slot().isActive()) continue;
            int cx = panel.contentX() + cell.x();
            int cy = panel.contentY() + cell.y() - panel.scroll;
            if (x >= cx && x < cx + cell.width() && y >= cy && y < cy + cell.height()) return cell;
        }
        return null;
    }

    @Nullable
    public final Slot findVisibleSlot(double x, double y) {
        InventoryPanel.Cell cell = findCell(x, y);
        return cell == null ? null : menu.getCarried().isEmpty() ? resolvePickupSlot(cell.slot()) : cell.slot();
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshLayout();
        int mx = (int) (mouseX / layoutScale);
        int my = (int) (mouseY / layoutScale);
        hoveredCell = findCell(mx, my);
        hoveredSlot = hoveredCell == null ? null : menu.getCarried().isEmpty() ? resolvePickupSlot(hoveredCell.slot()) : hoveredCell.slot();
        renderBackground(graphics);
        graphics.pose().pushPose();
        graphics.pose().scale(layoutScale, layoutScale, 1);
        RenderSystem.enableBlend();
        for (InventoryPanel panel : order) {
            if (!panel.visible) continue;
            graphics.flush();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            InventoryTheme.panel(graphics, panel.x, panel.y, panel.width, panel.height(), panel.id.equals("character"));
            InventoryTheme.header(graphics, font, panel.title, panel.x, panel.y, panel.width, panel.collapsed, true, panel.metal);
            renderPanelHeader(graphics, panel);
            if (panel.collapsed) continue;
            clipPanel(graphics, panel);
            if (panel.id.equals("sling")) {
                InventoryTheme.sling(graphics, panel.x, panel.y + InventoryTheme.HEADER, panel.width, panel.bodyHeight);
            }
            renderPanelContent(graphics, panel, mx, my, partialTick);
            for (InventoryPanel.Cell cell : panel.cells) renderCell(graphics, panel, cell, true);
            graphics.flush();
            for (InventoryPanel.Cell cell : panel.cells) renderCell(graphics, panel, cell, false);
            if (topPanel(mx, my) == panel && hoveredCell != null) renderTargetFeedback(graphics, panel, hoveredCell);
            graphics.flush();
            graphics.disableScissor();
            renderScrollbar(graphics, panel);
        }
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        renderCarried(graphics, mx, my);
        resetLayoutButton.render(graphics, mx, my, partialTick);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
        renderTooltip(graphics, mouseX, mouseY);
        InventoryPanel hoverPanel = topPanel(mx, my);
        if (hoveredSlot == null && hoverPanel != null && hoverPanel.headerContains(mx, my)
                && font.width(hoverPanel.title) > hoverPanel.width - 22) {
            graphics.renderTooltip(font, hoverPanel.title, mouseX, mouseY);
        }
    }

    protected final void clipPanel(GuiGraphics graphics, InventoryPanel panel) {
        graphics.flush();
        graphics.enableScissor(Mth.floor(panel.x * layoutScale), Mth.floor((panel.y + InventoryTheme.HEADER) * layoutScale),
                Mth.ceil((panel.x + panel.width) * layoutScale), Mth.ceil((panel.y + panel.height()) * layoutScale));
    }

    private void renderCell(GuiGraphics graphics, InventoryPanel panel, InventoryPanel.Cell cell, boolean background) {
        Slot slot = cell.slot();
        if (!slot.isActive()) return;
        int x = panel.contentX() + cell.x();
        int y = panel.contentY() + cell.y() - panel.scroll;
        int visibleHeight = cell.height();
        if (!background && slot instanceof TetrisSlot && slot.hasItem()) {
            visibleHeight = Math.max(visibleHeight, ItemSizeManager.getSize(slot.getItem()).height() * InventoryTheme.CELL);
        }
        if (y >= panel.y + panel.height() || y + visibleHeight <= panel.y + InventoryTheme.HEADER) return;
        if (cell.alias()) {
            if (background) return;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                int size = Math.min(32, Math.min(cell.width(), cell.height()) - 4);
                graphics.pose().pushPose();
                graphics.pose().translate(x + (cell.width() - size) / 2f, y + (cell.height() - size) / 2f, 0);
                graphics.pose().scale(size / 16f, size / 16f, 1);
                graphics.renderItem(stack, 0, 0);
                graphics.renderItemDecorations(font, stack, 0, 0);
                graphics.pose().popPose();
            }
            return;
        }
        boolean selected = panel.id.equals("hotbar") && slot.container == minecraft.player.getInventory()
                && slot.getSlotIndex() == minecraft.player.getInventory().selected;
        if (background) {
            InventoryTheme.slot(graphics, x, y, cell.width(), selected);
            if (panel.equipmentIcon >= 0 && !slot.hasItem()) {
                InventoryTheme.equipment(graphics, x + 1, y + 1, cell.width() - 2, panel.equipmentIcon);
            }
            return;
        }
        ((ContainerScreenAccess) this).blockz$renderSlot(graphics, slot);
        if (panel.id.equals("hotbar")) {
            graphics.drawString(font, Integer.toString(slot.getSlotIndex() + 1), x + 2, y + 1, InventoryTheme.MUTED, false);
        }
    }

    public boolean renderCustomSlot(GuiGraphics graphics, Slot slot, @Nullable Slot clickedSlot, ItemStack draggingItem) {
        if (!(slot instanceof TetrisSlot) || !slot.hasItem()) return false;
        ItemStack stack = slot.getItem();
        ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
        if (size.width() == 1 && size.height() == 1) return false;
        if (slot == clickedSlot && !draggingItem.isEmpty()) return true;
        int w = size.width() * InventoryTheme.CELL;
        int h = size.height() * InventoryTheme.CELL;
        Integer custom = ItemSizeManager.getGridColor(stack);
        int color = custom == null ? 0x60626B5B : (custom & 0xFFFFFF) | 0x60000000;
        graphics.fill(slot.x - 1, slot.y - 1, slot.x - 1 + w, slot.y - 1 + h, color);
        graphics.renderOutline(slot.x - 1, slot.y - 1, w, h, 0xFF8D9789);
        graphics.flush();
        float scale = Math.min(w, h) / 16f;
        graphics.pose().pushPose();
        graphics.pose().translate(slot.x - 1 + (w - 16 * scale) / 2f, slot.y - 1 + (h - 16 * scale) / 2f, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
        graphics.renderItemDecorations(font, stack, slot.x - 1 + w - 16, slot.y - 1 + h - 16);
        return true;
    }

    protected void renderTargetFeedback(GuiGraphics graphics, InventoryPanel panel, InventoryPanel.Cell cell) {
        int x = panel.contentX() + cell.x();
        int y = panel.contentY() + cell.y() - panel.scroll;
        ItemStack carried = menu.getCarried();
        int color = carried.isEmpty() || cell.slot().mayPlace(carried) ? 0xA0D0D7AA : 0xB0DC6655;
        graphics.renderOutline(x, y, cell.width(), cell.height(), color);
    }

    private void renderCarried(GuiGraphics graphics, int mouseX, int mouseY) {
        ContainerScreenAccess access = (ContainerScreenAccess) this;
        ItemStack dragging = access.blockz$getDraggingItem();
        ItemStack stack = dragging.isEmpty() ? menu.getCarried() : dragging;
        String count = null;
        if (!dragging.isEmpty() && access.blockz$isSplittingStack()) {
            stack = stack.copyWithCount(Mth.ceil(stack.getCount() / 2f));
        } else if (isQuickCrafting && quickCraftSlots.size() > 1) {
            stack = stack.copyWithCount(access.blockz$getQuickCraftingRemainder());
            if (stack.isEmpty()) count = ChatFormatting.YELLOW + "0";
        }
        if (!stack.isEmpty() || count != null) {
            graphics.flush();
            access.blockz$renderFloatingItem(graphics, stack, mouseX - 8, mouseY - (dragging.isEmpty() ? 8 : 16), count);
        }
    }

    protected final void renderCharacter(GuiGraphics graphics, InventoryPanel panel, int mouseX, int mouseY) {
        if (minecraft.player == null) return;
        int scale = Math.max(20, Math.min((panel.bodyHeight - 16) * 5 / 9, (panel.width - 12) * 4 / 3));
        int x = panel.x + panel.width / 2;
        int y = panel.y + panel.height() - 12;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, scale, Mth.clamp(x - mouseX, -25, 25), Mth.clamp(y - scale - mouseY, -15, 15), minecraft.player);
    }

    private void renderScrollbar(GuiGraphics graphics, InventoryPanel panel) {
        if (panel.maxScroll() <= 0) return;
        int h = panel.bodyHeight - 4;
        int thumb = Math.max(8, h * h / Math.max(1, panel.contentHeight));
        int y = panel.y + InventoryTheme.HEADER + 2 + (h - thumb) * panel.scroll / panel.maxScroll();
        graphics.fill(panel.x + panel.width - 3, panel.y + InventoryTheme.HEADER + 2, panel.x + panel.width - 1,
                panel.y + panel.height() - 2, 0xFF181B17);
        graphics.fill(panel.x + panel.width - 3, y, panel.x + panel.width - 1, y + thumb, InventoryTheme.MUTED);
    
    }

    @Override protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }
    @Override protected final void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshLayout();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX / layoutScale;
        double y = mouseY / layoutScale;
        refreshLayout();
        if (resetLayoutButton.mouseClicked(x, y, button)) return true;
        InventoryPanel panel = topPanel(x, y);
        if (panel != null && button == 0) {
            order.remove(panel);
            order.add(panel);
            if (panelHeaderClicked(panel, x, y, button)) return true;
            if (panel.headerContains(x, y)) {
                if (x >= panel.x + panel.width - 14) {
                    panel.collapsed = !panel.collapsed;
                    layoutChanged = true;
                    refreshLayout();
                } else {
                    draggedPanel = panel;
                    dragOffsetX = x - panel.x;
                    dragOffsetY = y - panel.y;
                }
                return true;
            }
            if (panel.maxScroll() > 0 && x >= panel.x + panel.width - 5) {
                scrollingPanel = panel;
                scrollTo(panel, y);
                return true;
            }
        }
        hoveredCell = findCell(x, y);
        hoveredSlot = hoveredCell == null ? null : menu.getCarried().isEmpty() ? resolvePickupSlot(hoveredCell.slot()) : hoveredCell.slot();
        if (panel != null && hoveredCell == null && panelClicked(panel, x, y, button)) return true;
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        double x = mouseX / layoutScale;
        double y = mouseY / layoutScale;
        if (draggedPanel != null) {
            draggedPanel.x = (int) (x - dragOffsetX);
            draggedPanel.y = (int) (y - dragOffsetY);
            draggedPanel.userPositioned = true;
            clampPanel(draggedPanel);
            layoutChanged = true;
            refreshLayout();
            return true;
        }
        if (scrollingPanel != null) {
            scrollTo(scrollingPanel, y);
            return true;
        }
        if (dragPanelInput(x, y, button, dx / layoutScale, dy / layoutScale)) return true;
        return super.mouseDragged(x, y, button, dx / layoutScale, dy / layoutScale);
    }

    private void scrollTo(InventoryPanel panel, double y) {
        double position = (y - panel.y - InventoryTheme.HEADER) / Math.max(1, panel.bodyHeight);
        panel.scroll = (int) (Mth.clamp(position, 0, 1) * panel.maxScroll());
        refreshLayout();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggedPanel != null || scrollingPanel != null) {
            draggedPanel = null;
            scrollingPanel = null;
            return true;
        }
        double x = mouseX / layoutScale;
        double y = mouseY / layoutScale;
        if (finishPanelInput(x, y, button)) return true;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double x = mouseX / layoutScale;
        double y = mouseY / layoutScale;
        InventoryPanel panel = topPanel(x, y);
        if (panel == null || panel.collapsed) return false;
        if (panelScrolled(panel, x, y, delta)) return true;
        if (panel.maxScroll() > 0) {
            panel.scroll = Mth.clamp(panel.scroll - (int) (delta * InventoryTheme.CELL), 0, panel.maxScroll());
            refreshLayout();
        }
        return true;
    }

    @Override
    protected boolean hasClickedOutside(double x, double y, int left, int top, int button) {
        return topPanel(x, y) == null;
    }

    protected final void saveLayout() {
        if (!layoutChanged) return;
        for (InventoryPanel panel : panels.values()) {
            if (panel.userPositioned) {
                preferences.setProperty(panel.id + ".x", Double.toString((double) panel.x / Math.max(1, canvasWidth - panel.width)));
                preferences.setProperty(panel.id + ".y", Double.toString((double) panel.y / Math.max(1, contentBottom() - panel.height())));
            }
            preferences.setProperty(panel.id + ".folded", Boolean.toString(panel.collapsed));
        }
        try {
            Files.createDirectories(LAYOUT_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(LAYOUT_FILE)) {
                preferences.store(writer, "BlockZ inventory panel positions");
            }
            layoutChanged = false;
        } catch (IOException writeFailure) {
            BlockZ.LOGGER.warn("Unable to save inventory panel layout", writeFailure);
        }
    }

    @Override
    public void removed() {
        saveLayout();
        super.removed();
    }
}
