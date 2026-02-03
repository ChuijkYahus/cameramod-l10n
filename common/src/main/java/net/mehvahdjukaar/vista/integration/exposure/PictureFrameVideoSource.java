package net.mehvahdjukaar.vista.integration.exposure;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.render.image.ImageRenderer;
import io.github.mortuusars.exposure.client.render.image.RenderedImageInstance;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.item.AlbumItem;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.world.item.component.album.AlbumPage;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.mehvahdjukaar.moonlight.api.misc.TField;
import net.mehvahdjukaar.moonlight.api.misc.TMethod;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PictureFrameVideoSource implements IVideoSource {

    //TODO: split in 4 instead
    //todo. cache instead
    private final ItemStack pictureStack;

    public PictureFrameVideoSource(ItemStack pictureStack) {
        this.pictureStack = pictureStack;
    }

    @Nullable
    public static PictureFrameVideoSource maybeCreate(ItemStack pictureStack) {
        if (pictureStack.getItem() instanceof PhotographItem ||
                pictureStack.getItem() instanceof StackedPhotographsItem ||
                pictureStack.getItem() instanceof AlbumItem ||
                pictureStack.getItem() instanceof PictureTapeItem) {
            return new PictureFrameVideoSource(pictureStack);
        }
        return null;
    }

    @Override
    public @Nullable VertexConsumer getVideoFrameBuilder(TVBlockEntity targetScreen, float partialTick, MultiBufferSource buffer, boolean shouldUpdate, int screenSize, int pixelEffectRes) {

        ResourceLocation texture = getPictureTextureForRenderer(pictureStack, targetScreen.getAnimationTick());
        if (texture != null) {
            return TvScreenVertexConsumers.getFullSpriteVC(texture, buffer, 0, pixelEffectRes, targetScreen.getSwitchAnimationTicks());
        }
        return null;
    }


    private static final int TIME_PER_PICTURE = 40;

    private static ResourceLocation getFrameTexture(ItemStack stack, Frame frame) {
        PhotographStyle style = PhotographStyle.of(stack);
        RenderableImage image = style.process(ExposureClient.renderedExposures().getOrCreate(frame));

        ImageRenderer imageRenderer = ExposureClient.imageRenderer();
        RenderedImageInstance texture = GET_OR_CREATE_INSTANCE.invoke(imageRenderer, image);
        ResourceLocation textureLocation = TEXTURE_LOCATION.get(texture);

        //same that happens when it renders
        if (REQUIRES_UPLOAD.get(texture)) {
            UPDATE_TEXTURE.invoke(texture);
            REQUIRES_UPLOAD.set(texture, false);
        }

        return textureLocation;
    }

    private static ResourceLocation getPictureTextureForRenderer(ItemStack stack, int time) {


        Item item = stack.getItem();
        switch (item) {
            case PictureTapeItem pi -> {
                PictureTapeContent content = PictureTapeItem.getContent(stack);
                int size = content.size();
                if (size > 0) {
                    ItemStack s = content.getPicture((time / content.playbackSpeed()) % size);
                    //TODO: add maps
                    PhotographItem photographItem = (PhotographItem) s.getItem();
                    Frame frame = photographItem.getFrame(s);

                    return getFrameTexture(s, frame);
                }
            }
            case PhotographItem photographItem -> {

                Frame frame = photographItem.getFrame(stack);

                return getFrameTexture(stack, frame);
            }
            case StackedPhotographsItem stackedPhotographsItem -> {
                List<ItemAndStack<PhotographItem>> photos = stackedPhotographsItem.getPhotographs(stack);
                int size = photos.size();
                if (size > 0) {
                    ItemAndStack<PhotographItem> frameItem =
                            photos.get((time / TIME_PER_PICTURE) % size);
                    ItemStack s = frameItem.getItemStack();
                    Frame frame = frameItem.getItem().getFrame(s);

                    return getFrameTexture(s, frame);
                }
            }
            case AlbumItem album -> {
                List<AlbumPage> content = album.getContent(stack).pages();
                List<ItemStack> photos = content.stream()
                        .map(AlbumPage::photograph)
                        .filter(p -> p.getItem() instanceof PhotographItem)
                        .toList();
                if (!photos.isEmpty()) {
                    ItemStack s = photos.get((time / TIME_PER_PICTURE) % photos.size());
                    PhotographItem photographItem = (PhotographItem) s.getItem();
                    Frame frame = photographItem.getFrame(s);

                    return getFrameTexture(s, frame);
                }
            }
            default -> {
            }
        }
        return null;
    }

    //why not public?
    private static final TMethod<ImageRenderer, RenderedImageInstance> GET_OR_CREATE_INSTANCE =
            TMethod.of(ImageRenderer.class, "getOrCreateInstance", RenderableImage.class);
    private static final TField<RenderedImageInstance, Boolean> REQUIRES_UPLOAD =
            TField.of(RenderedImageInstance.class, "requiresUpload");
    private static final TField<RenderedImageInstance, ResourceLocation> TEXTURE_LOCATION =
            TField.of(RenderedImageInstance.class, "textureLocation");
    private static final TMethod<RenderedImageInstance, Void> UPDATE_TEXTURE =
            TMethod.of(RenderedImageInstance.class, "updateTexture");

}
