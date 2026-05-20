package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.CrtOverlay;
import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.textures.WebTexturesManager;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
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
        URI uri = null;
        if (url != null && !url.isBlank()) {
            String s = url.trim();

            Path path = Paths.get(s);
            try {
                URI parsed = URI.create(s);

                // Has a scheme like http:, https:, file:, ftp:, etc.
                if (parsed.getScheme() != null) {
                    uri = parsed;
                } else {
                    // No scheme → treat as filesystem path
                    uri = path.toUri();
                }

            } catch (Exception e) {
                try {
                    // Invalid URI syntax → fallback to filesystem path
                    uri = path.toUri();
                } catch (Exception ignored) {
                }
            }
        }
        return uri;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, Vec2i screenSize, Vec2i pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim,
                                                        boolean showsTime) {

        if (uri == null) {
            return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
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
}
