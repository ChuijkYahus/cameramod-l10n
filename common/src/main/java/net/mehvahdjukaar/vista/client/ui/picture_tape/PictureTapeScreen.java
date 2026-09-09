package net.mehvahdjukaar.vista.client.ui.picture_tape;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.ui.ScrollBarWidget;
import net.mehvahdjukaar.vista.client.ui.VistaContainerScreen;
import net.mehvahdjukaar.vista.common.picture_tape.PictureTapeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class PictureTapeScreen extends VistaContainerScreen<PictureTapeMenu> {

    private static final ResourceLocation BACKGROUND = VistaMod.res("textures/gui/picture_tape.png");
    private static final ResourceLocation SCROLLER = VistaMod.res("picture_tape/scroller");
    private static final ResourceLocation SCROLLER_DISABLED = VistaMod.res("picture_tape/scroller_disabled");
    private static final ResourceLocation SPEED_HANDLE = VistaMod.res("picture_tape/scroller_speed");

    private PictureTapeReelWidget reelStrip;
    private ScrollBarWidget speedBar;
    private int lastSentSpeed;

    public PictureTapeScreen(PictureTapeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = 176;
        this.imageHeight = 181;
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = PictureTapeMenu.INV_X;
        this.inventoryLabelY = PictureTapeMenu.INV_TOP - 11;

        this.reelStrip = addRenderableWidget(new PictureTapeReelWidget(
                leftPos + 8, topPos + 32, 135, 52,
                getMenu(), this::onCellClicked));

        addRenderableWidget(new ScrollBarWidget(
                ScrollBarWidget.Orientation.HORIZONTAL, leftPos + 8, topPos + 17, 135, 12,
                SCROLLER, 15, 12)
                .disabledSprite(SCROLLER_DISABLED)
                .usableWhen(reelStrip::canScroll)
                .bind(reelStrip::getScrollFraction, reelStrip::setScrollFraction));

        this.lastSentSpeed = getMenu().getPlaySpeed();
        this.speedBar = addRenderableWidget(new ScrollBarWidget(
                ScrollBarWidget.Orientation.VERTICAL, leftPos + 149, topPos + 17, 19, 67,
                SPEED_HANDLE, 19, 12)
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
