package net.mehvahdjukaar.vista.integration;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ExposureCompat {

    //TODO: add picture tape item that can be insterted inplace of photops. basically a clone of the album but less functionality. also it holds maps

    private static final int TIME_PER_PICTURE = 40;

    public static ResourceLocation getPictureTextureForRenderer(ItemStack stack, int time) {


        Item item = stack.getItem();
        if (item instanceof PhotographItem photographItem) {

            Frame frame = photographItem.getFrame(stack);

            return getFrameTexture(stack, frame);
        } else if (item instanceof StackedPhotographsItem stackedPhotographsItem) {
            List<ItemAndStack<PhotographItem>> photos = stackedPhotographsItem.getPhotographs(stack);
            int size = photos.size();
            if (size > 0) {
                ItemAndStack<PhotographItem> frameItem =
                        photos.get((time / TIME_PER_PICTURE) % size);
                ItemStack s = frameItem.getItemStack();
                Frame frame = frameItem.getItem().getFrame(stack);

                return getFrameTexture(s, frame);
            }
        } else if (item instanceof AlbumItem album) {
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
        return null;
    }

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

    public static boolean isPictureItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof PhotographItem || item instanceof StackedPhotographsItem || item instanceof AlbumItem;
    }


    private static final TMethod<ImageRenderer, RenderedImageInstance> GET_OR_CREATE_INSTANCE =
            TMethod.of(ImageRenderer.class, "getOrCreateInstance", RenderableImage.class);
    private static final TField<RenderedImageInstance, Boolean> REQUIRES_UPLOAD =
            TField.of(RenderedImageInstance.class, "requiresUpload");
    private static final TField<RenderedImageInstance, ResourceLocation> TEXTURE_LOCATION =
            TField.of(RenderedImageInstance.class, "textureLocation");
    private static final TMethod<RenderedImageInstance, Void> UPDATE_TEXTURE =
            TMethod.of(RenderedImageInstance.class, "updateTexture");


}
