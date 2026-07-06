package net.mehvahdjukaar.vista.client.ui;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Horizontally-scrolling film strip of stored maps. Each cell is a film "reel": a background slot,
 * the map image, and a foreground frame drawn over it. The trailing "add" cell uses the {@code new}
 * reel instead. The whole strip is scissored to its bounds so partially-scrolled reels are clipped.
 * <p>All pixel sizes live in static fields so they can be retuned if the art changes.
 */
public class PictureTapeReelWidget extends AbstractWidget {

    private static final ResourceLocation REEL_BACKGROUND = VistaMod.res("picture_tape/reel_background");
    private static final ResourceLocation REEL_FOREGROUND = VistaMod.res("picture_tape/reel_foreground");
    private static final ResourceLocation REEL_NEW = VistaMod.res("picture_tape/reel_new");

    // reel sprite size
    private static final int REEL_W = 40;
    private static final int REEL_H = 52;
    // spacing between reels and padding at the strip ends
    private static final int GAP = 4;
    private static final int PAD = 2;
    private static final int CELL = REEL_W + GAP;                 // stride between reels
    // transparent window inside the foreground where the map image shows through
    private static final int IMG_X = 1;                          // x offset of the image inside a reel
    private static final int IMG_Y = 7;                          // y offset of the image inside a reel
    private static final int IMG_SIZE = 38;                      // image is drawn square, frame hides the horizontal bleed

    private static final int HOVER_TINT = 0x33FFFFFF;

    /**
     * Routes a click on a reel back to the screen (which forwards it to the menu).
     */
    public interface CellClickHandler {
        void onCellClicked(int cell, int button, boolean shift);
    }

    private final PictureTapeMenu menu;
    private final CellClickHandler clickHandler;
    private double scrollOffset = 0;

    public PictureTapeReelWidget(int x, int y, int width, int height, PictureTapeMenu menu, CellClickHandler clickHandler) {
        super(x, y, width, height, Component.empty());
        this.menu = menu;
        this.clickHandler = clickHandler;
    }

    // ---- scroll geometry ----

    private int contentWidth() {
        int cells = menu.getVisibleCells();
        return PAD * 2 + cells * CELL - GAP;
    }

    public int maxScroll() {
        return Math.max(0, contentWidth() - width);
    }

    public boolean canScroll() {
        return maxScroll() > 0;
    }

    public double getScrollFraction() {
        int max = maxScroll();
        return max <= 0 ? 0 : scrollOffset / max;
    }

    public void setScrollFraction(double fraction) {
        scrollOffset = Mth.clamp(fraction, 0, 1) * maxScroll();
    }

    /**
     * Reel (== map slot index) under the mouse, or -1.
     */
    public int cellAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return -1;
        int cells = menu.getVisibleCells();
        for (int i = 0; i < cells; i++) {
            int cx = getX() + PAD + i * CELL - (int) scrollOffset;
            if (mouseX >= cx && mouseX < cx + REEL_W && mouseY >= getY() && mouseY < getY() + REEL_H) {
                return i;
            }
        }
        return -1;
    }

    // ---- rendering ----

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int filled = menu.getFilledCount();
        int cells = menu.getVisibleCells();
        int hovered = cellAt(mouseX, mouseY);

        g.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (int i = 0; i < cells; i++) {
            int cx = getX() + PAD + i * CELL - (int) scrollOffset;
            int cy = getY();
            if (cx + REEL_W < getX() || cx > getX() + width) continue;

            g.blitSprite(REEL_BACKGROUND, cx, cy, REEL_W, REEL_H);
            if (i < filled) {
                PictureTapeRenderers.render(g, menu.getTapeContent().getItem(i), cx + IMG_X, cy + IMG_Y, IMG_SIZE);
                g.blitSprite(REEL_FOREGROUND, cx, cy, REEL_W, REEL_H);
            } else {
                g.blitSprite(REEL_NEW, cx, cy, REEL_W, REEL_H);
            }
            if (i == hovered) {
                g.fill(cx, cy, cx + REEL_W, cy + REEL_H, HOVER_TINT);
            }
        }
        g.disableScissor();
    }

    // ---- input ----

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (max > 0) {
            scrollOffset = Mth.clamp(scrollOffset - scrollY * (CELL / 2.0), 0, max);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // handled in mouseClicked so we get the button
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            clickHandler.onCellClicked(cell, button, hasShiftDown());
            return true;
        }
        return false;
    }

    private static boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    ItemStack itemAt(int cell) {
        return cell >= 0 && cell < menu.getFilledCount() ? menu.getTapeContent().getItem(cell) : ItemStack.EMPTY;
    }
}
