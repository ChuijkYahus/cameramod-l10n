package net.mehvahdjukaar.vista.integration.watermedia;

import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageCache;

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
        this.imageCache = ImageAPI.getCache(URI.create(url.trim()), Minecraft.getInstance());
        this.executor = executor;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.imageCache.load();
    }

    @Override
    public IWebTexture createTextureView(ResourceLocation resourceLocation) {
        return (true || imageCache.getException() != null || imageCache.isVideo() || imageCache.getRenderer() == null) ?
                new WatermediaVideoTexture(resourceLocation, imageCache, targetWidth, targetHeight, executor) :
                new WatermediaImageTexture(resourceLocation, imageCache, targetWidth, targetHeight, executor);
    }

    @Override
    public void close() {
        //imageCache.deuse();
    }
}
