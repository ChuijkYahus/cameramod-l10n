package net.mehvahdjukaar.vista.integration.exposure;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PictureFrameVideoSource implements IVideoSource {

    private final ItemStack pictureStack;

    public PictureFrameVideoSource(ItemStack pictureStack) {
        this.pictureStack = pictureStack;
    }

    @Override
    public @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes) {
        ItemStack stack = targetScreen.getDisplayedItem();

        ResourceLocation texture = ExposureCompatClient.getPictureTextureForRenderer(stack, targetScreen.getAnimationTick());
        if (texture != null) {
            return TvScreenVertexConsumers.getFullSpriteVC(texture, buffer, 0, pixelEffectRes, targetScreen.getSwitchAnimationTicks());
        }
        return null;
    }
}
