package net.mehvahdjukaar.vista.client.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

// helper for draggable widget
public abstract class VistaContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected VistaContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof ScrollBarWidget bar && bar.isDragging()) {
                return bar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof ScrollBarWidget bar) bar.stopDragging();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
