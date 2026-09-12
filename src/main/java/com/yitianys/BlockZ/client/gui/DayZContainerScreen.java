package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.mixin.client.ContainerScreenAccess;
import com.yitianys.BlockZ.mixin.client.ScreenRenderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/** Keeps specialized menu transactions and native widgets on the same menu instance. */
public class DayZContainerScreen<M extends AbstractContainerMenu> extends PanelInventoryScreen<M> {
    private record Position(int x, int y) { }
    private final AbstractContainerScreen<M> nativeScreen;
    private final Map<Slot, Position> nativePositions = new IdentityHashMap<>();
    private final Inventory inventory;
    private int nativeHeight;
    private boolean nativeGesture;
    @Nullable private static DayZContainerScreen<?> renderingNative;

    public DayZContainerScreen(AbstractContainerScreen<M> nativeScreen, Inventory inventory) {
        super(nativeScreen.getMenu(), inventory, nativeScreen.getTitle());
        this.nativeScreen = nativeScreen;
        this.inventory = inventory;
        int inventoryY = nativeScreen.getYSize();
        for (Slot slot : menu.slots) {
            nativePositions.put(slot, new Position(slot.x, slot.y));
            if (slot.container == inventory) inventoryY = Math.min(inventoryY, slot.y);
        }
        nativeHeight = Math.max(40, inventoryY - 4);
        if (nativeScreen instanceof MerchantScreen) nativeHeight = nativeScreen.getYSize();
    }

    @Override protected int requiredWidth() { return InventoryTheme.PLAYER_WIDTH + InventoryTheme.STORAGE_WIDTH + InventoryTheme.GAP * 2 + 8 + Math.max(170, nativeScreen.getXSize() + 48); }

    @Override
    protected void init() {
        nativeScreen.init(minecraft, nativeScreen.getXSize(), nativeScreen.getYSize());
        super.init();
    }

    private boolean recipeBookOpen() {
        return nativeScreen instanceof RecipeUpdateListener recipes && recipes.getRecipeBookComponent().isVisible();
    }

