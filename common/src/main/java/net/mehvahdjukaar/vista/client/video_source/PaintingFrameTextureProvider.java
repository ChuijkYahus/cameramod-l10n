package net.mehvahdjukaar.vista.client.video_source;

import net.mehvahdjukaar.vista.client.ui.PaintingTapeEntryRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

// plays a painting on a tv by stretching its variant art across the screen
public class PaintingFrameTextureProvider implements PictureTapeFrames.ImageProvider {

    @Override
    public boolean matches(ItemStack stack) {
        return stack.is(Items.PAINTING);
    }

    @Override
    @Nullable
    public ResourceLocation getTexture(ItemStack stack) {
        return PaintingTapeEntryRenderer.textureFor(stack);
    }
}
