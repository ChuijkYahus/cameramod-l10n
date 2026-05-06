package net.mehvahdjukaar.vista.client.textures;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.web.MediaCacheManager;
import net.mehvahdjukaar.vista.client.web.MediaSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class WebTexturesManager {
    private static final long DEFAULT_CACHE_SIZE_BYTES = 512L * 1024L * 1024L;

    private static final Map<String, MediaSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, WebTexture> TEXTURES = new ConcurrentHashMap<>();
    LoadingCache<ResourceLocation, CompletableFuture<WebTexture>>
            TEXTURE_CACHE = CacheBuilder.newBuilder().removalListener((i) -> {
        CompletableFuture<RenderableDynamicTexture> future = (CompletableFuture) i.getValue();
        if (future != null) {
            future.thenAccept((texture) -> {
                Objects.requireNonNull(texture);
                RenderSystem.recordRenderCall(texture::unregister);
            });
        }
    }).expireAfterAccess(2L, TimeUnit.MINUTES).build(new CacheLoader<>() {
        public CompletableFuture<WebTexture> load(ResourceLocation key) {
            return new CompletableFuture();
        }
    });

    private static final AtomicLong TEXTURE_COUNTER = new AtomicLong();
    private static final ExecutorService WEB_WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Vista-WebTextures");
        thread.setDaemon(true);
        return thread;
    });


    @Nullable
    private static final MediaCacheManager MEDIA_CACHE_MANAGER = new MediaCacheManager(
            PlatHelper.getGamePath(), DEFAULT_CACHE_SIZE_BYTES);

    public static WebTexture requestWebTexture(String url) {
        MediaSession session = SESSIONS.computeIfAbsent(url, WebTexturesManager::createSession);
        ResourceLocation location = createNewFreeLocation();
        WebTexture texture = new WebTexture(url, location, session);
        texture.register();
        TEXTURES.put(location, texture);

        return texture;
    }

    public static void releaseWebTexture(WebTexture texture) {
        ResourceLocation location = texture.getResourceLocation();
        WebTexture wt = TEXTURES.remove(location);
        if (wt != null) wt.unregister(); //internally calls close
    }

    public static void clear() {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation location : TEXTURES.keySet()) {
            manager.release(location);
        }
        TEXTURES.clear();

        for (MediaSession session : SESSIONS.values()) {
            session.close();
        }
        SESSIONS.clear();
        TEXTURE_COUNTER.set(0);
    }

    private static ResourceLocation createNewFreeLocation() {
        return VistaMod.res("web_feed_" + TEXTURE_COUNTER.getAndIncrement());
    }

    private static MediaSession createSession(String url) {
            return new MediaSession(url, VistaModClient.FFMPEG, MEDIA_CACHE_MANAGER, WEB_WORKER);
    }
}
