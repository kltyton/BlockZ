package com.yitianys.BlockZ.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings({"deprecation", "removal"})
public final class InventoryTheme {
    public static final int CELL = 18;
    public static final int HEADER = 16;
    public static final int PAD = 4;
    public static final int GAP = 6;
    public static final int PLAYER_WIDTH = 198;
    public static final int STORAGE_WIDTH = 170;
    public static final int TEXT = 0xFFE1E2DF;
    public static final int MUTED = 0xFFA3A7A1;
    public static final int SELECTED = 0xFFD0D7AA;
    private static final ResourceLocation STATUS = texture("status");
    private static final ResourceLocation STORAGE = texture("storage");
    private static final ResourceLocation HOTBAR = texture("hotbar");

    private InventoryTheme() { }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("blockz", "textures/gui/panels/" + name + ".png");
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, boolean character) {
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x70000000);
        graphics.fill(x, y, x + width, y + height, 0xE0080908);
        graphics.flush();
        graphics.blit(STATUS, x, y, width, height, 0f, 120f, 535, 1320, 717, 1845);
        if (!character) graphics.renderOutline(x, y, width, height, 0x704C514B);
    }

    public static void header(GuiGraphics graphics, Font font, Component title, int x, int y, int width,
                              boolean collapsed, boolean foldable, boolean metal) {
        if (metal) {
            graphics.blit(STORAGE, x, y, width, HEADER, 20f, 383f, 778, 68, 845, 1874);
        } else {
            graphics.fillGradient(x, y, x + width, y + HEADER, 0xFF343733, 0xF0131513);
            graphics.hLine(x, x + width - 1, y, 0xFF777D74);
        }
        int available = Math.max(0, width - PAD * 2 - (foldable ? 12 : 0));
        String label = title.getString();
        float scale = Math.max(0.65f, Math.min(1f, (float) available / Math.max(1, font.width(label))));
        int textWidth = (int) (available / scale);
        if (font.width(label) > textWidth) label = font.plainSubstrByWidth(label, Math.max(0, textWidth - font.width("…"))) + "…";
        graphics.pose().pushPose();
        graphics.pose().translate(x + PAD, y + (HEADER - font.lineHeight * scale) / 2f, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, 0, 0, TEXT, false);
        graphics.pose().popPose();
        if (foldable) {
            graphics.drawString(font, collapsed ? ">" : "v", x + width - 10, y + 4, TEXT, false);
        }
    }

    public static void slot(GuiGraphics graphics, int x, int y, int size, boolean selected) {
        graphics.blit(HOTBAR, x, y, size, size, 3f, 1f, 123, 123, 1127, 126);
        graphics.renderOutline(x, y, size, size, selected ? SELECTED : 0xFF747872);
    }

    public static void equipment(GuiGraphics graphics, int x, int y, int size, int type) {
        int sourceY = switch (type) {
            case 0 -> 274;
            case 1 -> 452;
            case 2 -> 635;
            default -> 816;
        };
        graphics.blit(STATUS, x, y, size, size, 551f, sourceY, 148, 148, 717, 1845);
    }

    public static void sling(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(STATUS, x, y, width, height, 0f, 1518f, 710, 316, 717, 1845);
    }
}
