package net.mehvahdjukaar.vista.integration.joy_of_painting;

import net.mehvahdjukaar.vista.client.video_source.PictureTapeFrames;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// plays a Joy of Painting canvas on a tv by stretching its painted pixels across the screen
public class CanvasFrameTextureProvider implements PictureTapeFrames.ImageProvider {

    @Override
    public boolean matches(ItemStack stack) {
        return JoyOfPaintingCompat.isCanvas(stack);
    }

    @Override
    @Nullable
    public ResourceLocation getTexture(ItemStack stack) {
        return CanvasTapeTextures.getOrCreate(stack);
    }
}
