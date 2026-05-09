package net.mehvahdjukaar.vista.client.video_source;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.vista.client.CrtOverlay;
import net.mehvahdjukaar.vista.client.textures.TvScreenVertexConsumers;
import net.mehvahdjukaar.vista.client.textures.WebTexture;
import net.mehvahdjukaar.vista.client.textures.WebTexturesManager;
import net.mehvahdjukaar.vista.client.web.MediaState;
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
        //return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);

        double seconds = (videoAnimationTick + partialTick) / 20.0;
        if (textureHandle == null || lastScreenSize != screenSize) {
            this.textureHandle = WebTexturesManager.createHandle(url, projectorID, screenSize);
            this.lastScreenSize = screenSize;
        }
        WebTexture texture = textureHandle.getTexture();
        MediaState state = texture.uploadFrameAtTime(seconds);
        CrtOverlay overlay = CrtOverlay.NONE;
        if (state == MediaState.CLOSED) {
            overlay = CrtOverlay.DISCONNECT;
        }

        if (state == MediaState.FAILED) {
            return TvScreenVertexConsumers.getNoiseVC(buffer, pixelEffectRes, switchAnim);
        } else if (state == MediaState.LOADING) {
            return TvScreenVertexConsumers.getWaitingVc(buffer, pixelEffectRes, switchAnim);
        }
        if (state == MediaState.BUFFERING) {
            overlay = CrtOverlay.LOADING;
        }
        ResourceLocation textureId = texture.getResourceLocation();
        if (paused) {
            overlay = CrtOverlay.PAUSE;
        }

        return TvScreenVertexConsumers.getSingleTextureVC(buffer, textureId, overlay, pixelEffectRes, switchAnim, staticAnim);

    }
}