    @Override
    protected void buildPanels() {
        int x = layoutLeft();
        int middle = x + InventoryTheme.PLAYER_WIDTH + InventoryTheme.GAP;
        int right = middle + 176;
        InventoryPanel hotbar = panel("hotbar", label("hotbar"), x + (requiredWidth() - 250) / 2, 3, 242, 34, false);
        int pocketCount = com.yitianys.BlockZ.ui.DayZUiPolicy.shouldUseDayZ(inventory.player)
                ? com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots() : 27;
        InventoryPanel pockets = panel("pockets", label("pockets"), middle, 58, 170,
                Math.max(26, (pocketCount + 8) / 9 * 18 + 8), true);
        panel("character", inventory.player.getDisplayName(), x, 58, InventoryTheme.PLAYER_WIDTH, contentBottom() - 148, false);
        InventoryPanel sling = panel("sling", label("sling"), x, contentBottom() - 72, InventoryTheme.PLAYER_WIDTH, 54, false);
        int height = recipeBookOpen() ? nativeScreen.getYSize() : nativeHeight;
        InventoryPanel vicinity = panel("vicinity", title, right, 58, Math.max(170, nativeScreen.getXSize() + 48),
                Math.min(contentBottom() - 80, height + 8), false);
        int stored = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == inventory) {
                int index = slot.getSlotIndex();
                if (index >= 0 && index < 9) {
                    hotbar.cells.add(new InventoryPanel.Cell(slot, index * 26, 0, 26, 26, false));
                    if (index == inventory.selected) sling.cells.add(new InventoryPanel.Cell(slot, 0, 0, InventoryTheme.PLAYER_WIDTH - 8, 46, true));
                } else if (index >= 9 && index < 9 + pocketCount) {
                    pockets.add(slot, (index - 9) % 9 * 18, (index - 9) / 9 * 18);
                    if (slot.hasItem()) stored++;
                }
            } else if (!recipeBookOpen()) {
                Position pos = nativePositions.get(slot);
                vicinity.add(slot, pos.x() + nativeInset(), pos.y());
            }
        }
        hotbar.contentHeight = 26;
        sling.contentHeight = 46;
        pockets.title = label("pockets").copy().append(" (" + stored + "/" + pockets.cells.size() + ")");
        vicinity.contentHeight = Math.max(vicinity.contentHeight, height);
    }

    private int nativeInset() { return 20; }

    @Override
    protected void renderPanelHeader(GuiGraphics graphics, InventoryPanel panel) {
        if (panel.id.equals("vicinity") && recipeBookOpen()) {
            graphics.drawString(font, Component.translatable("screen.blockz.recipe_book_close"),
                    panel.x + panel.width - 86, panel.y + 4, InventoryTheme.TEXT, false);
        }
    }

    @Override
    protected boolean panelHeaderClicked(InventoryPanel panel, double x, double y, int button) {
        if (button == 0 && panel.id.equals("vicinity") && recipeBookOpen() && panel.headerContains(x, y)
                && x >= panel.x + panel.width - 88 && x < panel.x + panel.width - 16
                && nativeScreen instanceof RecipeUpdateListener recipes) {
            recipes.getRecipeBookComponent().toggleVisibility();
            refreshLayout();
            return true;
        }
        return false;
    }

    private static Component label(String name) { return Component.translatable("screen.blockz.panel." + name); }

    @Override
    protected void renderPanelContent(GuiGraphics graphics, InventoryPanel panel, int mouseX, int mouseY, float partialTick) {
        if (panel.id.equals("character")) renderCharacter(graphics, panel, mouseX, mouseY);
        if (!panel.id.equals("vicinity")) return;
        int x = panel.contentX() + nativeInset();
        int y = panel.contentY() - panel.scroll;
        boolean hovered = topPanel(mouseX, mouseY) == panel;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        renderingNative = this;
        try {
            nativeScreen.render(graphics, hovered ? mouseX - x : -10000, hovered ? mouseY - y : -10000, partialTick);
        } finally {
            renderingNative = null;
            graphics.pose().popPose();
        }
    }

    public static boolean isRenderingNative(Object screen) {
        return renderingNative != null && renderingNative.nativeScreen == screen;
    }

    public static boolean isNativeDelegate(Object screen) {
        return Minecraft.getInstance().screen instanceof DayZContainerScreen<?> wrapper && wrapper.nativeScreen == screen;
    }

    public static boolean isNativeRenderActive() { return renderingNative != null; }

    public static boolean shouldSkipBackground(ResourceLocation texture, int width, int height, float u, float v) {
        return renderingNative != null && texture.getPath().startsWith("textures/gui/container/")
                && width >= renderingNative.nativeScreen.getXSize() - 2 && height >= 24;
    }

    public static void renderNativeBase(AbstractContainerScreen<?> original, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ContainerScreenAccess access = (ContainerScreenAccess) original;
        access.blockz$setHoveredSlot(null);
        access.blockz$renderBackground(graphics, partialTick, mouseX, mouseY);
        for (Renderable widget : ((ScreenRenderAccess) original).blockz$getRenderables()) widget.render(graphics, mouseX, mouseY, partialTick);
        access.blockz$renderLabels(graphics, mouseX, mouseY);
    }

    public static boolean applyNativeScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        DayZContainerScreen<?> wrapper = renderingNative;
        if (wrapper == null) return false;
        InventoryPanel panel = wrapper.panels.get("vicinity");
        int ox = panel.contentX() + wrapper.nativeInset();
        int oy = panel.contentY() - panel.scroll;
        renderingNative = null;
        try {
            graphics.enableScissor((int) Math.floor((x1 + ox) * wrapper.layoutScale), (int) Math.floor((y1 + oy) * wrapper.layoutScale),
                    (int) Math.ceil((x2 + ox) * wrapper.layoutScale), (int) Math.ceil((y2 + oy) * wrapper.layoutScale));
        } finally {
            renderingNative = wrapper;
        }
        return true;
    }

    public static boolean shouldSkipLabel(Component text) {
        return renderingNative != null && text.getString().equals(renderingNative.inventory.getDisplayName().getString());
    }

    @Override
    protected boolean panelClicked(InventoryPanel panel, double x, double y, int button) {
        if (!panel.id.equals("vicinity") || !panel.contentContains(x, y)) return false;
        nativeGesture = true;
        nativeScreen.mouseClicked(x - panel.contentX() - nativeInset(), y - panel.contentY() + panel.scroll, button);
        return true;
    }

    @Override
    protected boolean finishPanelInput(double x, double y, int button) {
        if (!nativeGesture) return false;
        nativeGesture = false;
        InventoryPanel panel = panels.get("vicinity");
        nativeScreen.mouseReleased(x - panel.contentX() - nativeInset(), y - panel.contentY() + panel.scroll, button);
        return true;
    }

    @Override
    protected boolean dragPanelInput(double x, double y, int button, double dx, double dy) {
        if (!nativeGesture) return false;
        InventoryPanel panel = panels.get("vicinity");
        nativeScreen.mouseDragged(x - panel.contentX() - nativeInset(), y - panel.contentY() + panel.scroll, button, dx, dy);
        return true;
    }

    @Override
    protected boolean panelScrolled(InventoryPanel panel, double x, double y, double delta) {
        return panel.id.equals("vicinity") && nativeScreen.mouseScrolled(x - panel.contentX() - nativeInset(), y - panel.contentY() + panel.scroll, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (recipeBookOpen() && nativeScreen instanceof RecipeUpdateListener recipes
                && recipes.getRecipeBookComponent().keyPressed(key, scanCode, modifiers)) return true;
        if (nativeScreen.getFocused() instanceof EditBox box && box.isFocused()) return nativeScreen.keyPressed(key, scanCode, modifiers);
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return nativeScreen.charTyped(character, modifiers) || super.charTyped(character, modifiers);
    }

    @Override
    protected void containerTick() {
        nativeScreen.tick();
        refreshLayout();
    }

    @Override
    public void removed() {
        saveLayout();
        nativeScreen.removed();
    }
}
