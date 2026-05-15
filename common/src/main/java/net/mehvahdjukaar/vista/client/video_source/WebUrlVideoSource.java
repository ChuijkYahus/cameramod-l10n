package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
import java.nio.file.Paths;
import java.util.UUID;

public class WebUrlVideoSource implements IVideoSource {
    @Nullable
    private final URI uri;
    private final UUID projectorID;
    private WebTexturesManager.Handle textureHandle;
    private int lastScreenSize = -1;

    public WebUrlVideoSource(String url, UUID projectorID) {
        this.projectorID = projectorID;
        URI uri = null;
        if (!url.isBlank()) {
            try {
                uri = Paths.get(url.trim()).toUri();
            } catch (Exception ignored) {
            }
        }
        this.uri = uri;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, int screenSize, int pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim) {

        if(uri == null){
            return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
        }

        if (textureHandle == null || lastScreenSize != screenSize) {
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
