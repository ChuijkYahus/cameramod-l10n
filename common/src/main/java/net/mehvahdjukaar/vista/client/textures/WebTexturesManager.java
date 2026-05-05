package net.mehvahdjukaar.vista.client.textures;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.client.texture_renderer.RenderableDynamicTexture;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.mehvahdjukaar.vista.client.web.MediaFramesList;
import net.mehvahdjukaar.vista.client.web.MediaCacheManager;
import net.mehvahdjukaar.vista.client.web.FFmpegMediaDecoder;
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

    private static final Map<String, WebMediaSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, WebTexture> TEXTURES = new ConcurrentHashMap<>();
    LoadingCache<ResourceLocation, CompletableFuture<RenderableDynamicTexture>>
            TEXTURE_CACHE = CacheBuilder.newBuilder().removalListener((i) -> {
        CompletableFuture<RenderableDynamicTexture> future = (CompletableFuture)i.getValue();
        if (future != null) {
            future.thenAccept((texture) -> {
                Objects.requireNonNull(texture);
                RenderSystem.recordRenderCall(texture::unregister);
            });
        }
    }).expireAfterAccess(2L,TimeUnit.MINUTES).build(new CacheLoader<>() {
        public CompletableFuture<RenderableDynamicTexture> load(ResourceLocation key) {
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
    private static MediaCacheManager cacheManager;

    public static WebTexture requestWebTexture(String url) {
        WebMediaSession session = SESSIONS.computeIfAbsent(url, WebMediaSession::new);
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

        for (WebMediaSession session : SESSIONS.values()) {
            session.close();
        }
        SESSIONS.clear();
        TEXTURE_COUNTER.set(0);
    }

    private static ResourceLocation createNewFreeLocation() {
        return VistaMod.res("web_feed_" + TEXTURE_COUNTER.getAndIncrement());
    }

    private static synchronized MediaCacheManager getCacheManager() throws IOException {
        if (cacheManager == null) {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            cacheManager = new MediaCacheManager(gameDir, DEFAULT_CACHE_SIZE_BYTES);
        }
        return cacheManager;
    }

    static class WebMediaSession implements AutoCloseable {
        private final String url;
        private final MediaFramesList frames = new MediaFramesList();
        private final CompletableFuture<Void> loadFuture;

        @Nullable
        private volatile FFmpegMediaDecoder decoder;
        private volatile boolean failed;
        private volatile boolean closed;

        private WebMediaSession(String url) {
            this.url = url;
            this.loadFuture = CompletableFuture.runAsync(this::load, WEB_WORKER);
        }

        @Nullable
        MediaFrame getFrameAtTime(double seconds) {
            return frames.getLoopingFrameAtTime(seconds);
        }

        boolean isReady() {
            return frames.size() > 0;
        }

        boolean isFailed() {
            return failed || loadFuture.isCompletedExceptionally();
        }

        private void load() {
            try {
                Path videoPath = getCacheManager().getOrDownload(url);
                if (closed) return;
                FFmpegMediaDecoder newDecoder = new FFmpegMediaDecoder(VistaModClient.FFMPEG, frames, videoPath);
                this.decoder = newDecoder;
                newDecoder.start();
            } catch (Exception e) {
                failed = true;
                VistaMod.LOGGER.error("Failed to load web video {}", url, e);
            }
        }

        @Override
        public void close() {
            closed = true;
            FFmpegMediaDecoder currentDecoder = decoder;
            if (currentDecoder != null) {
                currentDecoder.stopDecoder();
            }
            frames.close();
        }
    }
}
