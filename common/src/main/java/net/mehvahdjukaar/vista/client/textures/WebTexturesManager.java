package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.web.MediaFrame;
import net.mehvahdjukaar.vista.client.web.MediaFramesHolder;
import net.mehvahdjukaar.vista.client.web.WebVideoCacheManager;
import net.mehvahdjukaar.vista.client.web.WebVideoDecoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class WebTexturesManager {
    private static final long DEFAULT_CACHE_SIZE_BYTES = 512L * 1024L * 1024L;

    private static final Map<String, WebVideoSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, WebTexture> TEXTURES = new ConcurrentHashMap<>();
    private static final AtomicLong TEXTURE_COUNTER = new AtomicLong();
    private static final ExecutorService WEB_WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Vista-WebTextures");
        thread.setDaemon(true);
        return thread;
    });

    @Nullable
    private static WebVideoCacheManager cacheManager;

    public static WebTexture requestWebTexture(String url) {
        WebVideoSession session = SESSIONS.computeIfAbsent(url, WebVideoSession::new);
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

        for (WebVideoSession session : SESSIONS.values()) {
            session.close();
        }
        SESSIONS.clear();
        TEXTURE_COUNTER.set(0);
    }

    private static ResourceLocation createNewFreeLocation() {
        return VistaMod.res("web_feed_" + TEXTURE_COUNTER.getAndIncrement());
    }

    private static synchronized WebVideoCacheManager getCacheManager() throws IOException {
        if (cacheManager == null) {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            cacheManager = new WebVideoCacheManager(gameDir, DEFAULT_CACHE_SIZE_BYTES);
        }
        return cacheManager;
    }

    static class WebVideoSession {
        private final String url;
        private final MediaFramesHolder frames = new MediaFramesHolder();
        private final CompletableFuture<Void> loadFuture;

        @Nullable
        private volatile WebVideoDecoder decoder;
        private volatile boolean failed;
        private volatile boolean closed;

        private WebVideoSession(String url) {
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
                WebVideoDecoder newDecoder = new WebVideoDecoder(VistaModClient.FFMPEG, frames, videoPath);
                this.decoder = newDecoder;
                newDecoder.start();
            } catch (Exception e) {
                failed = true;
                VistaMod.LOGGER.error("Failed to load web video {}", url, e);
            }
        }

        private void close() {
            closed = true;
            WebVideoDecoder currentDecoder = decoder;
            if (currentDecoder != null) {
                currentDecoder.stopDecoder();
            }
        }
    }
}
