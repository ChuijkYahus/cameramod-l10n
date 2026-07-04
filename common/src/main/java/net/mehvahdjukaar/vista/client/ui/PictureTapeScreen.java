package net.mehvahdjukaar.vista.client.ui;

import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Gallery screen for the picture tape: a horizontally scrolling strip of maps above the standard
 * player inventory. Maps render as their actual image, the trailing outlined cell is where new
 * maps get dropped/shift-clicked in, and clicking a map takes it back out.
 */
public class PictureTapeScreen extends AbstractContainerScreen<PictureTapeMenu> {

    // strip geometry (relative to leftPos/topPos)
    private static final int VIEW_X = 8;
    private static final int VIEW_Y = 18;
    private static final int VIEW_W = 160;
    private static final int VIEW_H = 48;
    private static final int MAP_SIZE = 40;
    private static final int GAP = 6;
    private static final int PAD = 4;                          // padding before the first cell
    private static final int CELL = MAP_SIZE + GAP;            // stride between cells
    private static final int MAP_TOP = VIEW_Y + (VIEW_H - MAP_SIZE) / 2;

    private static final int SB_X = VIEW_X;
    private static final int SB_Y = VIEW_Y + VIEW_H + 2;
    private static final int SB_W = VIEW_W;
    private static final int SB_H = 6;

    // vanilla inventory palette
    private static final int PANEL = 0xFFC6C6C6;
    private static final int BEVEL_LIGHT = 0xFFFFFFFF;
    private static final int BEVEL_DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int RECESS = 0xFF373737;
    private static final int OUTLINE = 0xFF777777;

    private double scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int lastSentSpeed;

    public PictureTapeScreen(PictureTapeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = 176;
        this.imageHeight = 171;
        super.init();
        this.titleLabelX = VIEW_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = PictureTapeMenu.INV_X;
        this.inventoryLabelY = PictureTapeMenu.INV_TOP - 11;

        // tiny playback-speed slider next to the title
        this.lastSentSpeed = getMenu().getPlaySpeed();
        int sliderW = 64, sliderH = 12;
        addRenderableWidget(new SpeedSlider(leftPos + imageWidth - 8 - sliderW, topPos + 4, sliderW, sliderH,
                getMenu().getPlaySpeed()));
    }

    // ---- geometry helpers ----

    private int contentWidth() {
        int cells = getMenu().getVisibleCells();
        return cells * CELL - GAP + PAD * 2;
    }

    private int maxScroll() {
        return Math.max(0, contentWidth() - VIEW_W);
    }

    private int handleWidth() {
        int content = contentWidth();
        if (content <= VIEW_W) return SB_W;
        return Math.max(16, (int) ((long) SB_W * VIEW_W / content));
    }

    // returns the cell (== map slot index) under the mouse, or -1
    private int cellAt(double mouseX, double mouseY) {
        int vx = leftPos + VIEW_X, vy = topPos + VIEW_Y;
        if (mouseX < vx || mouseX >= vx + VIEW_W || mouseY < vy || mouseY >= vy + VIEW_H) return -1;
        int cy = topPos + MAP_TOP;
        int cells = getMenu().getVisibleCells();
        for (int i = 0; i < cells; i++) {
            int cx = vx + PAD + i * CELL - (int) scrollOffset;
            if (mouseX >= cx && mouseX < cx + MAP_SIZE && mouseY >= cy && mouseY < cy + MAP_SIZE) {
                return i;
            }
        }
        return -1;
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0 && cell < getMenu().getFilledCount()) {
            ItemStack s = getMenu().getMaps().getItem(cell);
            if (!s.isEmpty()) {
                g.renderTooltip(this.font, s, mouseX, mouseY);
                return;
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int l = leftPos, t = topPos, r = leftPos + imageWidth, b = topPos + imageHeight;

        // window panel
        g.fill(l, t, r, b, PANEL);
        g.fill(l, t, r, t + 1, BEVEL_LIGHT);
        g.fill(l, t, l + 1, b, BEVEL_LIGHT);
        g.fill(r - 1, t, r, b, BEVEL_DARK);
        g.fill(l, b - 1, r, b, BEVEL_DARK);

        // strip recess
        int vx = leftPos + VIEW_X, vy = topPos + VIEW_Y;
        g.fill(vx, vy, vx + VIEW_W, vy + VIEW_H, RECESS);
        g.fill(vx, vy, vx + VIEW_W, vy + 1, BEVEL_DARK);
        g.fill(vx, vy, vx + 1, vy + VIEW_H, BEVEL_DARK);
        g.fill(vx + VIEW_W - 1, vy, vx + VIEW_W, vy + VIEW_H, BEVEL_LIGHT);
        g.fill(vx, vy + VIEW_H - 1, vx + VIEW_W, vy + VIEW_H, BEVEL_LIGHT);

        // player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, leftPos + PictureTapeMenu.INV_X + col * 18, topPos + PictureTapeMenu.INV_TOP + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, leftPos + PictureTapeMenu.INV_X + col * 18, topPos + PictureTapeMenu.HOTBAR_Y);
        }

        renderStrip(g, mouseX, mouseY);
        renderScrollbar(g);
    }

