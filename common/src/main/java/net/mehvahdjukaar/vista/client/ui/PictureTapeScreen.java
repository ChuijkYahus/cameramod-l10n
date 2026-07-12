package net.mehvahdjukaar.vista.client.ui;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * Gallery screen for the picture tape. A horizontally scrolling film strip of maps ({@link
 * PictureTapeReelWidget}) sits above the player inventory, driven by a loom-style scrollbar along
 * the top and a vertical playback-speed slider down the right side. Everything is laid out over the
 * {@code picture_tape.png} background; all pixel positions live in static fields.
 */
public class PictureTapeScreen extends AbstractContainerScreen<PictureTapeMenu> {

    private static final ResourceLocation BACKGROUND = VistaMod.res("textures/gui/picture_tape.png");
    private static final ResourceLocation SCROLLER = VistaMod.res("picture_tape/scroller");
    private static final ResourceLocation SCROLLER_DISABLED = VistaMod.res("picture_tape/scroller_disabled");
    private static final ResourceLocation SPEED_HANDLE = VistaMod.res("picture_tape/scroller_speed");
    private static final ResourceLocation SPEED_HANDLE_DISABLED = VistaMod.res("picture_tape/scroller_speed_disabled");

    private static final int IMAGE_W = 176;
    private static final int IMAGE_H = 181;

    // scrolling reel strip (relative to leftPos/topPos)
    private static final int REEL_VIEW_X = 8;
    private static final int REEL_VIEW_Y = 32;
    private static final int REEL_VIEW_W = 135;
    private static final int REEL_VIEW_H = 52;

    // horizontal scrollbar track above the strip; handle is scroller.png (15x12)
    private static final int HBAR_X = 8;
    private static final int HBAR_Y = 17;
    private static final int HBAR_W = 135;
    private static final int HBAR_H = 12;
    private static final int HBAR_HANDLE_W = 15;
    private static final int HBAR_HANDLE_H = 12;

    // vertical speed slider track on the right; handle is scroller_speed.png (19x12)
    private static final int SPEED_X = 149;
    private static final int SPEED_Y = 17;
    private static final int SPEED_W = 19;
    private static final int SPEED_H = 67;
    private static final int SPEED_HANDLE_W = 19;
    private static final int SPEED_HANDLE_H = 12;

    private PictureTapeReelWidget reelStrip;
    private int lastSentSpeed;

    public PictureTapeScreen(PictureTapeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        super.init();
        this.titleLabelX = REEL_VIEW_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = PictureTapeMenu.INV_X;
        this.inventoryLabelY = PictureTapeMenu.INV_TOP - 11;

        this.reelStrip = addRenderableWidget(new PictureTapeReelWidget(
                leftPos + REEL_VIEW_X, topPos + REEL_VIEW_Y, REEL_VIEW_W, REEL_VIEW_H,
                getMenu(), this::onCellClicked));
        addRenderableWidget(new HorizontalScrollBar(
                leftPos + HBAR_X, topPos + HBAR_Y, HBAR_W, HBAR_H));

        this.lastSentSpeed = getMenu().getPlaySpeed();
        addRenderableWidget(new SpeedSlider(
                leftPos + SPEED_X, topPos + SPEED_Y, SPEED_W, SPEED_H, getMenu().getPlaySpeed()));
    }

    private void onCellClicked(int cell, int button, boolean shift) {
        ClickType type = shift ? ClickType.QUICK_MOVE : ClickType.PICKUP;
        this.minecraft.gameMode.handleInventoryMouseClick(
                getMenu().containerId, cell, button, type, this.minecraft.player);
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cell = reelStrip.cellAt(mouseX, mouseY);
        ItemStack hovered = reelStrip.itemAt(cell);
        if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mouseX, mouseY);
            return;
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    private static double speedToValue(int speed) {
        int clamped = Mth.clamp(speed, PictureTapeMenu.MIN_SPEED, PictureTapeMenu.MAX_SPEED);
        return (clamped - PictureTapeMenu.MIN_SPEED) / (double) (PictureTapeMenu.MAX_SPEED - PictureTapeMenu.MIN_SPEED);
    }

    // ---- horizontal scrollbar (loom style) ----

    private class HorizontalScrollBar extends AbstractWidget {
        HorizontalScrollBar(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean can = reelStrip.canScroll();
            int handleX = getX() + (int) (reelStrip.getScrollFraction() * (width - HBAR_HANDLE_W));
            int handleY = getY() + (height - HBAR_HANDLE_H) / 2;
            g.blitSprite(can ? SCROLLER : SCROLLER_DISABLED, handleX, handleY, HBAR_HANDLE_W, HBAR_HANDLE_H);
        }

        private void seek(double mouseX) {
            double fraction = (mouseX - getX() - HBAR_HANDLE_W / 2.0) / (width - HBAR_HANDLE_W);
            reelStrip.setScrollFraction(Mth.clamp(fraction, 0, 1));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (reelStrip.canScroll() && isMouseOver(mouseX, mouseY)) {
                seek(mouseX);
                return true;
            }
            return false;
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            seek(mouseX);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }

    // ---- vertical playback-speed slider ----

    private class SpeedSlider extends AbstractWidget {
        private static final int TEXT_COLOR = 0xFF404040;
        private double value;

        SpeedSlider(int x, int y, int width, int height, int initialSpeed) {
            super(x, y, width, height, Component.empty());
            this.value = speedToValue(initialSpeed);
        }

        private int speedTicks() {
            return (int) Math.round(Mth.lerp(value, PictureTapeMenu.MIN_SPEED, PictureTapeMenu.MAX_SPEED));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int range = height - SPEED_HANDLE_H;
            int handleX = getX() + (width - SPEED_HANDLE_W) / 2;
            int handleY = getY() + (int) ((1 - value) * range);
            g.blitSprite(SPEED_HANDLE, handleX, handleY, SPEED_HANDLE_W, SPEED_HANDLE_H);

            String text = String.valueOf(speedTicks());
            int tx = handleX + (SPEED_HANDLE_W - font.width(text)) / 2;
            int ty = handleY + (SPEED_HANDLE_H - font.lineHeight) / 2 + 1;
            g.drawString(font, text, tx, ty, TEXT_COLOR, false);
        }

        private void seek(double mouseY) {
            int range = height - SPEED_HANDLE_H;
            double v = 1 - (mouseY - getY() - SPEED_HANDLE_H / 2.0) / range;
            this.value = Mth.clamp(v, 0, 1);
            applyValue();
        }

        private void applyValue() {
            int speed = speedTicks();
            if (speed != lastSentSpeed) {
                lastSentSpeed = speed;
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, speed);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY)) {
                seek(mouseY);
                return true;
            }
            return false;
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            seek(mouseY);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }
}
