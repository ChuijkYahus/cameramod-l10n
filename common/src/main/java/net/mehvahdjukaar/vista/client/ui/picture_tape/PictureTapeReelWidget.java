package net.mehvahdjukaar.vista.client.ui.picture_tape;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class PictureTapeReelWidget extends AbstractWidget {

    private static final ResourceLocation REEL_BACKGROUND = VistaMod.res("picture_tape/reel_background");
    private static final ResourceLocation REEL_FOREGROUND = VistaMod.res("picture_tape/reel_foreground");
    private static final ResourceLocation REEL_NEW = VistaMod.res("picture_tape/reel_new");
    private static final ResourceLocation REEL_LEFT = VistaMod.res("picture_tape/reel_left");
    private static final ResourceLocation REEL_RIGHT = VistaMod.res("picture_tape/reel_right");

    private static final int REEL_W = 40;
    private static final int REEL_H = 52;
    private static final int PAD = 2;
    private static final int CAP_W = 2;

    private static final int FRAME_Z = 200;
    private static final int HOVER_Z = FRAME_Z - 10;

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

    private int contentWidth() {
        int cells = menu.getVisibleCells();
        return PAD * 2 + cells * REEL_W;
    }

    public int maxScroll() {
        return Math.max(0, contentWidth() - width);
    }

    public boolean canScroll() {
        return maxScroll() > 0;
    }

    public double getScrollFraction() {
        int max = maxScroll();
        if (max <= 0) return 0;
        clampScroll();
        return scrollOffset / max;
    }

    private void clampScroll() {
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
    }

    public void setScrollFraction(double fraction) {
        scrollOffset = Mth.clamp(fraction, 0, 1) * maxScroll();
    }

    public int cellAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return -1;
        int cells = menu.getVisibleCells();
        for (int i = 0; i < cells; i++) {
            int cx = getX() + PAD + i * REEL_W - (int) scrollOffset;
            if (mouseX >= cx && mouseX < cx + REEL_W && mouseY >= getY() && mouseY < getY() + REEL_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        clampScroll();
        int filled = menu.getFilledCount();
        int realCells = menu.getVisibleCells();
        int hovered = cellAt(mouseX, mouseY);

        int fillCells = Mth.ceil((width - PAD) / (float) REEL_W) + 1;
        int cells = Math.max(realCells, fillCells);

        g.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (int i = 0; i < cells; i++) {
            int cx = getX() + PAD + i * REEL_W - (int) scrollOffset;
            int cy = getY();
            if (cx + REEL_W < getX() || cx > getX() + width) continue;

            ResourceLocation base = i == filled ? REEL_NEW : REEL_BACKGROUND;
            g.blitSprite(base, cx, cy, REEL_W, REEL_H);
            if (i < filled) {
                PictureTapeRenderers.render(g, menu.getTapeContent().getItem(i), cx + 1, cy + 7, 38); //frame hides the bleed
            }
            if (i == hovered) {
                g.pose().pushPose();
                g.pose().translate(0, 0, HOVER_Z);
                g.fill(cx, cy, cx + REEL_W, cy + REEL_H, 0x33FFFFFF);
                g.pose().popPose();
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, FRAME_Z);
            g.blitSprite(REEL_FOREGROUND, cx, cy, REEL_W, REEL_H);
            g.pose().popPose();
        }

        int stripStart = getX() + PAD - (int) scrollOffset;
        int stripEnd = stripStart + cells * REEL_W;
        g.pose().pushPose();
        g.pose().translate(0, 0, FRAME_Z);
        g.blitSprite(REEL_LEFT, stripStart - CAP_W, getY(), CAP_W, REEL_H);
        g.blitSprite(REEL_RIGHT, stripEnd, getY(), CAP_W, REEL_H);
        g.pose().popPose();
        g.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (max > 0) {
            scrollOffset = Mth.clamp(scrollOffset - scrollY * (REEL_W / 2.0), 0, max);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
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
        return Screen.hasShiftDown();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    ItemStack itemAt(int cell) {
        return cell >= 0 && cell < menu.getFilledCount() ? menu.getTapeContent().getItem(cell) : ItemStack.EMPTY;
    }
}
