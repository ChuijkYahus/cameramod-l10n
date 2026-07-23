package net.mehvahdjukaar.vista.client.ui;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

// scrolling film strip of maps with a horizontal scrollbar and a vertical playback-speed slider
public class PictureTapeScreen extends VistaContainerScreen<PictureTapeMenu> {

    private static final ResourceLocation BACKGROUND = VistaMod.res("textures/gui/picture_tape.png");
    private static final ResourceLocation SCROLLER = VistaMod.res("picture_tape/scroller");
    private static final ResourceLocation SCROLLER_DISABLED = VistaMod.res("picture_tape/scroller_disabled");
    private static final ResourceLocation SPEED_HANDLE = VistaMod.res("picture_tape/scroller_speed");

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
    private ScrollBarWidget speedBar;
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

        //reel scrollbar reads/writes the strip's own offset so it stays in sync with wheel scrolling
        addRenderableWidget(new ScrollBarWidget(
                ScrollBarWidget.Orientation.HORIZONTAL, leftPos + HBAR_X, topPos + HBAR_Y, HBAR_W, HBAR_H,
                SCROLLER, HBAR_HANDLE_W, HBAR_HANDLE_H)
                .disabledSprite(SCROLLER_DISABLED)
                .usableWhen(reelStrip::canScroll)
                .bind(reelStrip::getScrollFraction, reelStrip::setScrollFraction));

        this.lastSentSpeed = getMenu().getPlaySpeed();
        this.speedBar = addRenderableWidget(new ScrollBarWidget(
                ScrollBarWidget.Orientation.VERTICAL, leftPos + SPEED_X, topPos + SPEED_Y, SPEED_W, SPEED_H,
                SPEED_HANDLE, SPEED_HANDLE_W, SPEED_HANDLE_H)
                .showValue(PictureTapeMenu.MIN_SPEED, PictureTapeMenu.MAX_SPEED)
                .value(speedToFraction(getMenu().getPlaySpeed()))
                .onChanged(f -> onSpeedChanged()));
    }

    private void onCellClicked(int cell, int button, boolean shift) {
        ClickType type = shift ? ClickType.QUICK_MOVE : ClickType.PICKUP;
        this.minecraft.gameMode.handleInventoryMouseClick(
                getMenu().containerId, cell, button, type, this.minecraft.player);
    }

    private void onSpeedChanged() {
        int speed = speedBar.getMappedValue();
        if (speed != lastSentSpeed) {
            lastSentSpeed = speed;
            getMenu().setPlaySpeedClientSide(speed);
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, speed);
        }
    }

    private static double speedToFraction(int speed) {
        int clamped = Mth.clamp(speed, PictureTapeMenu.MIN_SPEED, PictureTapeMenu.MAX_SPEED);
        return (clamped - PictureTapeMenu.MIN_SPEED) / (double) (PictureTapeMenu.MAX_SPEED - PictureTapeMenu.MIN_SPEED);
    }

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
}
