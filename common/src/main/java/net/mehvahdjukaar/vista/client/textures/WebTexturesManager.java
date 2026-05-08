package net.mehvahdjukaar.vista.client.textures;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.web.MediaCacheManager;
import net.mehvahdjukaar.vista.client.web.MediaSession;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebTexturesManager {
    private static final long DEFAULT_CACHE_SIZE_BYTES = 512L * 1024L * 1024L;
    private static final long CACHE_EXPIRY_MINUTES = 2L;

    private static final ExecutorService WEB_WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Vista-WebTextures");
        thread.setDaemon(true);
        return thread;
    });

    private static final MediaCacheManager MEDIA_CACHE_MANAGER = new MediaCacheManager(
            PlatHelper.getGamePath(), DEFAULT_CACHE_SIZE_BYTES);

    private static final LoadingCache<String, MediaSession> SESSION_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
            .removalListener(notification -> {
                MediaSession session = (MediaSession) notification.getValue();
                if (session != null) {
                    session.close();
                }
            })
            .build(new CacheLoader<>() {
                @Override
                public MediaSession load(String url) {
                    return new MediaSession(url, VistaModClient.getFFmpeg(), MEDIA_CACHE_MANAGER, WEB_WORKER);
                }
            });

    private static final LoadingCache<ResourceLocation, WebTexture> TEXTURE_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
            .removalListener(notification -> {
                WebTexture texture = (WebTexture) notification.getValue();
                if (texture != null) {
                    RenderSystem.recordRenderCall(texture::unregister);
                }
            })
            .build(new CacheLoader<>() {
                @Override
                public WebTexture load(ResourceLocation key) {
                    throw new IllegalStateException("not supported!");
                }
            });

    public static class Handle {

        private final ResourceLocation textureId;
        private final String url;
        private final int screenSize;

        public Handle(String url, UUID id, int screenSize) {
            this.textureId = makeUniqueTextureLoc(url, id, screenSize);
            this.url = url;
            this.screenSize = screenSize;
        }

        public WebTexture getTexture() {
            return TEXTURE_CACHE.asMap()
                    .computeIfAbsent(textureId,
                            resourceLocation -> {
                                WebTexture texture = new WebTexture(resourceLocation,
                                        SESSION_CACHE.getUnchecked(this.url));
                                texture.register();
                                return texture;
                            });
        }
    }


    public static Handle createHandle(String url, UUID projectorUUID, int screenSize) {
        return new Handle(url, projectorUUID, screenSize);
    }

    public static void clear() {
        TEXTURE_CACHE.invalidateAll();
        TEXTURE_CACHE.cleanUp();

        SESSION_CACHE.invalidateAll();
        SESSION_CACHE.cleanUp();
    }

    private static ResourceLocation makeUniqueTextureLoc(String url, UUID uuid, int screenSize) {
        String uniqueTextureKey = url + "_" + uuid + "_" + screenSize;
        return VistaMod.res("web_feed/" + sanitizePath(uniqueTextureKey));
    }

    private static String sanitizePath(String key) {
        String sanitized = key.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");

        return sanitized.isBlank() ? "unnamed" : sanitized;
    }
}
