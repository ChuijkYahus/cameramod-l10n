package net.mehvahdjukaar.vista.client.ui;

import net.mehvahdjukaar.moonlight.api.util.math.EntityAngles;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;

public class ViewFinderScreen extends AbstractContainerScreen<ViewFinderMenu> {

    private static final ResourceLocation BACKGROUND = VistaMod.res("textures/gui/viewfinder_gui.png");
    private static final ResourceLocation VIEW_SPRITE = VistaMod.res("viewfinder/view");
    private static final ResourceLocation VIEW_HOVERED_SPRITE = VistaMod.res("viewfinder/view_highlighted");
    private static final ResourceLocation VIEW_DISABLED_SPRITE = VistaMod.res("viewfinder/view_disabled");

    private NumberEditBox pitchSelector;
    private NumberEditBox yawSelector;

    public ViewFinderScreen(ViewFinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        int i = this.leftPos;
        int j = this.topPos;
        ViewFinderBlockEntity tile = this.menu.viewFinder;

        //view button sits in the same spot the cannon's maneuver button does
        this.addRenderableWidget(new ViewButton(i + 154, j + 16, true));

        //pitch / yaw angle boxes: copied from the cannon control panel
        Quaternionf rot = tile.getLocalOrientation(1);
        EntityAngles eulerAngles = EntityAngles.fromQuaternion(rot);
        this.pitchSelector = this.addRenderableWidget(new NumberEditBox(this.font, i + 144, j + 35, 18, 10));
        this.pitchSelector.setNumber(eulerAngles.pitch());
        this.yawSelector = this.addRenderableWidget(new NumberEditBox(this.font, i + 144, j + 55, 18, 10));
        this.yawSelector.setNumber(eulerAngles.yaw());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void onViewPressed(Button button) {
        if (!button.active) return;
        ViewFinderController.startControlling(this.menu.viewFinder);
        //dont clear owner: we are taking control right now
        this.onClose();
    }

    @Override
    public void onClose() {
        super.onClose();
        ViewFinderBlockEntity tile = this.menu.viewFinder;
        float yaw = this.yawSelector.getNumber();
        float pitch = this.pitchSelector.getNumber();
        //update client immediately too
        Quaternionf wantedQuat = EntityAngles.of(pitch, yaw).toQuaternion();
        tile.setTrustedInternalAttributes(wantedQuat, tile.getZoomLevel(), tile.isLocked());
        //release ownership unless we just entered view mode
        tile.syncToServer(!ViewFinderController.isActive(), minecraft.player);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    private final class ViewButton extends Button {

        public ViewButton(int x, int y, boolean active) {
            super(x, y, 10, 10, Component.empty(), ViewFinderScreen.this::onViewPressed, Button.DEFAULT_NARRATION);
            if (active) this.setTooltip(Tooltip.create(Component.translatable("gui.vista.viewfinder.view")));
            this.active = active;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
            ResourceLocation texture = this.active
                    ? (this.isHovered() ? VIEW_HOVERED_SPRITE : VIEW_SPRITE)
                    : VIEW_DISABLED_SPRITE;
            guiGraphics.blitSprite(texture, this.getX(), this.getY(), this.width, this.height);
        }
    }

    // number field for an angle, copied from the cannon screen
    private static class NumberEditBox extends EditBox {
        public NumberEditBox(Font font, int x, int y, int width, int height) {
            super(font, x, y, width, height, Component.empty());
            this.setMaxLength(4);
            this.setBordered(false);
            this.setFilter(this::isValidAngle);
        }

        private boolean isValidAngle(String str) {
            try {
                if (str.isEmpty() || str.equals("+") || str.equals("-")) return true;
                double d = Double.parseDouble(str);
                if (str.contains("[a-zA-Z]+")) return false;
                return d <= 360 && d >= -360;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public float getNumber() {
            try {
                return Float.parseFloat(this.getValue());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void setNumber(float value) {
            if (Math.abs(value - Math.round(value)) < 1e-3) {
                this.setValue(String.valueOf(Math.round(value)));
            } else {
                this.setValue(String.valueOf(value));
            }
        }
    }
}
