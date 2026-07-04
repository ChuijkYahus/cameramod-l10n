package net.mehvahdjukaar.vista.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Draws one picture-tape entry as a square thumbnail. Implementations are contributed by
 * integrations and registered in {@link PictureTapeRenderers}.
 */
public interface TapeEntryRenderer {

    /**
     * Whether this renderer can draw the given stack.
     */
    boolean matches(ItemStack stack);

    /**
     * Draw {@code stack} as a {@code size}x{@code size} thumbnail with its top-left at (x, y).
     */
    void render(GuiGraphics graphics, ItemStack stack, int x, int y, int size);
}
