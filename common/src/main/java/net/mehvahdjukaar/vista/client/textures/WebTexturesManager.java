package net.mehvahdjukaar.vista.client.textures;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.web.FFmpegMediaSession;
import net.mehvahdjukaar.vista.client.web.IMediaSession;
import net.mehvahdjukaar.vista.client.web.MediaCacheManager;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpeg;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.watermedia.WatermediaSession;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class WebTexturesManager {
    private static final long DEFAULT_CACHE_SIZE_BYTES = 512L * 1024L * 1024L;
    private static final long CACHE_EXPIRY_MINUTES = 2L;


    private static final ExecutorService SESSION_LOADER_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "WebMediaLoader");
        thread.setDaemon(true);
        return thread;
    });

    private static final MediaCacheManager MEDIA_CACHE_MANAGER = new MediaCacheManager(
            PlatHelper.getGamePath(), DEFAULT_CACHE_SIZE_BYTES);

    private static final LoadingCache<String, IMediaSession> SESSION_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
            .removalListener(notification -> {
                IMediaSession session = (IMediaSession) notification.getValue();
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            })
            .build(new CacheLoader<>() {
                @Override
                public IMediaSession load(String key) {
                    throw new IllegalStateException("not supported!");
                }
            });

    private static final LoadingCache<ResourceLocation, IWebTexture> TEXTURE_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
            .removalListener(notification -> {
                IWebTexture texture = (IWebTexture) notification.getValue();
                if (texture != null) {
                    RenderSystem.recordRenderCall(texture::unregister);
                }
            })
            .build(new CacheLoader<>() {
                @Override
                public IWebTexture load(ResourceLocation key) {
                    throw new IllegalStateException("not supported!");
                }
            });

    public static class Handle {

        private final ResourceLocation textureId;
        private final String sessionId;
        private final String url;
        private final int screenSize;

        public Handle(String url, UUID id, int screenSize) {
            this.textureId = makeUniqueTextureLoc(url, id, screenSize);
            this.sessionId = makeUniqueSessionLoc(url, screenSize);
            this.url = url;
            this.screenSize = screenSize;
        }

        public IWebTexture getTexture() {
            //refresh sessions first for loading cache.

            IMediaSession session = getSession();

            IWebTexture wt = TEXTURE_CACHE.asMap()
                    .computeIfAbsent(textureId,
                            resourceLocation -> {
                                IWebTexture texture = session.createTextureView(resourceLocation);
                                texture.register();
                                return texture;
                            });
            if (session.shouldRefreshTexture(wt)) {
                TEXTURE_CACHE.invalidate(textureId);
            }
            return wt;
        }

        private IMediaSession getSession() {
            return SESSION_CACHE.asMap()
                    .computeIfAbsent(sessionId, res -> {
                        int imageSize = ClientConfigs.WEB_RESOLUTION_SCALE.get() * screenSize;
                        return createMediaSession(url, imageSize, imageSize);
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

    private static String makeUniqueSessionLoc(String url, int screenSize) {
        return url + "@" + screenSize;
    }

    private static ResourceLocation makeUniqueTextureLoc(String url, UUID uuid, int screenSize) {
        String uniqueTextureKey = url + "@" + uuid + "@" + screenSize;
        return VistaMod.res("web_feed/" + sanitizePath(uniqueTextureKey));
    }

    private static String sanitizePath(String key) {
        String sanitized = key.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");

        return sanitized.isBlank() ? "unnamed" : sanitized;
    }


    public static IMediaSession createMediaSession(String url, int width, int height) {
        ClientConfigs.EngineMode engine = ClientConfigs.VIDEO_ENGINE.get();

        FFmpeg fFmpeg = VistaModClient.getFFmpeg();
        if (CompatHandler.WATERMEDIA && engine != ClientConfigs.EngineMode.USE_FFMPEG) {
            if (engine == ClientConfigs.EngineMode.USE_VLC || !looksLikeMedia(url)) {
                return createWatermediaSession(url, width, height);
            } else {
                return new AlternativeSession(
                        () -> createFFmpegSession(url, width, height, fFmpeg),
                        () -> createWatermediaSession(url, width, height)
                );
            }
        } else {
            return createFFmpegSession(url, width, height, fFmpeg);
        }
    }

    private static @NotNull WatermediaSession createWatermediaSession(String url, int width, int height) {
        return new WatermediaSession(url, SESSION_LOADER_EXECUTOR, width, height);
    }

    private static @NotNull FFmpegMediaSession createFFmpegSession(String url, int width, int height, FFmpeg fFmpeg) {
        return new FFmpegMediaSession(url, fFmpeg, MEDIA_CACHE_MANAGER, SESSION_LOADER_EXECUTOR, width, height);
    }

    private static final Pattern MEDIA_EXT = Pattern.compile("\\.[a-zA-Z0-9]+(?:\\?.*)?$");

    public static boolean looksLikeMedia(String url) {
        return MEDIA_EXT.matcher(url).find();
    }
}
