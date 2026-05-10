package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.minecraft.resources.ResourceLocation;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageCache;
import org.watermedia.api.network.patchs.YoutubePatch;
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
            Field field = YoutubePatch.class.getDeclaredField("WORKING_CLIENT");
            field.setAccessible(true);
            field.set(null, DefaultClients.VALUES[0]);
        } catch (Exception ignored) {
        }

    }

    @Override
    public IWebTexture createTextureView(ResourceLocation resourceLocation) {
        //true || imageCache.getException() != null || imageCache.isVideo() || imageCache.getRenderer() == null
        return (!imageCache.uri.toString().endsWith(".gif")) ?
                new WatermediaVideoTexture(resourceLocation, this, imageCache, targetWidth, targetHeight, executor) :
                new WatermediaImageTexture(resourceLocation, this, imageCache, targetWidth, targetHeight, executor);
    }

    @Override
    public boolean shouldRefreshTexture(IWebTexture tt) {
        return false;
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
