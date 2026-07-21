package net.mehvahdjukaar.vista.integration.joy_of_painting;

import net.mehvahdjukaar.vista.client.ui.TapeEntryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xerca.xercapaint.item.ItemCanvas;

// draws a painted canvas as its actual image, stretched to fill the square thumbnail
public class CanvasTapeEntryRenderer implements TapeEntryRenderer {

    @Override
    public boolean matches(ItemStack stack) {
        return JoyOfPaintingCompat.isCanvas(stack);
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        ResourceLocation texture = CanvasTapeTextures.getOrCreate(stack);
        if (texture == null || !(stack.getItem() instanceof ItemCanvas canvas)) {
            // pixels missing: neutral placeholder with the item icon
            graphics.fill(x, y, x + size, y + size, 0xFF888888);
            graphics.renderItem(stack, x + size / 2 - 8, y + size / 2 - 8);
            return;
        }
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        graphics.blit(texture, x, y, size, size, 0, 0, w, h, w, h);
    }
}
