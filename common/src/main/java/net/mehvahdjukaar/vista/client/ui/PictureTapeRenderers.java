package net.mehvahdjukaar.vista.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side registry of {@link TapeEntryRenderer}s. The vanilla map renderer is built in;
 * integrations register their own (gated behind their mod-compat check). Renderers are tried in
 * registration order and the first match wins.
 */
public class PictureTapeRenderers {

    private static final List<TapeEntryRenderer> RENDERERS = new ArrayList<>();

    static {
        register(new MapTapeEntryRenderer());
    }

    public static void register(TapeEntryRenderer renderer) {
        RENDERERS.add(renderer);
    }

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        for (TapeEntryRenderer renderer : RENDERERS) {
            if (renderer.matches(stack)) {
                renderer.render(graphics, stack, x, y, size);
                return;
            }
        }
        // unknown entry: neutral background with the item icon centered
        graphics.fill(x, y, x + size, y + size, 0xFF888888);
        graphics.renderItem(stack, x + size / 2 - 8, y + size / 2 - 8);
    }
}