    private void renderStrip(GuiGraphics g, int mouseX, int mouseY) {
        int vx = leftPos + VIEW_X, vy = topPos + VIEW_Y;
        int cy = topPos + MAP_TOP;
        int filled = getMenu().getFilledCount();
        int cells = getMenu().getVisibleCells();
        int hovered = cellAt(mouseX, mouseY);

        g.enableScissor(vx, vy, vx + VIEW_W, vy + VIEW_H);
        for (int i = 0; i < cells; i++) {
            int cx = vx + PAD + i * CELL - (int) scrollOffset;
            if (cx + MAP_SIZE < vx || cx > vx + VIEW_W) continue;
            if (i < filled) {
                drawEntry(g, getMenu().getMaps().getItem(i), cx, cy);
            } else {
                drawAddCell(g, cx, cy);
            }
            if (i == hovered) {
                g.fill(cx, cy, cx + MAP_SIZE, cy + MAP_SIZE, 0x33FFFFFF);
            }
        }
        g.disableScissor();
    }

    private void drawEntry(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        // dark frame around the thumbnail
        g.fill(x - 1, y - 1, x + MAP_SIZE + 1, y + MAP_SIZE + 1, 0xFF000000);
        PictureTapeRenderers.render(g, stack, x, y, MAP_SIZE);
    }

    private void drawAddCell(GuiGraphics g, int x, int y) {
        g.renderOutline(x, y, MAP_SIZE, MAP_SIZE, 0xFF4A4A4A);
        g.renderOutline(x + 1, y + 1, MAP_SIZE - 2, MAP_SIZE - 2, 0x66FFFFFF);
        int cx = x + MAP_SIZE / 2;
        int cy = y + MAP_SIZE / 2;
        g.fill(cx - 8, cy - 1, cx + 8, cy + 1, OUTLINE);
        g.fill(cx - 1, cy - 8, cx + 1, cy + 8, OUTLINE);
    }

    private void renderScrollbar(GuiGraphics g) {
        if (maxScroll() <= 0) return;
        int sx = leftPos + SB_X, sy = topPos + SB_Y;
        g.fill(sx, sy, sx + SB_W, sy + SB_H, RECESS);

        int handleW = handleWidth();
        int handleX = sx + (int) (scrollOffset / maxScroll() * (SB_W - handleW));
        g.fill(handleX, sy, handleX + handleW, sy + SB_H, 0xFFAAAAAA);
        g.fill(handleX, sy, handleX + handleW, sy + 1, BEVEL_LIGHT);
        g.fill(handleX, sy, handleX + 1, sy + SB_H, BEVEL_LIGHT);
        g.fill(handleX + handleW - 1, sy, handleX + handleW, sy + SB_H, BEVEL_DARK);
        g.fill(handleX, sy + SB_H - 1, handleX + handleW, sy + SB_H, BEVEL_DARK);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 16, y + 16, SLOT_BG);
        g.fill(x - 1, y - 1, x + 16, y, RECESS);
        g.fill(x - 1, y - 1, x, y + 16, RECESS);
        g.fill(x + 16, y, x + 17, y + 17, BEVEL_LIGHT);
        g.fill(x - 1, y + 16, x + 17, y + 17, BEVEL_LIGHT);
    }

    // ---- input ----

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (max > 0) {
            scrollOffset = Mth.clamp(scrollOffset - scrollY * (CELL / 2.0), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (maxScroll() > 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            dragScrollbar(mouseX);
            return true;
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            ClickType type = hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
            this.minecraft.gameMode.handleInventoryMouseClick(
                    getMenu().containerId, cell, button, type, this.minecraft.player);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            dragScrollbar(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int sx = leftPos + SB_X, sy = topPos + SB_Y;
        return mouseX >= sx && mouseX < sx + SB_W && mouseY >= sy && mouseY < sy + SB_H;
    }

    private void dragScrollbar(double mouseX) {
        int max = maxScroll();
        if (max <= 0) {
            scrollOffset = 0;
            return;
        }
        int sx = leftPos + SB_X;
        int handleW = handleWidth();
        double t = (mouseX - sx - handleW / 2.0) / (SB_W - handleW);
        scrollOffset = Mth.clamp(t, 0, 1) * max;
    }

    private static double speedToValue(int speed) {
        int clamped = Mth.clamp(speed, PictureTapeMenu.MIN_SPEED, PictureTapeMenu.MAX_SPEED);
        return (clamped - PictureTapeMenu.MIN_SPEED) / (double) (PictureTapeMenu.MAX_SPEED - PictureTapeMenu.MIN_SPEED);
    }

    private class SpeedSlider extends AbstractSliderButton {
        SpeedSlider(int x, int y, int width, int height, int initialSpeed) {
            super(x, y, width, height, Component.empty(), speedToValue(initialSpeed));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.vista.picture_tape.speed", speedTicks()));
        }

        @Override
        protected void applyValue() {
            int speed = speedTicks();
            if (speed != lastSentSpeed) {
                lastSentSpeed = speed;
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, speed);
            }
        }

        private int speedTicks() {
            return (int) Math.round(Mth.lerp(this.value, PictureTapeMenu.MIN_SPEED, PictureTapeMenu.MAX_SPEED));
        }
    }
}
