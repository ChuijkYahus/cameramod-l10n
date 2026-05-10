package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.minecraft.resources.ResourceLocation;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageCache;
import org.watermedia.shaded.kiulian.downloader.downloader.client.DefaultClients;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.concurrent.Executor;

public class WatermediaSession implements IMediaSession {

    private final Executor executor;

    private final ImageCache imageCache;
    private final int targetWidth;
    private final int targetHeight;

    public WatermediaSession(String url, Executor executor,
                             int targetWidth, int targetHeight) {
        //TODO: check executor
        this.imageCache = ImageAPI.getCache(URI.create(url.trim()), executor);
        this.executor = executor;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.imageCache.load();
    }

    public static void initHack() {
        try {
            var c = Class.forName("org.watermedia.api.network.patchs.YoutubePatch");
            Field field = c.getDeclaredField("WORKING_CLIENT");
            field.setAccessible(true);
            field.set(null, DefaultClients.VALUES[0]);
        } catch (Throwable ignored) {
        }

    }

    @Override
    public IWebTexture createTextureView(ResourceLocation resourceLocation) {
        return isVideo() ?
                new WatermediaVideoTexture(resourceLocation, this, imageCache.uri, targetWidth, targetHeight, executor) :
                new WatermediaImageTexture(resourceLocation, this, imageCache, targetWidth, targetHeight, executor);
    }

    @Override
    public boolean shouldRefreshTexture(IWebTexture tt) {
        if (isVideo()) {
            //this was a video after all. we make a texture then
            return tt instanceof WatermediaImageTexture;
        }
        return false;
    }

    private boolean isVideo() {
        ImageCache.Status status = imageCache.getStatus();
        return (status == ImageCache.Status.READY && imageCache.isVideo()) || status == ImageCache.Status.FAILED;
    }

    @Override
    public boolean isFailed() {
        return imageCache.getException() != null;
    }

    @Override
    public void close() {
        //imageCache.deuse();
    }
}
