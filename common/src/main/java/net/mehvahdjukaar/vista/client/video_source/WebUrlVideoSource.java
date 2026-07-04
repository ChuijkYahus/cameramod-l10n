package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.CrtOverlay;
import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.textures.WebTexturesManager;
import net.mehvahdjukaar.vista.client.web.MediaError;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

public class WebUrlVideoSource implements IVideoSource {
    @Nullable
    private final URI uri;
    private final UUID projectorID;
    private WebTexturesManager.Handle textureHandle;
    private Vec2i lastScreenSize = Vec2i.ZERO;

    public WebUrlVideoSource(String url, UUID projectorID) {
        this.projectorID = projectorID;
        this.uri = createUri(url);
    }
    private static URI createUri(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String s = url.trim();

        try {
            URI parsed = URI.create(s);

            // Has a scheme like http:, https:, file:, ftp:, etc.
            if (parsed.getScheme() != null) {
                return parsed;
            }

        } catch (Exception ignored) {
        }

        // No valid URI scheme -> treat as filesystem path
        try {
            return Paths.get(s).toUri();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, Vec2i screenSize, Vec2i pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim,
                                                        boolean showsTime) {

        if (uri == null) {
            // No link configured (blank/unset url) -> show a benign test card, not "broken" static.
            return TvScreenVertexConsumers.getBarsVC(buffer, pixelEffectRes, switchAnim);
        }

        if (textureHandle == null || !lastScreenSize.equals(screenSize)) {
            this.textureHandle = WebTexturesManager.createHandle(uri, projectorID, screenSize);
            this.lastScreenSize = screenSize;
        }

        IWebTexture texture = textureHandle.getTexture();
        MediaStatus state = texture.uploadFrameAtTime(videoAnimationTick, partialTick, paused);
        CrtOverlay overlay = CrtOverlay.NONE;
        if (state == MediaStatus.CLOSED) {
            overlay = CrtOverlay.DISCONNECT;
        }

        if (state == MediaStatus.FAILED) {
            ResourceLocation errorScreen = errorScreen(texture.getError());
            if (errorScreen != null) {
                return TvScreenVertexConsumers.getErrorVc(buffer, pixelEffectRes, errorScreen, switchAnim);
            }
            return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
        } else if (state == MediaStatus.LOADING) {
            if (VistaModClient.isFFmpegDownloading()) {
               int ffmpegProg = VistaModClient.getFFmpegDownloadProgress();
                if (ffmpegProg >= 0) {
                    return TvScreenVertexConsumers.getDownloadingVc(buffer, pixelEffectRes, ffmpegProg, switchAnim);
                }
            }
            int progress = texture.getDownloadProgress();
            if (progress >= 0) {
                //return TvScreenVertexConsumers.getDownloadingVc(buffer, pixelEffectRes, progress, switchAnim);
            }
            if (texture.isRetrying()) {
                return TvScreenVertexConsumers.getRetryingVc(buffer, pixelEffectRes, videoAnimationTick, switchAnim);
            }
            return TvScreenVertexConsumers.getWaitingVc(buffer, pixelEffectRes, videoAnimationTick, switchAnim);
        }
        if (state == MediaStatus.BUFFERING) {
            overlay = CrtOverlay.LOADING;
        }
        ResourceLocation textureId = texture.getTextureLocation();
        if (paused) {
            overlay = CrtOverlay.PAUSE;
        }

        return TvScreenVertexConsumers.getSingleTextureVC(buffer, textureId, overlay, pixelEffectRes, switchAnim, staticAnim);

    }

    @Nullable
    private static ResourceLocation errorScreen(MediaError error) {
        return switch (error) {
            case FORBIDDEN -> VistaModClient.FORBIDDEN_SCREEN;
            case NOT_FOUND -> VistaModClient.NOT_FOUND_SCREEN;
            case BAD_LINK -> VistaModClient.BAD_LINK_SCREEN;
            // no backend available -> plain static noise instead of a dedicated card
            case NO_FFMPEG, NONE -> null;
        };
    }
}
