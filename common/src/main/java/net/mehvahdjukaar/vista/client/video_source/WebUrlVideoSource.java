package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.CrtOverlay;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.textures.IWebTexture;
import net.mehvahdjukaar.vista.client.textures.WebTexturesManager;
import net.mehvahdjukaar.vista.client.web.MediaStatus;
import net.mehvahdjukaar.vista.common.tv.IntAnimationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WebUrlVideoSource implements IVideoSource {
    private final String url;
    private final UUID projectorID;
    private WebTexturesManager.Handle textureHandle;
    private int lastScreenSize = -1;

    public WebUrlVideoSource(String url, UUID projectorID) {
        this.url = url;
        this.projectorID = projectorID;
    }

    @Override
    public @NotNull VertexConsumer getVideoFrameBuilder(float partialTick, MultiBufferSource buffer,
                                                        boolean shouldUpdate, int screenSize, int pixelEffectRes,
                                                        int videoAnimationTick, boolean paused,
                                                        IntAnimationState switchAnim, IntAnimationState staticAnim) {

        if (textureHandle == null || lastScreenSize != screenSize) {
            this.textureHandle = WebTexturesManager.createHandle(url, projectorID, screenSize);
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
